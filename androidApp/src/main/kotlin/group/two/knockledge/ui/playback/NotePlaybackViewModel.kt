package group.two.knockledge.ui.playback

import androidx.lifecycle.ViewModel
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.audio.AudioPlayer
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

/**
 * ViewModel managing audio playback state and controls for a note.
 */
class NotePlaybackViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer
) : ViewModel(), NotePlaybackController {

    override val playbackState: StateFlow<AudioPlaybackState> = audioPlayer.state

    private var currentNote: Note? = null

    /**
     * Initializes playback for the specified [note].
     */
    fun loadNote(note: Note) {
        currentNote = note
        val chapters = note.summary?.chapterMarkers ?: emptyList()
        val localPath = note.audio.localPath
        val file = File(localPath)

        if (file.exists()) {
            audioPlayer.play(file, chapters)
        } else {
            note.audio.remoteUrl?.let { url ->
                audioPlayer.play(url, chapters)
            } ?: run {
                audioPlayer.play(file, chapters)
            }
        }
    }

    override fun togglePlayPause() {
        val state = playbackState.value
        if (state.isPlaying) {
            audioPlayer.pause()
        } else {
            if (state.currentPositionMs >= state.durationMs && state.durationMs > 0) {
                audioPlayer.seekTo(0)
            }
            audioPlayer.resume()
        }
    }

    override fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    override fun selectChapter(chapter: ChapterMarker) {
        audioPlayer.seekTo(chapter.startTimeMs)
    }

    public override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
