package two.knockledge

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RewinderSaveThresholdTest {
    private class DummyBuffer(var currentMillis: Long = 0L) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) {}
        override fun setWindowSeconds(seconds: Int) {}
        override fun bufferedMillis(): Long = currentMillis
        override fun snapshot(): File = File.createTempFile("dummy", ".wav")
        override fun clear() {}
    }

    private class DummySource(val chunks: Int = 0) : AudioSource {
        override fun frames(sampleRate: Int) = flow {
            repeat(chunks) { emit(ShortArray(1600) { 100 }) }
        }
    }

    @Test
    fun canRewindObeysDynamicThreshold() = runBlocking {
        val dummyBuffer = DummyBuffer(4_000L) // 4 seconds buffered
        val minSaveSeconds = MutableStateFlow(5) // Requires 5 seconds
        val pipeline = RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor())

        val controller = KnockledgeController(
            audioSource = DummySource(chunks = 1),
            hooks = NoopSessionHooks,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            buffer = dummyBuffer,
            minAudioSaveSeconds = minSaveSeconds,
        )

        controller.start()
        // Wait until the emitted chunk publishes the buffered amount into state.
        withTimeout(5_000L) {
            while (controller.state.value.bufferedMillis == 0L) delay(10L)
        }
        assertFalse("At 4s with 5s threshold, canRewind must be false", controller.state.value.canRewind)

        // With threshold updated to 4, the 4s buffer unlocks.
        minSaveSeconds.value = 4
        assertTrue("At 4s with 4s threshold, canRewind must be true", controller.state.value.canRewind)
        controller.stop()
    }
}
