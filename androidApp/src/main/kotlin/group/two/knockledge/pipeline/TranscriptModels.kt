package group.two.knockledge.pipeline

import group.two.knockledge.data.model.ChapterMarker

/** Where transcription runs: Firebase AI in the cloud, or Whisper on the device. */
enum class TranscriptionEngineType {
    CLOUD_FIREBASE_AI,
    LOCAL_WHISPER,
}

/** A stretch of 16-bit PCM; [isGainNormalized] marks audio that was levelled before transcription. */
data class AudioSegment(
    val pcm: ShortArray,
    val sampleRate: Int,
    val durationMillis: Long,
    val isGainNormalized: Boolean = false,
)

/** One speaker turn with its span inside the captured audio, in milliseconds from the start. */
data class TranscriptSentence(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

/** What one transcription run produced; [isDiarized] means [sentences] carry speaker labels. */
data class TranscriptResult(
    val sentences: List<TranscriptSentence>,
    val fullText: String,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
    val chapters: List<ChapterMarker> = emptyList(),
)
