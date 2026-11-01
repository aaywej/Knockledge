package group.two.knockledge.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.data.AuthCollisionException
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the authentication screen.
 */
data class AuthState(
    val user: UserProfile? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val collisionNotice: String? = null,
)

/**
 * ViewModel exposing [AuthState] and delegating sign-in operations to [AuthRepository].
 */
class AuthViewModel(
    private val repo: AuthRepository,
    private val googleToken: suspend () -> String,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState(user = repo.currentUser.value))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        repo.currentUser.onEach { u -> _state.update { it.copy(user = u) } }.launchIn(viewModelScope)
    }

    /** Signs in with a Google ID token produced by [googleToken]. */
    fun signInWithGoogle() = run {
        runCatching { googleToken() }.fold({ repo.signInWithGoogle(it) }) { Result.failure(it) }
    }

    /** Signs in with email and password. */
    fun signInWithEmail(email: String, password: String) = run { repo.signInWithEmail(email, password) }

    /** Registers a new account with display name, email, and password. */
    fun register(name: String, email: String, password: String) = run { repo.registerWithEmail(name, email, password) }

    /** Signs out the current user. */
    fun signOut() {
        viewModelScope.launch { repo.signOut() }
    }

    private fun run(block: suspend () -> Result<UserProfile>) {
        _state.update { it.copy(busy = true, error = null, collisionNotice = null) }
        viewModelScope.launch {
            val e = block().exceptionOrNull()
            _state.update {
                it.copy(
                    busy = false,
                    error = e?.takeIf { x -> x !is AuthCollisionException }?.let { x -> x.message ?: "Sign-in failed" },
                    collisionNotice = (e as? AuthCollisionException)?.message,
                )
            }
        }
    }
}
