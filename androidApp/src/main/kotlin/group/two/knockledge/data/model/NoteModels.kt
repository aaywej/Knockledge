package group.two.knockledge.data.model

/** Sign-in method recorded on the user's profile. */
enum class AuthProvider { GOOGLE, EMAIL_PASSWORD }

/** How far a note's copy in the cloud has progressed. */
enum class SyncStatus { LOCAL_ONLY, SYNCING, SYNCED, SYNC_FAILED }

/** What started a recording: a knock gesture or the record control. */
enum class TriggerType { GESTURE, MANUAL }

/** Where speech-to-text ran for a note. */
enum class TranscriptionEngineType { CLOUD_FIREBASE_AI, LOCAL_WHISPER }

/** Where summarization ran for a note. */
enum class AiEngineType { CLOUD_FIREBASE_AI, LOCAL_ON_DEVICE }

/** Which step of the sync a queued task performs for its note. */
enum class SyncTaskType { UPLOAD_AUDIO, SYNC_FIRESTORE }

/** Task lifecycle; entries marked PROCESSING are skipped while the queue is polled. */
enum class QueueStatus { QUEUED, PROCESSING, FAILED }

/** Account details copied from Firebase Auth after sign-in. */
data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val provider: AuthProvider,
    val createdAtEpochMs: Long,
)

/** Where a note's audio is stored, how long it is and how it was captured. */
data class AudioMetadata(
    val localPath: String,
    val remoteUrl: String? = null,
    val durationMs: Long,
    val sampleRate: Int,
    val triggerType: TriggerType,
)

/** One transcript segment; timings are milliseconds from the start of the audio. */
data class Sentence(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float,
)

/** A note's transcription: the full text plus per-sentence segments and which engine produced it. */
data class TranscriptMetadata(
    val fullText: String,
    val sentences: List<Sentence>,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
)

/** A titled section of the summary, starting at [startTimeMs] into the audio. */
data class ChapterMarker(
    val startTimeMs: Long,
    val title: String,
    val keyBullets: List<String>,
)

/** A note's summary: the gist, key points, chapter markers and which engine produced it. */
data class SummaryMetadata(
    val executiveSummary: String,
    val keyPoints: List<String>,
    val chapterMarkers: List<ChapterMarker>,
    val generationMode: AiEngineType,
)

/** A saved rewind: audio plus optional transcript and summary, with its cloud sync state. */
data class Note(
    val id: String,
    val userId: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val audio: AudioMetadata,
    val transcript: TranscriptMetadata? = null,
    val summary: SummaryMetadata? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)
