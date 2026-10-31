import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.InputStatus
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KnockledgeControllerAutoAiTest {

    private val tempDir: File = Files.createTempDirectory("controller_test").toFile()

    private class RecordingNoteSink : NoteSink {
        var recordedAudioOnlyCount = 0
        var recordedFullCount = 0
        var lastTrigger: TriggerType? = null

        override suspend fun record(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
            transcript: TranscriptResult,
            summary: Summary,
        ) {
            recordedFullCount++
            lastTrigger = trigger
        }

        override suspend fun recordAudioOnly(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
        ) {
            recordedAudioOnlyCount++
            lastTrigger = trigger
        }
    }

    private class NoOpAudioSource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = kotlinx.coroutines.flow.flow { emit(ShortArray(1_600) { 100 }) }
        override fun status(): InputStatus = InputStatus("mic", null)
    }

    private class NoOpHooks : SessionHooks {
        override fun onSessionStarted() = Unit
        override fun onSessionStopped() = Unit
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun rewindSavesAudioOnlyWhenAutoAiIsDisabled() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(false)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000))

        val controller = KnockledgeController(
            audioSource = NoOpAudioSource(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            recents = InMemoryRecentsRepository(),
            buffer = ringBuffer,
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.GESTURE)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals(1, sink.recordedAudioOnlyCount)
        assertEquals(0, sink.recordedFullCount)
        assertEquals(TriggerType.GESTURE, sink.lastTrigger)
    }

    @Test
    fun rewindExecutesPipelineWhenAutoAiIsEnabled() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000) { 2_000 })

        val controller = KnockledgeController(
            audioSource = NoOpAudioSource(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            recents = InMemoryRecentsRepository(),
            buffer = ringBuffer,
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals(0, sink.recordedAudioOnlyCount)
        assertEquals(1, sink.recordedFullCount)
        assertEquals(TriggerType.MANUAL, sink.lastTrigger)
    }
}
