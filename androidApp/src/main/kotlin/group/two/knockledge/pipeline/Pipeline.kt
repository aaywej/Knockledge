package group.two.knockledge.pipeline

import group.two.knockledge.core.formatDuration
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.settings.SummaryLanguages

/** Local LLM. Input is the filtered transcript, output is the only thing a user ever sees. */
interface Summarizer {
    /** Turns a filtered transcript into the recap the user reads. */
    suspend fun summarize(request: SummaryRequest): Summary
}

/** Everything the summarizer needs that is not the transcript text itself. */
data class SummaryRequest(
    val filteredTranscript: String,
    val looksLikeConversation: Boolean,
    val redactedCount: Int,
    val durationMillis: Long,
)

/** A rewind card: short title, one-line gist, and a few key points. */
data class Summary(
    val title: String,
    val gist: String,
    val points: List<String>,
    val redactedCount: Int,
    val chapters: List<ChapterMarker> = emptyList(),
)

/** What the pipeline is doing right now, for the UI. */
enum class RewindStage {
    /** Audio conditioning: dead air eradication and normalization. */
    Preprocessing,

    /** Speech recognition transcribing audio to text. */
    Transcribing,

    /** Large language model distilling the transcript into a summary. */
    Summarizing,
}

/** Terminal result of running the rewind pipeline. */
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
 * preprocess -> transcribe -> summarize. The pre-processed PCM array is zero-filled
 * as soon as the transcriber is done with it, success or not.
 */
class RewindPipeline(
    private val transcriber: Transcriber,
    private val summarizer: Summarizer,
    private val preprocessor: AudioPreprocessor,
) {
    /**
     * Runs the full pipeline once. [onStage] reports progress.
     * Returns [PipelineResult.InsufficientSpeech] when the audio holds under 1.0s of speech.
     */
    suspend fun run(
        pcm: ShortArray,
        sampleRate: Int,
        durationMillis: Long,
        onStage: (RewindStage) -> Unit = {},
        onTranscript: (TranscriptResult) -> Unit = {},
    ): PipelineResult {
        onStage(RewindStage.Preprocessing)
        when (val processed = preprocessor.process(pcm, sampleRate)) {
            is AudioPreprocessResult.InsufficientSpeech ->
                return PipelineResult.InsufficientSpeech(processed.detectedSpeechMillis)
            is AudioPreprocessResult.Success -> {
                onStage(RewindStage.Transcribing)
                val transcript = try {
                    transcriber.transcribe(processed.pcm, sampleRate)
                } finally {
                    processed.pcm.fill(0)
                }
                onTranscript(transcript)
                onStage(RewindStage.Summarizing)
                val summary = summarizer.summarize(
                    SummaryRequest(
                        filteredTranscript = labeled(transcript),
                        looksLikeConversation = transcript.isDiarized,
                        redactedCount = 0,
                        durationMillis = durationMillis,
                    )
                )
                return PipelineResult.Success(summary, transcript)
            }
        }
    }

    private fun labeled(t: TranscriptResult): String =
        if (t.isDiarized) t.sentences.joinToString("\n") { "${it.speaker}: ${it.text}" } else t.fullText
}

/**
 * System prompt for the on-device summarizer, kept here so Android and iOS ship
 * the same behavior.
 */
object SummarizerPrompt : PromptProvider {
    override val geminiTranscribePrompt: String =
        "Transcribe this audio verbatim. The language may be English, Filipino or Taglish. " +
            "Separate speaker turns as Speaker 1, Speaker 2, and so on, with millisecond start and end times. " +
            "Also divide the speech into timestamped chapters with titles and key bullet points."

    override val taglishPrompt: String = TranscriptionHints.TAGLISH_PROMPT

    override fun summarizerSystem(outputLanguage: String): String = system(outputLanguage)

    override fun summarizerUser(
        request: SummaryRequest,
        maxChars: Int,
        outputLanguage: String,
    ): String = user(request, maxChars, outputLanguage)

    /** Keeps prompt + answer inside a 4096 token context, even for dense Filipino speech. */
    const val MAX_TRANSCRIPT_CHARS: Int = 8_000

    /** System prompt. [outputLanguage] is one of [SummaryLanguages]. */
    fun system(outputLanguage: String = SummaryLanguages.AUTO): String = """
        You help a person with ADHD catch up on what was said around them in the last few minutes.
        You get a rough automatic transcript. It may be in Filipino, English or Taglish, it may contain recognition mistakes, and it may label turns as Speaker 1, Speaker 2 when voices were told apart.
        Rules:
        - Paraphrase. Never quote sentences or copy the original wording.
        - Speaker labels are only turn markers. Do not guess names. You may say that one speaker said or asked something.
        - Never repeat, guess or describe anything written as [sensitive detail]. You may say that a sensitive detail came up.
        - ${languageRule(outputLanguage)}
        - Be short and concrete: topics, decisions, and anything the listener was asked to do.
        Answer in exactly this format and nothing else (keep the words TITLE and GIST in English):
        TITLE: <at most 5 words>
        GIST: <one sentence saying what this was about>
        - <key point>
        - <key point>
        Use 2 to 5 key points.
    """.trimIndent()

    /** The default (match the speakers), kept for callers that do not pass a language. */
    val SYSTEM: String = system(SummaryLanguages.AUTO)

    private fun languageRule(outputLanguage: String): String = when (outputLanguage) {
        SummaryLanguages.FILIPINO ->
            "Write the title, gist and key points in natural, everyday Filipino (Tagalog), even if the speakers used English. " +
                "Keep names and technical terms as they were said. Do not answer in English."
        SummaryLanguages.ENGLISH ->
            "Write the title, gist and key points in clear, simple English, even if the speakers used Filipino or Taglish."
        else -> "Write in the language the speakers mostly used: English, Filipino or Taglish."
    }

    /** Apple's on-device model has a 4096 token window for input and output together. */
    const val APPLE_MAX_TRANSCRIPT_CHARS: Int = 5_000

    /** User prompt carrying the clipped transcript and the language instruction. */
    fun user(
        request: SummaryRequest,
        maxChars: Int = MAX_TRANSCRIPT_CHARS,
        outputLanguage: String = SummaryLanguages.AUTO,
    ): String {
        val text = request.filteredTranscript.trim()
        // If it is too long, keep the most recent part: that is what the listener just missed.
        val clipped = if (text.length > maxChars) "... " + text.takeLast(maxChars) else text
        val ask = when (outputLanguage) {
            SummaryLanguages.FILIPINO -> "Write the recap in Filipino. Isulat ang buod sa Filipino."
            SummaryLanguages.ENGLISH -> "Write the recap in English."
            else -> "Write the recap."
        }
        return "Transcript of the last ${formatDuration(request.durationMillis)}:\n\"\"\"\n$clipped\n\"\"\"\n$ask"
    }
}

