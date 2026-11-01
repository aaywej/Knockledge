package group.two.knockledge.ui.playback

import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.ChapterMarker
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level playback controls contract for Compose UI.
 */
interface NotePlaybackController {
    /**
     * Observable playback state for the currently loaded note.
     */
    val playbackState: StateFlow<AudioPlaybackState>

    /**
     * Toggles playback between play and pause.
     */
    fun togglePlayPause()

    /**
     * Seeks playback to the requested position.
     *
     * @param positionMs Target timestamp in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Jumps directly to the start timestamp of the requested chapter.
     *
     * @param chapter Selected chapter marker.
     */
    fun selectChapter(chapter: ChapterMarker)

    /**
     * Releases playback resources when the screen is closed or navigated away from.
     */
    fun onCleared()
}
