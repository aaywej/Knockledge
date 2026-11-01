package two.knockledge.pipeline

import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.PipelineResult
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.RewindStage
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RewindPipelineTest {

    private val transcript = TranscriptResult(
        sentences = emptyList(),
        fullText = "speaker said a password is hunter2",
        isDiarized = false,
        detectedLanguage = "en",
        engineType = TranscriptionEngineType.LOCAL_WHISPER,
    )

    private class FakePreprocessor(
        private val result: AudioPreprocessResult,
    ) : AudioPreprocessor {
        var calls = 0
        override fun process(pcm: ShortArray, sampleRate: Int): AudioPreprocessResult {
            calls++
            return result
        }
    }

    private class FakeTranscriber(private val result: TranscriptResult) : Transcriber {
        var calls = 0
        var receivedPcm: ShortArray? = null
        override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult {
            calls++
            receivedPcm = pcm.copyOf()
            return result
        }
    }

    private class FakeSummarizer : Summarizer {
        var calls = 0
        var request: SummaryRequest? = null
        override suspend fun summarize(request: SummaryRequest): Summary {
            calls++
            this.request = request
            return Summary("t", "g", emptyList(), request.redactedCount)
        }
    }

    @Test
    fun validSpeechRunsAllStagesAndSkipsFiltering() = runBlocking {
        val conditioned = ShortArray(16_000) { 1_000 }
        val preprocessor = FakePreprocessor(AudioPreprocessResult.Success(conditioned, 1_000L))
        val transcriber = FakeTranscriber(transcript)
        val summarizer = FakeSummarizer()
        val stages = mutableListOf<RewindStage>()
        var callbackTranscript: TranscriptResult? = null

        val pipeline = RewindPipeline(transcriber, summarizer, preprocessor)
        val result = pipeline.run(
            ShortArray(48_000) { 500 },
            16_000,
            3_000L,
            onStage = { stages += it },
            onTranscript = { callbackTranscript = it },
        )

        val success = assertIs<PipelineResult.Success>(result)
        assertEquals(1, preprocessor.calls)
        assertEquals(1, transcriber.calls)
        assertEquals(1, summarizer.calls)
        assertEquals(
            listOf(RewindStage.Preprocessing, RewindStage.Transcribing, RewindStage.Summarizing),
            stages,
        )
        // The conditioned (not the raw) PCM is what reaches the transcriber.
        assertTrue(ShortArray(16_000) { 1_000 }.contentEquals(transcriber.receivedPcm!!))
        assertEquals(transcript, callbackTranscript)
        assertEquals(transcript, success.transcript)
        // Transcript flows to the summarizer unredacted.
        assertEquals(transcript.fullText, summarizer.request!!.filteredTranscript)
        assertEquals(0, summarizer.request!!.redactedCount)
        assertEquals(0, success.summary.redactedCount)
    }

    @Test
    fun insufficientSpeechAbortsBeforeTranscriptionAndSummarization() = runBlocking {
        val preprocessor = FakePreprocessor(AudioPreprocessResult.InsufficientSpeech(400L))
        val transcriber = FakeTranscriber(transcript)
        val summarizer = FakeSummarizer()
        val stages = mutableListOf<RewindStage>()

        val pipeline = RewindPipeline(transcriber, summarizer, preprocessor)
        val result = pipeline.run(ShortArray(48_000), 16_000, 3_000L, onStage = { stages += it })

        val insufficient = assertIs<PipelineResult.InsufficientSpeech>(result)
        assertEquals(400L, insufficient.detectedSpeechMillis)
        assertEquals(0, transcriber.calls)
        assertEquals(0, summarizer.calls)
        assertEquals(listOf(RewindStage.Preprocessing), stages)
    }
}
