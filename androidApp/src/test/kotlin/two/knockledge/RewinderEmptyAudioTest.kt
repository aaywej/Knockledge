package two.knockledge

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Rewinds whose audio holds under 1.0s of speech must skip AI and persistence. */
class RewinderEmptyAudioTest {

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

    private class RecordingSink : NoteSink {
        var recordCalls = 0
        var audioOnlyCalls = 0
        override suspend fun record(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
            transcript: group.two.knockledge.pipeline.TranscriptResult,
            summary: group.two.knockledge.pipeline.Summary,
        ) {
            recordCalls++
        }

        override suspend fun recordAudioOnly(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
        ) {
            audioOnlyCalls++
        }
    }

    @AfterTest
    fun cleanup() = tempFiles.forEach { it.delete() }

    private fun silentWav(): File {
        val f = File.createTempFile("empty_snap", ".wav")
        f.writeBytes(ByteArray(44) + ByteArray(64_000))
        tempFiles += f
        return f
    }

    @Test
    fun silentRewindReportsErrorSkipsPersistenceAndDeletesWav() = runBlocking {
        val wav = silentWav()
        val recents = InMemoryRecentsRepository()
        val sink = RecordingSink()
        val pipeline = RewindPipeline(
            PlaceholderTranscriber(),
            PlaceholderSummarizer(),
            object : AudioPreprocessor {
                override fun process(pcm: ShortArray, sampleRate: Int): AudioPreprocessResult =
                    AudioPreprocessResult.InsufficientSpeech(0L)
            },
        )
        val controller = KnockledgeController(
            FakeSource(),
            NoopSessionHooks,
            pipeline,
            recents,
            FakeBuffer(wav),
            sink,
            minAudioSaveSeconds = kotlinx.coroutines.flow.MutableStateFlow(2),
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals("No speech detected in recorded audio.", controller.state.value.error)
        assertTrue(recents.recents.value.isEmpty())
        assertEquals(0, sink.recordCalls)
        assertEquals(0, sink.audioOnlyCalls)
        assertFalse(wav.exists())
        controller.stop()
    }
}
