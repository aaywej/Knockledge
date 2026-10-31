package group.two.knockledge.data.repository

import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.ChapterMarkerInput
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SentenceInput
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.local.SyncQueueEntity
import group.two.knockledge.data.local.toEntity
import group.two.knockledge.data.local.toNote
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.QueueStatus
import group.two.knockledge.data.model.Sentence
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.model.TranscriptMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

/**
 * Room-backed [NoteRepository]. Audio is moved into [audioDir] on creation and every write also
 * queues sync work, so a saved note eventually reaches Storage and Firestore.
 */
class NoteRepositoryImpl(
    private val notes: NoteDao,
    private val queue: SyncQueueDao,
    private val audioDir: File,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val onSync: suspend (String) -> Unit = {},
) : NoteRepository {

    /** Emits the user's notes newest first, and re-emits whenever Room changes the table. */
    override fun observeNotes(userId: String): Flow<List<Note>> =
        notes.observeNotes(userId).map { list -> list.map { it.toNote() } }

    /** Null when the note does not exist or belongs to another user. */
    override suspend fun getNote(id: String, userId: String): Note? = notes.getNote(id, userId)?.toNote()

    /**
     * Moves [snapshot]'s file to `[audioDir]/<id>.wav`, inserts a local-only note titled from the
     * creation time, and queues an audio upload plus a Firestore sync.
     *
     * @throws IllegalArgumentException when the audio is not 1 to 300 seconds long or not 16 kHz.
     */
    override suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note {
        require(snapshot.durationMs in 1_000..300_000) { "Audio must be 1 to 300 seconds" }
        require(snapshot.sampleRate == 16_000) { "Audio must be 16 kHz" }
        val id = newId()
        val target = File(audioDir.apply { mkdirs() }, "$id.wav")
        if (!snapshot.file.renameTo(target)) {
            snapshot.file.copyTo(target, overwrite = true)
            snapshot.file.delete()
        }
        val t = now()
        val note = Note(
            id = id,
            userId = userId,
            title = "Note $t",
            createdAtEpochMs = t,
            updatedAtEpochMs = t,
            audio = AudioMetadata(target.absolutePath, null, snapshot.durationMs, snapshot.sampleRate, snapshot.triggerType),
        )
        notes.upsert(note.toEntity())
        enqueue(note, SyncTaskType.UPLOAD_AUDIO)
        enqueue(note, SyncTaskType.SYNC_FIRESTORE)
        return note
    }

    /** Replaces any existing transcript, drops the note back to local-only and queues a Firestore sync. */
    override suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note =
        update(noteId, userId) {
            it.copy(
                transcript = TranscriptMetadata(
                    transcript.fullText,
                    transcript.sentences.map(SentenceInput::toSentence),
                    transcript.isDiarized,
                    transcript.detectedLanguage,
                    transcript.engineType,
                ),
            )
        }

    /** Keeps the current title when [summary]'s title is blank; markers are stored sorted by start time. */
    override suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note =
        update(noteId, userId) {
            it.copy(
                title = summary.title.ifBlank { it.title },
                summary = SummaryMetadata(
                    summary.executiveSummary,
                    summary.keyPoints,
                    summary.chapterMarkers.map(ChapterMarkerInput::toMarker).sortedBy { m -> m.startTimeMs },
                    summary.generationMode,
                ),
            )
        }

    /** Deletes the row first, then its audio file; an unknown id is a no-op. */
    override suspend fun deleteNote(id: String, userId: String) {
        val note = notes.getNote(id, userId) ?: return
        notes.delete(id, userId)
        File(note.audioLocalPath).delete()
    }

    /** Hands off to the [onSync] callback given at construction; this class does not sync itself. */
    override suspend fun syncPending(userId: String) = onSync(userId)

    private suspend fun update(id: String, userId: String, change: (Note) -> Note): Note {
        val current = requireNotNull(getNote(id, userId)) { "Note not found" }
        val next = change(current).copy(updatedAtEpochMs = now(), syncStatus = SyncStatus.LOCAL_ONLY)
        notes.upsert(next.toEntity())
        enqueue(next, SyncTaskType.SYNC_FIRESTORE)
        return next
    }

    private suspend fun enqueue(note: Note, type: SyncTaskType) = queue.insert(
        SyncQueueEntity(
            queueId = newId(),
            noteId = note.id,
            userId = note.userId,
            taskType = type,
            status = QueueStatus.QUEUED,
            updatedAtEpochMs = now(),
        ),
    )
}

private fun SentenceInput.toSentence() = Sentence(speaker, startTimeMs, endTimeMs, text, confidence)

private fun ChapterMarkerInput.toMarker() = ChapterMarker(startTimeMs, title, keyBullets)
