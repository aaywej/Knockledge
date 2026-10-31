package group.two.knockledge.data.remote

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import group.two.knockledge.data.AuthCollisionException
import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.UserProfile
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Firebase Authentication backend; an email used by both providers is reported as a collision instead of being merged. */
class FirebaseAuthSource(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthSource {
    /** The signed-in user from the persisted Firebase session, or null when signed out. */
    override fun restoreSession(): UserProfile? = auth.currentUser?.toProfile()

    /** Signs out again before throwing when the email already has a password account, so no session is left behind. */
    override suspend fun signInWithGoogle(idToken: String): UserProfile {
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user!!
        if (user.providerData.any { it.providerId == "password" }) {
            auth.signOut()
            throw AuthCollisionException(user.email.orEmpty(), AuthProvider.EMAIL_PASSWORD)
        }
        return user.toProfile()
    }

    /** Signs in with an existing email and password account. */
    override suspend fun signInWithEmail(email: String, password: String): UserProfile =
        auth.signInWithEmailAndPassword(email, password).await().user!!.toProfile()

    /** Creates the account, then stores the display name; an email already in use becomes AuthCollisionException. */
    override suspend fun register(displayName: String, email: String, password: String): UserProfile {
        val user = try {
            auth.createUserWithEmailAndPassword(email, password).await().user!!
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AuthCollisionException(email, AuthProvider.GOOGLE)
        }
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
        return user.toProfile()
    }

    /** Ends the current session; local data is kept. */
    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toProfile() = UserProfile(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
        photoUrl = photoUrl?.toString(),
        provider = if (providerData.any { it.providerId == "google.com" }) AuthProvider.GOOGLE else AuthProvider.EMAIL_PASSWORD,
        createdAtEpochMs = metadata?.creationTimestamp ?: 1L,
    )
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result) else c.resumeWithException(it.exception ?: IllegalStateException("Task failed"))
    }
}
