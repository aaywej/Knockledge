package two.knockledge.audio

import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingAudioCapturerTest {

    private class FakeBuffer(var millis: Long = 0L) : LocalAudioRingBuffer {
        var writtenChunks = 0
        var cleared = false
        var windowSecondsSet = 0
        override fun write(chunk: ShortArray, length: Int) {
            writtenChunks++
            millis += 100L
        }
        override fun setWindowSeconds(seconds: Int) {
            windowSecondsSet = seconds
        }
        override fun bufferedMillis(): Long = millis
        override fun snapshot(): File = File.createTempFile("test_snap", ".wav")
        override fun clear() {
            cleared = true
            millis = 0L
        }
    }

    private class ChunkSource(private val count: Int = 3) : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = flow {
            repeat(count) {
                emit(ShortArray(1600) { 1000 })
            }
        }
    }

    @Test
    fun startCapturesChunksAndUpdatesState() = runBlocking {
        val buffer = FakeBuffer()
        val capturer = RingAudioCapturer(
            audioSource = ChunkSource(count = 3),
            buffer = buffer,
        )

        assertFalse(capturer.state.value.isRunning)
        capturer.start()
        assertTrue(capturer.state.value.isRunning)

        withTimeout(3000L) {
            while (buffer.writtenChunks < 3) delay(10L)
        }

        assertTrue(capturer.state.value.bufferedMillis > 0L)
        capturer.stop()
        assertFalse(capturer.state.value.isRunning)
        withTimeout(3000L) {
            while (!buffer.cleared) delay(10L)
        }
    }

    @Test
    fun dynamicWindowUpdatesPropagateToBuffer() = runBlocking {
        val buffer = FakeBuffer()
        val windowSeconds = MutableStateFlow(60)
        val capturer = RingAudioCapturer(
            audioSource = ChunkSource(count = 0),
            buffer = buffer,
            windowSeconds = windowSeconds,
        )

        withTimeout(3000L) {
            while (buffer.windowSecondsSet != 60) delay(10L)
        }

        windowSeconds.value = 180
        withTimeout(3000L) {
            while (buffer.windowSecondsSet != 180) delay(10L)
        }
        assertEquals(180_000L, capturer.state.value.windowMillis)
    }
}
