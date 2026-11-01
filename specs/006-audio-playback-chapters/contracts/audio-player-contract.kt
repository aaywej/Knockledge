package group.two.knockledge.audio

import group.two.knockledge.data.model.ChapterMarker
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Observable UI state for audio playback.
 */
data class AudioPlaybackState(
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val activeChapter: ChapterMarker? = null,
    val errorMessage: String? = null,
)

/**
 * Hardware audio playback abstraction wrapping Android MediaPlayer.
 */
interface AudioPlayer {
    /**
     * Emits current playback state reactively.
     */
    val state: StateFlow<AudioPlaybackState>

    /**
     * Prepares and starts playback of a local audio file.
     *
     * @param file Local WAV audio file to play.
     * @param chapters Optional chapter markers to track during playback.
     */
    fun play(file: File, chapters: List<ChapterMarker> = emptyList())

    /**
     * Prepares and starts playback of a remote audio URL.
     *
     * @param url Remote HTTPS audio URL.
     * @param chapters Optional chapter markers to track during playback.
     */
    fun play(url: String, chapters: List<ChapterMarker> = emptyList())

    /**
     * Pauses active playback.
     */
    fun pause()

    /**
     * Resumes paused playback.
     */
    fun resume()

    /**
     * Seeks playback to a specific timestamp in milliseconds.
     *
     * @param positionMs Target position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Stops playback and resets media source.
     */
    fun stop()

    /**
     * Releases underlying platform media player resources.
     */
    fun release()
}
