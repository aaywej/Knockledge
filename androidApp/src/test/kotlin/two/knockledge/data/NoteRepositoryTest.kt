package group.two.knockledge.data

import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.NoteEntity
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.local.SyncQueueEntity
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.repository.NoteRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteRepositoryTest {
    private class FakeNoteDao : NoteDao {
        val rows = MutableStateFlow<Map<String, NoteEntity>>(emptyMap())
        override fun observeNotes(userId: String): Flow<List<NoteEntity>> =
            rows.map { m -> m.values.filter { it.userId == userId }.sortedByDescending { it.createdAtEpochMs } }
        override suspend fun getNote(id: String, userId: String) = rows.value[id]?.takeIf { it.userId == userId }
        override suspend fun upsert(note: NoteEntity) { rows.value = rows.value + (note.id to note) }
        override suspend fun delete(id: String, userId: String) {
            if (getNote(id, userId) != null) rows.value = rows.value - id
        }
    }

    private class FakeQueue : SyncQueueDao {
        val items = mutableListOf<SyncQueueEntity>()
        override suspend fun insert(item: SyncQueueEntity) { items += item }
        override suspend fun pending(userId: String) = items.filter { it.userId == userId }
        override suspend fun delete(queueId: String) { items.removeAll { it.queueId == queueId } }
    }

    private val dir = createTempDirectory().toFile()
    private val dao = FakeNoteDao()
    private val queue = FakeQueue()
    private var n = 0
    private val repo = NoteRepositoryImpl(dao, queue, File(dir, "audio"), { 1_000L }, { "id${n++}" })

    private fun snapshot(durationMs: Long = 30_000, sampleRate: Int = 16_000) = AudioSnapshotInput(
        file = File(dir, "snap$n.wav").apply { writeText("RIFF") },
        startEpochMs = 0,
        endEpochMs = durationMs,
        durationMs = durationMs,
        sampleRate = sampleRate,
    )

    @Test fun createMovesFileAndStoresLocalOnly() = runBlocking {
        val s = snapshot()
        val note = repo.createFromSnapshot(s, "u1")
        assertFalse(s.file.exists())
        assertTrue(File(note.audio.localPath).exists())
        assertTrue(note.audio.localPath.endsWith("${note.id}.wav"))
        assertEquals(SyncStatus.LOCAL_ONLY, note.syncStatus)
        assertEquals(note, repo.getNote(note.id, "u1")?.copy())
    }

    @Test fun createStaysUnder200ms() = runBlocking {
        val s = snapshot()
        val t = System.nanoTime()
        repo.createFromSnapshot(s, "u1")
        assertTrue((System.nanoTime() - t) / 1_000_000 < 200)
    }

    @Test fun createEnqueuesSync() = runBlocking {
        val note = repo.createFromSnapshot(snapshot(), "u1")
        val types = queue.pending("u1").filter { it.noteId == note.id }.map { it.taskType }
        assertEquals(listOf(SyncTaskType.UPLOAD_AUDIO, SyncTaskType.SYNC_FIRESTORE), types)
    }

    @Test fun invalidSnapshotRejected() = runBlocking {
        assertFailsWith<IllegalArgumentException> { repo.createFromSnapshot(snapshot(durationMs = 500), "u1") }
        assertFailsWith<IllegalArgumentException> { repo.createFromSnapshot(snapshot(sampleRate = 44_100), "u1") }
        Unit
    }

    @Test fun attachTranscriptAndSummary() = runBlocking {
        val note = repo.createFromSnapshot(snapshot(), "u1")
        val t = repo.attachTranscript(
            note.id, "u1",
            TranscriptInput("hi", listOf(SentenceInput("S1", 0, 10, "hi")), true, "en", TranscriptionEngineType.LOCAL_WHISPER),
        )
        assertEquals("hi", t.transcript?.fullText)
        val s = repo.attachSummary(
            note.id, "u1",
            SummaryInput("Title", "sum", listOf("k"), listOf(ChapterMarkerInput(0, "c", listOf("b"))), AiEngineType.LOCAL_ON_DEVICE),
        )
        assertEquals("Title", s.title)
        assertEquals("hi", s.transcript?.fullText)
        assertEquals("sum", s.summary?.executiveSummary)
    }

    @Test fun otherUserCannotTouchNote() = runBlocking {
        val note = repo.createFromSnapshot(snapshot(), "u1")
        assertNull(repo.getNote(note.id, "u2"))
        assertTrue(repo.observeNotes("u2").first().isEmpty())
        assertEquals(1, repo.observeNotes("u1").first().size)
        assertFailsWith<IllegalArgumentException> {
            repo.attachSummary(note.id, "u2", SummaryInput("t", "s", emptyList(), emptyList(), AiEngineType.LOCAL_ON_DEVICE))
        }
        Unit
    }

    @Test fun deleteRemovesNoteAndAudio() = runBlocking {
        val note = repo.createFromSnapshot(snapshot(), "u1")
        repo.deleteNote(note.id, "u1")
        assertNull(repo.getNote(note.id, "u1"))
        assertFalse(File(note.audio.localPath).exists())
    }
}

