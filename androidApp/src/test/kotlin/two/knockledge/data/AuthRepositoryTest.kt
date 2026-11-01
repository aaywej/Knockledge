package group.two.knockledge.data

import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.UserProfile
import group.two.knockledge.data.remote.AuthSource
import group.two.knockledge.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {
    private class FakeSource(var existing: UserProfile? = null) : AuthSource {
        var emailTaken = false
        var calls = 0
        override fun restoreSession() = existing
        override suspend fun signInWithGoogle(idToken: String): UserProfile {
            calls++
            return profile("g1", AuthProvider.GOOGLE)
        }
        override suspend fun signInWithEmail(email: String, password: String): UserProfile {
            calls++
            return profile("e1", AuthProvider.EMAIL_PASSWORD, email)
        }
        override suspend fun register(displayName: String, email: String, password: String): UserProfile {
            calls++
            if (emailTaken) throw AuthCollisionException(email, AuthProvider.GOOGLE)
            return profile("e2", AuthProvider.EMAIL_PASSWORD, email, displayName)
        }
        override fun signOut() { existing = null }
    }

    companion object {
        fun profile(uid: String, p: AuthProvider, email: String = "a@b.co", name: String = "A") =
            UserProfile(uid, email, name, null, p, 1L)
    }

    @Test fun restoresSessionOnStart() {
        val repo = AuthRepositoryImpl(FakeSource(profile("u", AuthProvider.GOOGLE)))
        assertEquals("u", repo.currentUser.value?.uid)
    }

    @Test fun googleTokenSignsIn() = runBlocking {
        val repo = AuthRepositoryImpl(FakeSource())
        assertEquals("g1", repo.signInWithGoogle("tok").getOrThrow().uid)
        assertEquals("g1", repo.currentUser.value?.uid)
    }

    @Test fun blankGoogleTokenRejected() = runBlocking {
        val src = FakeSource()
        assertTrue(AuthRepositoryImpl(src).signInWithGoogle(" ").isFailure)
        assertEquals(0, src.calls)
    }

    @Test fun badEmailRejected() = runBlocking {
        val src = FakeSource()
        assertTrue(AuthRepositoryImpl(src).signInWithEmail("nope", "secret1").isFailure)
        assertEquals(0, src.calls)
    }

    @Test fun shortPasswordRejectedOnRegister() = runBlocking {
        val src = FakeSource()
        assertTrue(AuthRepositoryImpl(src).registerWithEmail("Ann", "a@b.co", "12345").isFailure)
        assertEquals(0, src.calls)
    }

    @Test fun displayNameLengthEnforced() = runBlocking {
        val repo = AuthRepositoryImpl(FakeSource())
        assertTrue(repo.registerWithEmail(" ", "a@b.co", "secret1").isFailure)
        assertTrue(repo.registerWithEmail("x".repeat(51), "a@b.co", "secret1").isFailure)
        assertEquals("Ann", repo.registerWithEmail("Ann", "a@b.co", "secret1").getOrThrow().displayName)
    }

    @Test fun duplicateEmailSurfacesCollision() = runBlocking {
        val repo = AuthRepositoryImpl(FakeSource().apply { emailTaken = true })
        val err = repo.registerWithEmail("Ann", "a@b.co", "secret1").exceptionOrNull()
        assertIs<AuthCollisionException>(err)
        assertNull(repo.currentUser.value)
    }

    @Test fun signOutClearsState() = runBlocking {
        val repo = AuthRepositoryImpl(FakeSource(profile("u", AuthProvider.GOOGLE)))
        repo.signOut()
        assertNull(repo.currentUser.value)
    }
}

