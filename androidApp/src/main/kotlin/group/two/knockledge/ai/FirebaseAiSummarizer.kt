package group.two.knockledge.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import group.two.knockledge.pipeline.PromptProvider
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.SummarizerPrompt
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryParser
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Summaries
import group.two.knockledge.settings.SummaryLanguages

/** Summarizes transcripts with cloud Gemini through Firebase AI Logic. */
class FirebaseAiSummarizer(
    private val outputLanguage: () -> String = { SummaryLanguages.AUTO },
    private val promptProvider: PromptProvider = SummarizerPrompt,
) : Summarizer {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = FirebaseGeminiClient.MODEL_ID,
        )
    }

    /** Sends the transcript to Gemini and parses its TITLE / GIST / bullets reply into a [Summary]. */
    override suspend fun summarize(request: SummaryRequest): Summary {
        val lang = outputLanguage()
        val systemPrompt = promptProvider.summarizerSystem(lang)
        val userPrompt = promptProvider.summarizerUser(request, outputLanguage = lang)
        val fullPrompt = "$systemPrompt\n\n$userPrompt"

        return try {
            val response = model.generateContent(content { text(fullPrompt) })
            val replyText = response.text ?: return Summaries.nothingHeard(request.redactedCount, lang)
            SummaryParser.parse(replyText, request.redactedCount)
        } catch (e: Exception) {
            Summaries.nothingHeard(request.redactedCount, lang)
        }
    }
}
