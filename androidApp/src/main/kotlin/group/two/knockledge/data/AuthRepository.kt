package group.two.knockledge.data

import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/** Session state for the app: who is signed in and how to change it. */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: StateFlow<UserProfile?>

    /** Exchanges a Google ID token for a session; fails with AuthCollisionException when the email already uses a password. */
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>

    /** Signs in with email and password; malformed input and rejected credentials come back as Result.failure. */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    /** Creates an account and signs it in; the name must be 1-50 characters and the password at least 6. */
    suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile>

    /** Clears the session on this device and in the backend. */
    suspend fun signOut()
}

/** Raised when an email already belongs to an account using the other sign-in provider. */
class AuthCollisionException(
    val email: String,
    val existingProvider: AuthProvider,
    message: String = "An account with email $email already exists using $existingProvider. Please sign in with that provider.",
) : Exception(message)
