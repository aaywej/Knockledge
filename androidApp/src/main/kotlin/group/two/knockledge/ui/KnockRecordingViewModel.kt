package group.two.knockledge.ui

import group.two.knockledge.KnockledgeController
import group.two.knockledge.KnockledgeState
import group.two.knockledge.data.TriggerType
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Backend abstraction for starting, stopping, and saving circular recording.
 */
interface RecordingBackend {
    /** Current Knockledge state. */
    val state: StateFlow<KnockledgeState>

    /** Starts recording into the circular buffer. */
    fun start()

    /** Stops recording. */
    fun stop()

    /** Persists the buffered audio; returns false when nothing can be rewound. */
    fun save(): Boolean
}

/**
 * Adapts a [KnockledgeController] to the [RecordingBackend] interface.
 */
fun KnockledgeController.asBackend(): RecordingBackend = object : RecordingBackend {
    override val state = this@asBackend.state
    override fun start() = this@asBackend.start()
    override fun stop() = this@asBackend.stop()
    override fun save(): Boolean {
        if (!state.value.canRewind) return false
        rewind(TriggerType.MANUAL)
        return true
    }
}

/**
 * [KnockRecordingController] implementation combining backend state and settings into [KnockRecordingUiState].
 */
class KnockRecordingViewModel(
    private val backend: RecordingBackend,
    private val settings: AppSettings,
    scope: CoroutineScope,
) : KnockRecordingController {
    private val lastSaveSuccess = MutableStateFlow<Boolean?>(null)

    /** Combined recording, buffer, and save-status state for the UI. */
    override val uiState: StateFlow<KnockRecordingUiState> =
        combine(backend.state, settings.bufferDurationSeconds, lastSaveSuccess) { rec, max, saved ->
            KnockRecordingUiState(
                isRecording = rec.isRunning,
                bufferedSeconds = (rec.bufferedMillis / 1000L).toInt(),
                maxBufferSeconds = max,
                isGestureActive = rec.isRunning,
                isSavingSnapshot = rec.pendingRewinds > 0,
                lastSaveSuccess = saved,
            )
        }.stateIn(scope, SharingStarted.Eagerly, KnockRecordingUiState())

    /** Starts the recording session. */
    override fun startSession() = backend.start()

    /** Stops the recording session. */
    override fun stopSession() = backend.stop()

    /** Saves a snapshot of the current buffer and records the outcome. */
    override fun triggerManualSave() {
        lastSaveSuccess.value = backend.save()
    }

    /** Persists the requested buffer window length via [AppSettings]. */
    override fun setBufferDurationSeconds(seconds: Int) = settings.setBufferDurationSeconds(seconds)
}
