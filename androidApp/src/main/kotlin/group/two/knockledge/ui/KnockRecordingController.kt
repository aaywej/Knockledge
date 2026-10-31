package group.two.knockledge.ui

import kotlinx.coroutines.flow.StateFlow

/**
 * UI state for knock-triggered circular recording.
 */
data class KnockRecordingUiState(
    val isRecording: Boolean = false,
    val bufferedSeconds: Int = 0,
    val maxBufferSeconds: Int = 300,
    val isGestureActive: Boolean = false,
    val isSavingSnapshot: Boolean = false,
    val lastSaveSuccess: Boolean? = null,
)

/**
 * Contract for controlling a circular audio recording session.
 */
interface KnockRecordingController {
    /** Current recording UI state. */
    val uiState: StateFlow<KnockRecordingUiState>

    /** Starts buffering audio and gesture detection. */
    fun startSession()

    /** Stops buffering audio and gesture detection. */
    fun stopSession()

    /** Saves the current buffer contents as a snapshot. */
    fun triggerManualSave()

    /** Updates the circular buffer window length. */
    fun setBufferDurationSeconds(seconds: Int)
}
