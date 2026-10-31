package two.knockledge

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngineType
import group.two.knockledge.sensor.KnockGestureDetector
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnockledgeControllerSessionTest {

    private class FakeSource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = flow { emit(ShortArray(16)) }
    }

    private class FakeBuffer(private val wav: File) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) = Unit
        override fun setWindowSeconds(seconds: Int) = Unit
        override fun bufferedMillis(): Long = 3_000L
        override fun snapshot(): File = wav
        override fun clear() = Unit
    }

    private class FakeDetector : KnockGestureDetector {
        override var isListening = false
            private set
        private var onKnockCallback: (() -> Unit)? = null
        override fun startListening(onKnockDetected: () -> Unit) {
            isListening = true
            onKnockCallback = onKnockDetected
        }
        override fun stopListening() {
            isListening = false
            onKnockCallback = null
        }
        override fun setPeakThreshold(threshold: Float) = Unit
        fun triggerKnock() = onKnockCallback?.invoke()
    }

    private fun wav(): File {
        val f = File.createTempFile("snap", ".wav")
        val pcm = ByteArray(32_000)
        java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer().put(ShortArray(16_000) { 2_000 })
        f.writeBytes(ByteArray(44) + pcm)
        return f
    }

    @Test
    fun startAndStopControlBothAudioAndKnockDetector() = runBlocking {
        val capturer = RingAudioCapturer(FakeSource(), FakeBuffer(wav()))
        val detector = FakeDetector()
        val pipeline = RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor())
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = detector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            hooks = NoopSessionHooks,
        )

        assertFalse(controller.state.value.isRunning)
        assertFalse(detector.isListening)

        controller.start()
        withTimeout(5000L) {
            while (!controller.state.value.isRunning) delay(10L)
        }
        assertTrue(detector.isListening)

        controller.stop()
        withTimeout(5000L) {
            while (controller.state.value.isRunning) delay(10L)
        }
        assertFalse(detector.isListening)
    }

    @Test
    fun knockGestureTriggersRewindAndPersistsNote() = runBlocking {
        val latch = CountDownLatch(1)
        var recordedTrigger: TriggerType? = null
        val sink = NoteSink { _, _, _, trigger, _, _ ->
            recordedTrigger = trigger
            latch.countDown()
        }
        val pipeline = RewindPipeline(
            object : Transcriber {
                override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult =
                    TranscriptResult(emptyList(), "test transcript", false, "en", TranscriptionEngineType.LOCAL_WHISPER)
            },
            object : Summarizer {
                override suspend fun summarize(request: SummaryRequest): Summary = Summary("title", "gist", emptyList(), 0)
            },
            DefaultAudioPreprocessor(),
        )
        val capturer = RingAudioCapturer(FakeSource(), FakeBuffer(wav()))
        val detector = FakeDetector()
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = detector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            noteSink = sink,
            hooks = NoopSessionHooks,
            minAudioSaveSeconds = MutableStateFlow(2),
        )

        controller.start()
        withTimeout(5000L) {
            while (!controller.state.value.canRewind) delay(10L)
        }

        detector.triggerKnock()
        assertTrue(latch.await(5, TimeUnit.SECONDS), "NoteSink was not notified on gesture")
        assertEquals(TriggerType.GESTURE, recordedTrigger)
        controller.stop()
    }
}
