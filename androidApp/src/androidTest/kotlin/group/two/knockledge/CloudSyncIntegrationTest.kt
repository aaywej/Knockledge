package group.two.knockledge

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.local.KnockledgeDatabase
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.remote.FirebaseStorageSource
import group.two.knockledge.data.remote.FirestoreDataSource
import group.two.knockledge.data.repository.NoteRepositoryImpl
import group.two.knockledge.data.repository.SyncEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs ONLY against the Firebase Local Emulator Suite — never production.
 * Start: firebase emulators:start --only auth,firestore,storage --project demo-knockledge
 * Run:   gradlew :androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=group.two.knockledge.CloudSyncIntegrationTest
 */
@RunWith(AndroidJUnit4::class)
class CloudSyncIntegrationTest {

    private fun <T> com.google.android.gms.tasks.Task<T>.await(): T = Tasks.await(this)

    @Test
    fun noteUploadsToStorageAndSyncsToFirestore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val auth = FirebaseAuth.getInstance().apply { useEmulator("10.0.2.2", 9099) }
        val firestore = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }
        val storage = FirebaseStorage.getInstance().apply { useEmulator("10.0.2.2", 9199) }

        val email = "sync-${System.currentTimeMillis()}@knockledge.test"
        auth.createUserWithEmailAndPassword(email, "secret6").await()
        val uid = auth.currentUser!!.uid

        val db = Room.inMemoryDatabaseBuilder(context, KnockledgeDatabase::class.java).build()
        try {
            val repo = NoteRepositoryImpl(db.noteDao(), db.syncQueueDao(), File(context.cacheDir, "audio"))
            val wav = File(context.cacheDir, "snap.wav").apply { writeBytes(ByteArray(44) + ByteArray(32_000)) }
            val note = repo.createFromSnapshot(
                AudioSnapshotInput(
                    file = wav,
                    startEpochMs = 0,
                    endEpochMs = 60_000,
                    durationMs = 60_000,
                    triggerType = TriggerType.GESTURE,
                ),
                uid,
            )

            val engine = SyncEngine(
                db.noteDao(),
                db.syncQueueDao(),
                FirebaseStorageSource(storage),
                FirestoreDataSource(firestore),
            )
            engine.syncUser(uid)

            val meta = storage.reference.child("users/$uid/audio/${note.id}.wav").metadata.await()
            assertTrue(meta.sizeBytes > 0)

            val doc = firestore.collection("users").document(uid)
                .collection("notes").document(note.id).get().await()
            assertTrue(doc.exists())
            assertEquals(note.id, doc.getString("id"))
            assertEquals(uid, doc.getString("userId"))
            assertEquals("GESTURE", (doc.get("audio") as Map<*, *>)["triggerType"])
            assertEquals(60_000L, (doc.get("audio") as Map<*, *>)["durationMs"])
            assertEquals(16_000L, (doc.get("audio") as Map<*, *>)["sampleRate"])
            assertNotNull((doc.get("audio") as Map<*, *>)["remoteUrl"])

            assertEquals(SyncStatus.SYNCED, db.noteDao().getNote(note.id, uid)!!.syncStatus)
            assertTrue(db.syncQueueDao().pending(uid).isEmpty())

            val before = firestore.collection("users").document(uid).collection("notes").get().await().size()
            engine.syncUser(uid)
            val after = firestore.collection("users").document(uid).collection("notes").get().await().size()
            assertEquals(before, after)
        } finally {
            db.close()
            auth.signOut()
        }
    }
}
