package group.two.knockledge.pipeline

import group.two.knockledge.core.formatDuration
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
)

/** What the pipeline is doing right now, for the UI. */
enum class RewindStage { Transcribing, Summarizing }

/**
 * transcribe -> filter -> summarize. The PCM array is zero-filled as soon as the
 * transcriber is done with it, success or not.
 */
class RewindPipeline(
    private val transcriber: Transcriber,
    private val filter: SensitiveFilter,
    private val summarizer: Summarizer,
) {
    /** Runs the full pipeline once and returns the summary. [onStage] reports progress. */
    suspend fun run(
        pcm: ShortArray,
        sampleRate: Int,
        durationMillis: Long,
        onStage: (RewindStage) -> Unit = {},
        onTranscript: (TranscriptResult) -> Unit = {},
    ): Summary {
        onStage(RewindStage.Transcribing)
        val transcript = try {
            transcriber.transcribe(pcm, sampleRate)
        } finally {
            pcm.fill(0)
        }
        onTranscript(transcript)
        val filtered = filter.filter(labeled(transcript))
        onStage(RewindStage.Summarizing)
        return summarizer.summarize(
            SummaryRequest(
                filteredTranscript = filtered.text,
                looksLikeConversation = transcript.isDiarized,
                redactedCount = filtered.redactedCount,
                durationMillis = durationMillis,
            )
        )
    }

    private fun labeled(t: TranscriptResult): String =
        if (t.isDiarized) t.sentences.joinToString("\n") { "${it.speaker}: ${it.text}" } else t.fullText
}

/**
 * System prompt for the on-device summarizer, kept here so Android and iOS ship
 * the same behavior.
 */
object SummarizerPrompt {
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

