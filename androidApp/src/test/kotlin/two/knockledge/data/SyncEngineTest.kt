package group.two.knockledge.data

import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.NoteEntity
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.local.SyncQueueEntity
import group.two.knockledge.data.local.toEntity
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.QueueStatus
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.remote.AudioUploads
import group.two.knockledge.data.remote.NoteSyncSource
import group.two.knockledge.data.repository.SyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncEngineTest {

    private class FakeNotes : NoteDao {
        val store = mutableMapOf<String, NoteEntity>()

        override fun observeNotes(userId: String): Flow<List<NoteEntity>> = flowOf(emptyList())

        override suspend fun getNote(id: String, userId: String): NoteEntity? = store[id]

        override suspend fun upsert(note: NoteEntity) {
            store[note.id] = note
        }

        override suspend fun delete(id: String, userId: String) {
            store.remove(id)
        }
    }

    private class FakeQueue : SyncQueueDao {
        val items = mutableMapOf<String, SyncQueueEntity>()

        override suspend fun insert(item: SyncQueueEntity) {
            items[item.queueId] = item
        }

        override suspend fun pending(userId: String): List<SyncQueueEntity> =
            items.values.filter { it.userId == userId && it.status != QueueStatus.PROCESSING }
                .sortedBy { it.updatedAtEpochMs }

        override suspend fun delete(queueId: String) {
            items.remove(queueId)
        }
    }

    private class FakeUploads(private val failures: Int = 0) : AudioUploads {
        var calls = 0
            private set

        override suspend fun uploadAudio(userId: String, noteId: String, file: File): String {
            calls++
            if (calls <= failures) throw IOException("offline")
            return "https://storage/$userId/$noteId.wav"
        }
    }

    private class FakeRemote : NoteSyncSource {
        val docs = mutableListOf<Map<String, Any?>>()

        override suspend fun upsertNote(userId: String, noteId: String, document: Map<String, Any?>) {
            docs += document
        }
    }

    private fun note(id: String = "n1", userId: String = "u1") = Note(
        id = id,
        userId = userId,
        title = "Lecture",
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        audio = AudioMetadata("/tmp/$id.wav", null, 60_000L, 16_000, TriggerType.GESTURE),
    )

    private fun task(note: Note, type: SyncTaskType, t: Long) = SyncQueueEntity(
        queueId = "q-${type.name}",
        noteId = note.id,
        userId = note.userId,
        taskType = type,
        status = QueueStatus.QUEUED,
        updatedAtEpochMs = t,
    )

    private fun engine(
        notes: FakeNotes,
        queue: FakeQueue,
        uploads: FakeUploads,
        remote: FakeRemote,
        sleeps: MutableList<Long> = mutableListOf(),
        maxAttempts: Int = 4,
    ) = SyncEngine(notes, queue, uploads, remote, sleep = { sleeps += it }, maxAttempts = maxAttempts)

    @Test
    fun processesQueueUploadsAudioAndMarksSynced() = runBlocking {
        val notes = FakeNotes()
        val queue = FakeQueue()
        val uploads = FakeUploads()
        val remote = FakeRemote()
        val sleeps = mutableListOf<Long>()
        val n = note()
        notes.upsert(n.toEntity())
        queue.insert(task(n, SyncTaskType.UPLOAD_AUDIO, 1))
        queue.insert(task(n, SyncTaskType.SYNC_FIRESTORE, 2))

        engine(notes, queue, uploads, remote, sleeps).syncUser("u1")

        assertEquals(1, uploads.calls)
        assertEquals(1, remote.docs.size)
        assertEquals("https://storage/u1/n1.wav", (remote.docs[0]["audio"] as Map<*, *>)["remoteUrl"])
        assertEquals(SyncStatus.SYNCED, notes.store["n1"]!!.syncStatus)
        assertEquals("https://storage/u1/n1.wav", notes.store["n1"]!!.audioRemoteUrl)
        assertTrue(queue.items.isEmpty())
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun retriesFailedUploadWithExponentialBackoff() = runBlocking {
        val notes = FakeNotes()
        val queue = FakeQueue()
        val uploads = FakeUploads(failures = 2)
        val remote = FakeRemote()
        val sleeps = mutableListOf<Long>()
        val n = note()
        notes.upsert(n.toEntity())
        queue.insert(task(n, SyncTaskType.UPLOAD_AUDIO, 1))

        engine(notes, queue, uploads, remote, sleeps).syncUser("u1")

        assertEquals(3, uploads.calls)
        assertEquals(listOf(1_000L, 2_000L), sleeps)
        assertTrue(queue.items.isEmpty())
        assertEquals("https://storage/u1/n1.wav", notes.store["n1"]!!.audioRemoteUrl)
    }

    @Test
    fun givesUpAfterMaxAttemptsAndMarksNoteFailed() = runBlocking {
        val notes = FakeNotes()
        val queue = FakeQueue()
        val uploads = FakeUploads(failures = 100)
        val remote = FakeRemote()
        val sleeps = mutableListOf<Long>()
        val n = note()
        notes.upsert(n.toEntity())
        queue.insert(task(n, SyncTaskType.UPLOAD_AUDIO, 1))
        queue.insert(task(n, SyncTaskType.SYNC_FIRESTORE, 2))

        engine(notes, queue, uploads, remote, sleeps, maxAttempts = 3).syncUser("u1")

        assertEquals(3, uploads.calls)
        assertEquals(listOf(1_000L, 2_000L), sleeps)
        assertEquals(QueueStatus.FAILED, queue.items["q-UPLOAD_AUDIO"]!!.status)
        assertEquals(3, queue.items["q-UPLOAD_AUDIO"]!!.retryCount)
        assertEquals(SyncStatus.SYNC_FAILED, notes.store["n1"]!!.syncStatus)
        assertNull(notes.store["n1"]!!.audioRemoteUrl)
    }
}
