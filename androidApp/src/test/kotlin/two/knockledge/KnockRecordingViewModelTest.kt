package group.two.knockledge

import android.os.Looper
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.KnockRecordingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class KnockRecordingViewModelTest {

    private class TestBuffer : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) = Unit
        override fun setWindowSeconds(seconds: Int) = Unit
        override fun bufferedMillis(): Long = 0L
        override fun snapshot(): File = File.createTempFile("snap", ".wav")
        override fun clear() = Unit
    }

    private class TestDetector : KnockGestureDetector {
        override var isListening = false
            private set
        override fun startListening(onKnockDetected: () -> Unit) { isListening = true }
        override fun stopListening() { isListening = false }
        override fun setPeakThreshold(threshold: Float) = Unit
    }

    private class EmptySource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = emptyFlow()
    }

    private fun createController(): KnockledgeController {
        val capturer = RingAudioCapturer(EmptySource(), TestBuffer())
        val pipeline = RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor())
        return KnockledgeController(
            capturer = capturer,
            knockDetector = TestDetector(),
            hooks = NoopSessionHooks,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
        )
    }

    /** Runs queued main-looper coroutines until [condition] holds or the timeout hits. */
    private suspend fun awaitUi(condition: () -> Boolean) {
        withTimeout(5_000L) {
            while (!condition()) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(10L)
            }
        }
    }

    @Test
    fun startAndStopReflectInUiState() = runBlocking {
        val controller = createController()
        val settings = AppSettings(MemoryStore(), "m")
        val vm = KnockRecordingViewModel(controller, settings)

        awaitUi { !vm.uiState.value.isRecording }

        vm.startSession()
        awaitUi { vm.uiState.value.isRecording }
        assertTrue(vm.uiState.value.isGestureActive)

        vm.stopSession()
        awaitUi { !vm.uiState.value.isRecording }
    }

    @Test
    fun bufferDurationClampingPropagatesToUiState() = runBlocking {
        val controller = createController()
        val settings = AppSettings(MemoryStore(), "m")
        val vm = KnockRecordingViewModel(controller, settings)

        awaitUi { vm.uiState.value.maxBufferSeconds == 300 }
        assertEquals(300, vm.uiState.value.maxBufferSeconds)

        vm.setBufferDurationSeconds(120)
        awaitUi { vm.uiState.value.maxBufferSeconds == 120 }
        assertEquals(120, vm.uiState.value.maxBufferSeconds)
    }
}
