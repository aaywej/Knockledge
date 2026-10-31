package group.two.knockledge.data.remote

import group.two.knockledge.data.model.UserProfile

/** Backend sign-in calls; each returns the signed-in profile and throws on failure. */
interface AuthSource {
    /** Profile of the session already signed in on this device, or null. */
    fun restoreSession(): UserProfile?

    /** Signs in with a Google ID token; throws AuthCollisionException when the email belongs to a password account. */
    suspend fun signInWithGoogle(idToken: String): UserProfile

    /** Signs in with email and password; rejected credentials throw. */
    suspend fun signInWithEmail(email: String, password: String): UserProfile

    /** Creates an account and returns it signed in. */
    suspend fun register(displayName: String, email: String, password: String): UserProfile

    /** Ends the session in the backend, so the next start comes back signed out. */
    fun signOut()
}
