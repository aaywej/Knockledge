package group.two.knockledge.ui

import group.two.knockledge.data.model.Note
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level utilitarian test tabs for the application shell.
 */
enum class LiveTestTab {
    /** Continuous audio buffering and accelerometer knock gesture verification. */
    RECORD,

    /** Saved notes list, audio playback controls, chapters, and AI processing details. */
    NOTES,

    /** Runtime settings, automatic AI toggle, buffer window slider, and authentication. */
    SETTINGS,
}

/**
 * State of the unified live testing interface.
 */
data class LiveTestUiState(
    val activeTab: LiveTestTab = LiveTestTab.RECORD,
    val selectedNote: Note? = null,
    val lastKnockEpochMs: Long? = null,
    val knockCount: Int = 0,
    val hasRecordAudioPermission: Boolean = false,
)

/**
 * Contract governing the unified live testing UI shell.
 */
interface LiveTestController {
    /** Observable state for the top-level test screen. */
    val uiState: StateFlow<LiveTestUiState>

    /** Switches the active utilitarian test tab. */
    fun selectTab(tab: LiveTestTab)

    /** Selects a note from history to inspect audio playback and AI results. */
    fun selectNote(note: Note?)

    /** Records a knock detection event from hardware sensor or manual test trigger. */
    fun onKnockTriggered()

    /** Updates recording audio permission state. */
    fun onPermissionResult(granted: Boolean)
}
