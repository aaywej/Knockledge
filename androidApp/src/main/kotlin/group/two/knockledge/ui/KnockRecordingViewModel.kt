package group.two.knockledge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.KnockledgeController
import group.two.knockledge.data.TriggerType
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * [ViewModel] combining session state from [KnockledgeController] and [AppSettings] into [KnockRecordingUiState].
 */
class KnockRecordingViewModel(
    private val controller: KnockledgeController,
    private val settings: AppSettings,
) : ViewModel(), KnockRecordingController {
    private val lastSaveSuccess = MutableStateFlow<Boolean?>(null)

    /** Combined recording, buffer, and save-status state for the UI. */
    override val uiState: StateFlow<KnockRecordingUiState> =
        combine(controller.state, settings.bufferDurationSeconds, lastSaveSuccess) { rec, max, saved ->
            KnockRecordingUiState(
                isRecording = rec.isRunning,
                bufferedSeconds = (rec.bufferedMillis / 1000L).toInt(),
                maxBufferSeconds = max,
                isGestureActive = rec.isGestureActive,
                isSavingSnapshot = rec.pendingRewinds > 0,
                lastSaveSuccess = saved,
                level = rec.level,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, KnockRecordingUiState())

    /** Starts the recording session. */
    override fun startSession() = controller.start()

    /** Stops the recording session. */
    override fun stopSession() = controller.stop()

    /** Saves a snapshot of the current buffer and records the outcome. */
    override fun triggerManualSave() {
        if (!controller.state.value.canRewind) {
            lastSaveSuccess.value = false
            return
        }
        controller.rewind(TriggerType.MANUAL)
        lastSaveSuccess.value = true
    }

    /** Persists the requested buffer window length via [AppSettings]. */
    override fun setBufferDurationSeconds(seconds: Int) = settings.setBufferDurationSeconds(seconds)
}
