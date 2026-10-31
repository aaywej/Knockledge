package group.two.knockledge.ui

import group.two.knockledge.data.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

/**
 * State coordinator implementing [LiveTestController] for top-level test UI state.
 */
class LiveTestCoordinator(
    initialTab: LiveTestTab = LiveTestTab.RECORD,
    initialPermission: Boolean = false,
    private val clock: () -> Long = System::currentTimeMillis,
) : LiveTestController {

    private val _uiState = MutableStateFlow(
        LiveTestUiState(
            activeTab = initialTab,
            hasRecordAudioPermission = initialPermission,
        )
    )
    override val uiState: StateFlow<LiveTestUiState> = _uiState.asStateFlow()

    override fun selectTab(tab: LiveTestTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    override fun selectNote(note: Note?) {
        _uiState.update { it.copy(selectedNote = note) }
    }

    override fun onKnockTriggered() {
        val now = clock()
        _uiState.update {
            it.copy(
                lastKnockEpochMs = now,
                knockCount = it.knockCount + 1,
            )
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasRecordAudioPermission = granted) }
    }
}
