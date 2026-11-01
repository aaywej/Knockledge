package group.two.knockledge.audio

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AndroidAudioPlayerTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun nonExistentFilePlaybackSetsErrorMessage() {
        val player = AndroidAudioPlayer(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        val nonExistentFile = File("/tmp/non_existent_audio.wav")
        player.play(nonExistentFile)

        val state = player.state.value
        assertFalse(state.isPlaying)
        assertFalse(state.isLoading)
        val errorMsg = state.errorMessage
        assertNotNull(errorMsg)
        assertTrue(errorMsg.contains("File not found"))
    }

    @Test
    fun initialPlayerStateIsDefault() {
        val player = AndroidAudioPlayer(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        val state = player.state.value
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertFalse(state.isPlaying)
        assertFalse(state.isLoading)
        assertNull(state.activeChapter)
        assertNull(state.errorMessage)
    }

    @Test
    fun seekToClampsPositionWithinBounds() {
        val player = AndroidAudioPlayer(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        player.seekTo(5000L)
        assertEquals(0L, player.state.value.currentPositionMs)

        player.seekTo(-100L)
        assertEquals(0L, player.state.value.currentPositionMs)
    }

    @Test
    fun stopAndReleaseResetState() {
        val player = AndroidAudioPlayer(
            context = ApplicationProvider.getApplicationContext(),
            scope = testScope
        )

        player.seekTo(100L)
        player.stop()

        assertEquals(AudioPlaybackState(), player.state.value)

        player.release()
        assertEquals(AudioPlaybackState(), player.state.value)
    }
}
