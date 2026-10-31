package group.two.knockledge.ui.processing

import group.two.knockledge.pipeline.RewindStage
import kotlinx.coroutines.flow.StateFlow

/**
 * State representing on-demand AI processing progress for a note.
 */
data class NoteProcessingUiState(
    val noteId: String? = null,
    val isProcessing: Boolean = false,
    val stage: RewindStage? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

/**
 * Controller interface for triggering on-demand transcription and summarization from the UI.
 */
interface NoteProcessingController {
    /**
     * Current AI processing state for the active or selected note.
     */
    val processingState: StateFlow<NoteProcessingUiState>

    /**
     * Initiates transcription and summarization for the specified note.
     *
     * @param noteId Unique identifier of the note with captured audio.
     */
    fun processNote(noteId: String)

    /**
     * Resets or clears the current processing state.
     */
    fun resetState()
}
