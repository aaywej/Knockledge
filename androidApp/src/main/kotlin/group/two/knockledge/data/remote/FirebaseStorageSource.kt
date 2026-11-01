package group.two.knockledge.data.remote

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Where recorded audio goes; the returned URL is stored on the note. */
interface AudioUploads {
    /** Uploads [file] as the audio for this note and returns its remote URL. */
    suspend fun uploadAudio(userId: String, noteId: String, file: File): String
}

/** Stores recordings in Firebase Storage at users/{userId}/audio/{noteId}.wav. */
class FirebaseStorageSource(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) : AudioUploads {
    /** Overwrites any file already stored for the note and returns its download URL. */
    override suspend fun uploadAudio(userId: String, noteId: String, file: File): String {
        val ref = storage.reference.child("users/$userId/audio/$noteId.wav")
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result) else c.resumeWithException(it.exception ?: IllegalStateException("Task failed"))
    }
}
