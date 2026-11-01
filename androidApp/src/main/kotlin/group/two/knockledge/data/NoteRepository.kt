package group.two.knockledge.data

import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.model.TriggerType
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Notes for one user: audio kept locally, transcript and summary attached over time, all queued for cloud sync. */
interface NoteRepository {
    /** The user's notes, newest first; re-emits whenever they change. */
    fun observeNotes(userId: String): Flow<List<Note>>

    /** Null when the note does not exist or belongs to another user. */
    suspend fun getNote(id: String, userId: String): Note?

    /** Imports the recording into app storage and returns the new note; rejects audio outside 1-300 seconds or not at 16 kHz. */
    suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note

    /** Stores the transcript on an existing note and queues it for sync. */
    suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note

    /** Stores the summary on an existing note; a blank title keeps the current one. */
    suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note

    /** Deletes the note and its local audio file. */
    suspend fun deleteNote(id: String, userId: String)

    /** Pushes this user's queued uploads and documents to the backend. */
    suspend fun syncPending(userId: String)
}

/** Recording to import: the file, the span it covers, and what started it. */
data class AudioSnapshotInput(
    val file: File,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMs: Long,
    val sampleRate: Int = 16_000,
    val triggerType: TriggerType = TriggerType.GESTURE,
)

/** Transcript to store, with its sentences and the engine that produced it. */
data class TranscriptInput(
    val fullText: String,
    val sentences: List<SentenceInput>,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
)

/** One timed, speaker-attributed piece of transcript text. */
data class SentenceInput(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

/** Summary to store, including the title it gives the note. */
data class SummaryInput(
    val title: String,
    val executiveSummary: String,
    val keyPoints: List<String>,
    val chapterMarkers: List<ChapterMarkerInput>,
    val generationMode: AiEngineType,
)

/** A chapter heading and the bullets that belong to it. */
data class ChapterMarkerInput(
    val startTimeMs: Long,
    val title: String,
    val keyBullets: List<String>,
)
