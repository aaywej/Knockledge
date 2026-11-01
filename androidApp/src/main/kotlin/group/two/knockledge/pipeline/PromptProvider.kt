package group.two.knockledge.pipeline

import android.content.Context
import group.two.knockledge.R
import group.two.knockledge.core.formatDuration
import group.two.knockledge.settings.SummaryLanguages
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract for resolving system and user prompts across transcription and summarization engines.
 */
interface PromptProvider {
    /** Prompt used for Gemini audio transcription. */
    val geminiTranscribePrompt: String

    /** Speech hint prompt biasing Whisper recognition towards Taglish terms. */
    val taglishPrompt: String

    /** Returns the system prompt for summarizing recaps in [outputLanguage]. */
    fun summarizerSystem(outputLanguage: String = SummaryLanguages.AUTO): String

    /** Returns the user prompt packaging [request] up to [maxChars] in [outputLanguage]. */
    fun summarizerUser(
        request: SummaryRequest,
        maxChars: Int = SummarizerPrompt.MAX_TRANSCRIPT_CHARS,
        outputLanguage: String = SummaryLanguages.AUTO,
    ): String
}

/**
 * Android resource-backed [PromptProvider] resolving prompt fixtures from `prompts.xml`.
 */
@Singleton
class ResourcePromptProvider @Inject constructor(
    private val context: Context,
) : PromptProvider {

    override val geminiTranscribePrompt: String
        get() = context.getString(R.string.prompt_gemini_transcribe)

    override val taglishPrompt: String
        get() = context.getString(R.string.prompt_taglish_hint)

    override fun summarizerSystem(outputLanguage: String): String {
        val ruleRes = when (outputLanguage) {
            SummaryLanguages.FILIPINO -> R.string.prompt_lang_rule_filipino
            SummaryLanguages.ENGLISH -> R.string.prompt_lang_rule_english
            else -> R.string.prompt_lang_rule_auto
        }
        val rule = context.getString(ruleRes)
        return context.getString(R.string.prompt_summarizer_system, rule)
    }

    override fun summarizerUser(
        request: SummaryRequest,
        maxChars: Int,
        outputLanguage: String,
    ): String {
        val text = request.filteredTranscript.trim()
        val clipped = if (text.length > maxChars) "... " + text.takeLast(maxChars) else text
        val askRes = when (outputLanguage) {
            SummaryLanguages.FILIPINO -> R.string.prompt_ask_filipino
            SummaryLanguages.ENGLISH -> R.string.prompt_ask_english
            else -> R.string.prompt_ask_auto
        }
        val ask = context.getString(askRes)
        val duration = formatDuration(request.durationMillis)
        return context.getString(R.string.prompt_summarizer_user, duration, clipped, ask)
    }
}
