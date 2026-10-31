package group.two.knockledge.data.repository

import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.model.UserProfile
import group.two.knockledge.data.remote.AuthSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AuthRepository] over [AuthSource]: the session restored at construction is published
 * immediately, so the app can start signed in without a network round trip.
 */
class AuthRepositoryImpl(private val source: AuthSource) : AuthRepository {
    private val user = MutableStateFlow(source.restoreSession())

    /** Starts with the restored session and follows every successful sign-in; null when signed out. */
    override val currentUser: StateFlow<UserProfile?> = user.asStateFlow()

    /** Returns a failure carrying [IllegalArgumentException] for a blank token. */
    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> = attempt {
        require(idToken.isNotBlank()) { "Missing Google ID token" }
        source.signInWithGoogle(idToken)
    }

    /** Trims the email and rejects malformed addresses and empty passwords before calling the source. */
    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = attempt {
        requireEmail(email)
        require(password.isNotEmpty()) { "Password is required" }
        source.signInWithEmail(email.trim(), password)
    }

    /** Requires a trimmed name of 1 to 50 characters, a valid email and a password of at least 6 characters. */
    override suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile> =
        attempt {
            require(displayName.trim().length in 1..50) { "Name must be 1 to 50 characters" }
            requireEmail(email)
            require(password.length >= 6) { "Password must be at least 6 characters" }
            source.register(displayName.trim(), email.trim(), password)
        }

    /** Signs out of the source first, then clears [currentUser]. */
    override suspend fun signOut() {
        source.signOut()
        user.value = null
    }

    private fun requireEmail(email: String) =
        require(EMAIL.matches(email.trim())) { "Enter a valid email address" }

    /** Runs [block], publishing the user on success and turning any failure into [Result.failure]. */
    private suspend fun attempt(block: suspend () -> UserProfile): Result<UserProfile> =
        try {
            Result.success(block().also { user.value = it })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
