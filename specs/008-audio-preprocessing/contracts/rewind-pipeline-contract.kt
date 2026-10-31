package group.two.knockledge.pipeline

/**
 * Stages of the rewind pipeline reported to the UI during processing.
 */
enum class RewindStage {
    /** Audio conditioning: dead air eradication and normalization. */
    Preprocessing,

    /** Speech recognition transcribing audio to text. */
    Transcribing,

    /** Large language model distilling the transcript into a summary. */
    Summarizing,
}

/**
 * Terminal result of running the rewind pipeline.
 */
sealed interface PipelineResult {
    /** Pipeline executed successfully and produced a summary and transcript. */
    data class Success(
        val summary: Summary,
        val transcript: TranscriptResult,
    ) : PipelineResult

    /** Pipeline aborted because remaining speech fell below the minimum viable threshold. */
    data class InsufficientSpeech(
        val detectedSpeechMillis: Long,
        val message: String = "No speech detected in recorded audio.",
    ) : PipelineResult
}

/**
 * The end-to-end rewind pipeline interface:
 * Preprocess Audio -> Transcribe -> Summarize.
 *
 * Text filtering is removed. Speech is passed directly from transcription to summarization.
 */
interface RewindPipelineContract {
    /**
     * Executes the pipeline once.
     *
     * @param pcm The raw PCM buffer read from the snapshot file.
     * @param sampleRate Audio sampling rate in Hz.
     * @param durationMillis Duration of the raw audio buffer.
     * @param onStage Progress callback for UI updates.
     * @param onTranscript Callback invoked as soon as transcription completes.
     * @return [PipelineResult.Success] on success, or [PipelineResult.InsufficientSpeech] if speech is under 1.0s.
     */
    suspend fun run(
        pcm: ShortArray,
        sampleRate: Int,
        durationMillis: Long,
        onStage: (RewindStage) -> Unit = {},
        onTranscript: (TranscriptResult) -> Unit = {},
    ): PipelineResult
}
