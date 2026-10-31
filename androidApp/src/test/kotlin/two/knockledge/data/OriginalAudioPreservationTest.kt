package two.knockledge.data

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.TranscriptResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The note's WAV must stay the untouched original; pre-processing works on a copy. */
class OriginalAudioPreservationTest {

    private val tempFiles = mutableListOf<File>()

    private class FakeSource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = flow { emit(ShortArray(16)) }
    }

    private class FakeBuffer(private val wav: File) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) = Unit
        override fun setWindowSeconds(seconds: Int) = Unit
        override fun bufferedMillis(): Long = 2_000L
        override fun snapshot(): File = wav
        override fun clear() = Unit
    }

    @AfterTest
    fun cleanup() = tempFiles.forEach { it.delete() }

    private fun wavOf(pcm: ShortArray): File {
        val f = File.createTempFile("original_snap", ".wav")
        val bytes = ByteArray(44 + pcm.size * 2)
        ByteBuffer.wrap(bytes, 44, pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm)
        f.writeBytes(bytes)
        tempFiles += f
        return f
    }

    @Test
    fun noteSinkReceivesTheOriginalUnmodifiedWav() = runBlocking {
        // 1s speech, 3s dead air, 1s speech: the preprocessor cuts this for AI,
        // but the note must keep every original sample.
        val pcm = ShortArray(80_000).also {
            ShortArray(16_000) { 2_000 }.copyInto(it, 0)
            ShortArray(16_000) { 2_000 }.copyInto(it, 64_000)
        }
        val wav = wavOf(pcm)
        val originalBytes = wav.readBytes()

        var recordedFile: File? = null
        var recordedBytes: ByteArray? = null
        val sink = object : NoteSink {
            override suspend fun record(
                file: File,
                startEpochMs: Long,
                endEpochMs: Long,
                trigger: TriggerType,
                transcript: TranscriptResult,
                summary: Summary,
            ) {
                recordedFile = file
                recordedBytes = file.readBytes()
            }
        }

        val controller = KnockledgeController(
            FakeSource(),
            NoopSessionHooks,
            RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor()),
            InMemoryRecentsRepository(),
            FakeBuffer(wav),
            sink,
            minAudioSaveSeconds = MutableStateFlow(2),
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals(wav, recordedFile)
        assertTrue(originalBytes.contentEquals(recordedBytes!!))
        controller.stop()
    }

    @Test
    fun preprocessorLeavesTheInputArrayUntouched() {
        val pcm = ShortArray(32_000).also { ShortArray(16_000) { 2_000 }.copyInto(it, 0) }
        val snapshot = pcm.copyOf()
        DefaultAudioPreprocessor().process(pcm, 16_000)
        assertTrue(snapshot.contentEquals(pcm))
    }
}
