package group.two.knockledge.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import group.two.knockledge.data.model.Note
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Writes note documents to the cloud. */
interface NoteSyncSource {
    /** Writes or merges [document] as the note's cloud document. */
    suspend fun upsertNote(userId: String, noteId: String, document: Map<String, Any?>)
}

/** Firestore notes at users/{userId}/notes/{noteId}. */
class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : NoteSyncSource {
    /** Merges the fields into the stored document, creating it when absent and keeping fields the map omits. */
    override suspend fun upsertNote(userId: String, noteId: String, document: Map<String, Any?>) {
        firestore.collection("users").document(userId)
            .collection("notes").document(noteId)
            .set(document, SetOptions.merge()).await()
    }
}

/** Field map for the cloud schema: enums go by name and a missing transcript or summary is left out, not nulled. */
fun Note.toFirestoreDocument(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "title" to title,
    "createdAtEpochMs" to createdAtEpochMs,
    "updatedAtEpochMs" to updatedAtEpochMs,
    "audio" to mapOf(
        "remoteUrl" to audio.remoteUrl,
        "durationMs" to audio.durationMs,
        "sampleRate" to audio.sampleRate,
        "triggerType" to audio.triggerType.name,
    ),
    "transcript" to transcript?.let {
        mapOf(
            "fullText" to it.fullText,
            "sentences" to it.sentences.map { s ->
                mapOf(
                    "speaker" to s.speaker,
                    "startTimeMs" to s.startTimeMs,
                    "endTimeMs" to s.endTimeMs,
                    "text" to s.text,
                    "confidence" to s.confidence,
                )
            },
            "isDiarized" to it.isDiarized,
            "detectedLanguage" to it.detectedLanguage,
            "engineType" to it.engineType.name,
        )
    },
    "summary" to summary?.let {
        mapOf(
            "executiveSummary" to it.executiveSummary,
            "keyPoints" to it.keyPoints,
            "chapterMarkers" to it.chapterMarkers.map { m ->
                mapOf(
                    "startTimeMs" to m.startTimeMs,
                    "title" to m.title,
                    "keyBullets" to m.keyBullets,
                )
            },
            "generationMode" to it.generationMode.name,
        )
    },
)

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result) else c.resumeWithException(it.exception ?: IllegalStateException("Task failed"))
    }
}
