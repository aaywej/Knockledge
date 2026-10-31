package group.two.knockledge

import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnockledgeNoteSinkTest {

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

    private val transcript = TranscriptResult(
        sentences = emptyList(),
        fullText = "hello transcript",
        isDiarized = false,
        detectedLanguage = "en",
        engineType = TranscriptionEngineType.LOCAL_WHISPER,
    )

    private fun wav(): File {
        val f = File.createTempFile("snap", ".wav")
        val pcm = ByteArray(32_000)
        java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer().put(ShortArray(16_000) { 2_000 })
        f.writeBytes(ByteArray(44) + pcm)
        return f
    }

    @Test
    fun rewindPassesWavAndTriggerToNoteSink() = runBlocking {
        val latch = CountDownLatch(1)
        var gotTrigger: TriggerType? = null
        var gotStart = -1L
        var gotTranscript: TranscriptResult? = null
        val sink = NoteSink { _, start, _, trigger, t, _ ->
            gotStart = start
            gotTrigger = trigger
            gotTranscript = t
            latch.countDown()
        }
        val pipeline = RewindPipeline(
            object : Transcriber {
                override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult = transcript
            },
            object : Summarizer {
                override suspend fun summarize(request: SummaryRequest): Summary = Summary("t", "g", emptyList(), 0)
            },
            DefaultAudioPreprocessor(),
        )
        val controller = KnockledgeController(
            FakeSource(),
            NoopSessionHooks,
            pipeline,
            InMemoryRecentsRepository(),
            FakeBuffer(wav()),
            sink,
            MutableStateFlow(2),
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)

        assertTrue(latch.await(5, TimeUnit.SECONDS), "note sink was not called")
        assertEquals(TriggerType.MANUAL, gotTrigger)
        assertEquals("hello transcript", gotTranscript?.fullText)
        assertTrue(gotStart > 0)
        controller.stop()
    }
}
