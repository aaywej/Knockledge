package group.two.knockledge.data.local

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.QueueStatus
import group.two.knockledge.data.model.Sentence
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.model.TriggerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class KnockledgeDatabaseTest {

    private lateinit var db: KnockledgeDatabase

    @BeforeTest
    fun open() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            KnockledgeDatabase::class.java,
        ).build()
    }

    @AfterTest
    fun close() = db.close()

    private fun note(id: String, userId: String, t: Long = 1L) = NoteEntity(
        id = id,
        userId = userId,
        title = "Note $id",
        createdAtEpochMs = t,
        updatedAtEpochMs = t,
        audioLocalPath = "/data/$id.wav",
        audioRemoteUrl = null,
        audioDurationMs = 30_000L,
        audioSampleRate = 16_000,
        audioTriggerType = TriggerType.GESTURE,
        transcriptFullText = null,
        transcriptSentences = null,
        transcriptIsDiarized = null,
        transcriptDetectedLanguage = null,
        transcriptEngineType = null,
        summaryExecutiveSummary = null,
        summaryKeyPoints = null,
        summaryChapterMarkers = null,
        summaryGenerationMode = null,
        syncStatus = SyncStatus.LOCAL_ONLY,
    )

    private fun queueItem(note: NoteEntity, type: SyncTaskType) = SyncQueueEntity(
        queueId = "q-${note.id}-${type.name}",
        noteId = note.id,
        userId = note.userId,
        taskType = type,
        status = QueueStatus.QUEUED,
        updatedAtEpochMs = 1L,
    )

    @Test
    fun schemaOpensAndPersistsNote() = runBlocking {
        db.noteDao().upsert(note("n1", "u1"))
        assertEquals("n1", db.noteDao().getNote("n1", "u1")?.id)
    }

    @Test
    fun observeNotesFiltersStrictlyByUserId() = runBlocking {
        db.noteDao().upsert(note("a1", "alice", 1L))
        db.noteDao().upsert(note("a2", "alice", 2L))
        db.noteDao().upsert(note("b1", "bob", 3L))

        assertEquals(listOf("a2", "a1"), db.noteDao().observeNotes("alice").first().map { it.id })
        assertEquals(listOf("b1"), db.noteDao().observeNotes("bob").first().map { it.id })
        assertNull(db.noteDao().getNote("b1", "alice"))
    }

    @Test
    fun syncQueueEnqueueAndDelete() = runBlocking {
        val n = note("n1", "u1")
        db.noteDao().upsert(n)
        db.syncQueueDao().insert(queueItem(n, SyncTaskType.UPLOAD_AUDIO))
        db.syncQueueDao().insert(queueItem(n, SyncTaskType.SYNC_FIRESTORE))

        assertEquals(2, db.syncQueueDao().pending("u1").size)

        db.syncQueueDao().delete("q-n1-UPLOAD_AUDIO")
        val left = db.syncQueueDao().pending("u1")
        assertEquals(1, left.size)
        assertEquals(SyncTaskType.SYNC_FIRESTORE, left[0].taskType)
    }

    @Test
    fun transcriptAndSummaryConvertersRoundTrip() = runBlocking {
        val n = note("n1", "u1").copy(
            transcriptFullText = "Speaker 1: Hi",
            transcriptSentences = listOf(
                Sentence("Speaker 1", 0L, 3_200L, "Hi there.", 0.95f),
                Sentence("Speaker 2", 3_300L, 5_000L, "Hello.", 0.8f),
            ),
            transcriptIsDiarized = true,
            transcriptDetectedLanguage = "en",
            transcriptEngineType = TranscriptionEngineType.CLOUD_FIREBASE_AI,
            summaryExecutiveSummary = "Greetings exchanged.",
            summaryKeyPoints = listOf("Said hi", "Said hello"),
            summaryChapterMarkers = listOf(
                ChapterMarker(0L, "Intro", listOf("Welcome", "Outline")),
            ),
            summaryGenerationMode = AiEngineType.CLOUD_FIREBASE_AI,
        )
        db.noteDao().upsert(n)

        assertEquals(n, db.noteDao().getNote("n1", "u1"))
    }

    @Test
    fun syncQueueRejectsOrphanNoteReference() {
        assertFailsWith<SQLiteConstraintException> {
            runBlocking { db.syncQueueDao().insert(queueItem(note("ghost", "u1"), SyncTaskType.UPLOAD_AUDIO)) }
        }
    }

    @Test
    fun deletingNoteCascadesQueue() = runBlocking {
        val n = note("n1", "u1")
        db.noteDao().upsert(n)
        db.syncQueueDao().insert(queueItem(n, SyncTaskType.UPLOAD_AUDIO))
        db.noteDao().delete("n1", "u1")
        assertTrue(db.syncQueueDao().pending("u1").isEmpty())
    }
}
