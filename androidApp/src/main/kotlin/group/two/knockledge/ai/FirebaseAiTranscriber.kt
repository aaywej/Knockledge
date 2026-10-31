package group.two.knockledge.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptSentence
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One request to Gemini: a WAV clip plus a text prompt, answered with raw response text. */
interface GeminiClient {
    /** Returns the model's reply for [wav] under [prompt]; implementations may throw on failure. */
    suspend fun generate(wav: ByteArray, prompt: String): String
}

/** Firebase AI Logic client that asks Gemini for a JSON transcript with speaker turns and chapters. */
class FirebaseGeminiClient : GeminiClient {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        "detectedLanguage" to Schema.string(),
                        "sentences" to Schema.array(
                            Schema.obj(
                                mapOf(
                                    "speaker" to Schema.string(),
                                    "startTimeMs" to Schema.long(),
                                    "endTimeMs" to Schema.long(),
                                    "text" to Schema.string(),
                                    "confidence" to Schema.float(),
                                ),
                                optionalProperties = listOf("confidence"),
                            )
                        ),
                        "chapters" to Schema.array(
                            Schema.obj(
                                mapOf(
                                    "startTimeMs" to Schema.long(),
                                    "title" to Schema.string(),
                                    "keyBullets" to Schema.array(Schema.string()),
                                ),
                                optionalProperties = listOf("keyBullets"),
                            )
                        ),
                    ),
                    optionalProperties = listOf("chapters"),
                )
            },
        )
    }

    /** Returns the JSON reply text; throws [IllegalStateException] when Gemini sends nothing. */
    override suspend fun generate(wav: ByteArray, prompt: String): String =
        model.generateContent(content { inlineData(wav, "audio/wav"); text(prompt) }).text
            ?: error("empty Gemini response")

    companion object {
        const val MODEL_ID = "gemini-3.5-flash-lite"
    }
}

@Serializable
private data class GeminiResponse(
    val detectedLanguage: String,
    val sentences: List<GeminiSentence>,
    val chapters: List<GeminiChapter> = emptyList(),
)

@Serializable
private data class GeminiSentence(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

@Serializable
private data class GeminiChapter(
    val startTimeMs: Long,
    val title: String,
    val keyBullets: List<String> = emptyList(),
)

/** Cloud transcription: uploads each segment to Gemini and maps its JSON reply to [TranscriptResult]. */
class FirebaseAiTranscriber(private val client: GeminiClient) : TranscriptionEngine {
    override val engineType = TranscriptionEngineType.CLOUD_FIREBASE_AI

    private val json = Json { ignoreUnknownKeys = true }

    /** Sends [segment] as a WAV plus the transcription prompt; throws if the reply cannot be parsed. */
    override suspend fun process(segment: AudioSegment): TranscriptResult =
        parse(client.generate(wav(segment.pcm, segment.sampleRate), PROMPT))

    private fun parse(raw: String): TranscriptResult {
        val root = json.decodeFromString<GeminiResponse>(raw)
        val sentences = root.sentences.map {
            TranscriptSentence(it.speaker, it.startTimeMs, it.endTimeMs, it.text, it.confidence)
        }
        val chapters = root.chapters.map {
            ChapterMarker(it.startTimeMs, it.title, it.keyBullets)
        }
        return TranscriptResult(
            sentences = sentences,
            fullText = sentences.joinToString(" ") { it.text },
            isDiarized = sentences.map { it.speaker }.distinct().size > 1,
            detectedLanguage = root.detectedLanguage,
            engineType = engineType,
            chapters = chapters,
        )
    }

    /** Wraps mono 16-bit [pcm] in a 44-byte RIFF/WAVE header, which is the shape Gemini accepts. */
    private fun wav(pcm: ShortArray, rate: Int): ByteArray {
        val data = pcm.size * 2
        val b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVEfmt ".toByteArray())
        b.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(data)
        for (s in pcm) b.putShort(s)
        return b.array()
    }

    private companion object {
        const val PROMPT = "Transcribe this audio verbatim. The language may be English, Filipino or Taglish. " +
            "Separate speaker turns as Speaker 1, Speaker 2, and so on, with millisecond start and end times. " +
            "Also divide the speech into timestamped chapters with titles and key bullet points."
    }
}
