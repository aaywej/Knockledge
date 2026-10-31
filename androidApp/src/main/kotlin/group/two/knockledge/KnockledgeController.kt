package group.two.knockledge

import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.PipelineResult
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.RewindStage
import group.two.knockledge.sensor.KnockGestureDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Immutable snapshot of the session, published to the UI via [KnockledgeController.state]. */
data class KnockledgeState(
    val isRunning: Boolean = false,
    /** True while the accelerometer knock detector is listening. */
    val isGestureActive: Boolean = false,
    /** Audio cached on disk since Start, capped at the configured window. Every Rewind uses all of it. */
    val bufferedMillis: Long = 0L,
    val windowMillis: Long = RewindPolicy.WINDOW_MILLIS,
    /** Rewinds captured but not yet summarized (they run one at a time, in order). */
    val pendingRewinds: Int = 0,
    /** Current input level, 0..1 on a decibel scale. Drives the VU meter. */
    val level: Float = 0f,
    /** Which part of a rewind is running (transcribing, then summarizing). */
    val stage: RewindStage? = null,
    /** Live input diagnostics: device, loudness, and any OS-level problem. */
    val inputLabel: String? = null,
    val inputDb: Int? = null,
    val inputWarning: String? = null,
    val error: String? = null,
    /** User-configured minimum seconds of audio Rewind needs; read live so changes apply at once. */
    val minSaveSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.MIN_REWIND_SECONDS),
) {
    /** True while at least one rewind is still being transcribed or summarized. */
    val isSummarizing: Boolean
        get() = pendingRewinds > 0

    /** Configured minimum save duration in milliseconds. */
    val minSaveMillis: Long
        get() = minSaveSeconds.value * 1000L

    /** Rewinds may overlap and may be tapped while others process. */
    val canRewind: Boolean
        get() = isRunning && bufferedMillis >= minOf(minSaveMillis, windowMillis)

    /** 0..1 progress toward unlocking Rewind. */
    val unlockProgress: Float
        get() = if (minSaveMillis <= 0L) 1f else (bufferedMillis.toFloat() / minSaveMillis).coerceIn(0f, 1f)

    /** Milliseconds of buffering left before Rewind unlocks; 0 once unlocked. */
    val millisUntilUnlock: Long
        get() = (minSaveMillis - bufferedMillis).coerceAtLeast(0L)
}

/**
 * Controller for the core Knockledge session.
 *
 * Coordinates audio ring capture through [RingAudioCapturer], knock gesture
 * detection through [KnockGestureDetector], and rewind AI processing through
 * [RewindPipeline].
 */
@OptIn(ExperimentalTime::class)
class KnockledgeController(
    private val capturer: RingAudioCapturer,
    private val knockDetector: KnockGestureDetector,
    private val hooks: SessionHooks,
    private val pipeline: RewindPipeline,
    private val recents: RecentsRepository,
    private val noteSink: NoteSink? = null,
    private val minAudioSaveSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.MIN_REWIND_SECONDS),
    private val autoAiProcessingEnabled: StateFlow<Boolean> = MutableStateFlow(true),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pipelineLock = Mutex()
    private val pendingRewinds = MutableStateFlow(0)
    private val stage = MutableStateFlow<RewindStage?>(null)
    private val coordinatorError = MutableStateFlow<String?>(null)
    private val isGestureListening = MutableStateFlow(false)

    private val _created = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    /** Emits the id of each new Recent, so the UI can open it. */
    val createdRecents: SharedFlow<Long> = _created.asSharedFlow()

    /** Current session state, observed by the UI and the foreground service. */
    val state: StateFlow<KnockledgeState> = combine(
        capturer.state,
        pendingRewinds,
        stage,
        coordinatorError,
        isGestureListening,
    ) { capState, pending, currentStage, coordErr, gestureActive ->
        KnockledgeState(
            isRunning = capState.isRunning,
            isGestureActive = gestureActive,
            bufferedMillis = capState.bufferedMillis,
            windowMillis = capState.windowMillis,
            pendingRewinds = pending,
            level = capState.level,
            stage = currentStage,
            inputLabel = capState.inputLabel,
            inputDb = capState.inputDb,
            inputWarning = capState.inputWarning,
            error = coordErr ?: capState.error,
            minSaveSeconds = minAudioSaveSeconds,
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        KnockledgeState(minSaveSeconds = minAudioSaveSeconds),
    )

    /** Starts audio capture and knock gesture detection. No-op when already running. */
    fun start() {
        if (state.value.isRunning) return
        capturer.start()
        knockDetector.startListening { rewind(TriggerType.GESTURE) }
        isGestureListening.value = true
        hooks.onSessionStarted()
    }

    /** Stops knock gesture detection and audio capture. */
    fun stop() {
        knockDetector.stopListening()
        isGestureListening.value = false
        capturer.stop()
        hooks.onSessionStopped()
    }

    /** Starts when stopped, stops when running. */
    fun toggle() = if (state.value.isRunning) stop() else start()

    /**
     * Captures the last (up to) 5 minutes right now and summarizes it. Windows overlap:
     * nothing is cut, so two rewinds a minute apart share four minutes of audio.
     * Snapshots are taken instantly; summaries run one at a time so only one model is
     * ever loaded. Ignored until 3 minutes (or the whole window, if shorter) are cached.
     */
    fun rewind(trigger: TriggerType = TriggerType.GESTURE) {
        if (!state.value.canRewind) return
        pendingRewinds.update { it + 1 }
        scope.launch {
            var wav: File? = null
            try {
                wav = capturer.snapshot()
                val pcm = readPcm(wav)
                val endedAt = nowMillis()
                val durationMillis = pcm.size * 1000L / RewindPolicy.SAMPLE_RATE

                if (!autoAiProcessingEnabled.value) {
                    runCatching { noteSink?.recordAudioOnly(wav, endedAt - durationMillis, endedAt, trigger) }
                    return@launch
                }
                pipelineLock.withLock {
                    val result = pipeline.run(
                        pcm,
                        RewindPolicy.SAMPLE_RATE,
                        durationMillis,
                        onStage = { s -> stage.value = s },
                    )
                    when (result) {
                        is PipelineResult.InsufficientSpeech -> reportError(result.message)
                        is PipelineResult.Success -> {
                            val recent = recents.add(result.summary, endedAt - durationMillis, endedAt)
                            _created.tryEmit(recent.id)
                            runCatching {
                                noteSink?.record(
                                    wav, endedAt - durationMillis, endedAt, trigger,
                                    result.transcript, result.summary,
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                reportError(t.message ?: "Could not summarize that rewind.")
            } finally {
                wav?.delete()
                pendingRewinds.update { (it - 1).coerceAtLeast(0) }
                if (pendingRewinds.value == 0) {
                    stage.value = null
                }
            }
        }
    }

    private fun readPcm(wav: File): ShortArray {
        val bytes = wav.readBytes()
        val offset = FileLocalAudioRingBuffer.WAV_HEADER_BYTES
        return ShortArray((bytes.size - offset) / 2).also {
            ByteBuffer.wrap(bytes, offset, it.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it)
        }
    }

    /** Deletes the Recent with [id]. */
    fun delete(id: Long) {
        scope.launch { recents.delete(id) }
    }

    /** Publishes [message] as the current error, shown until [clearError]. */
    fun reportError(message: String) {
        coordinatorError.value = message
    }

    /** Dismisses the current error. */
    fun clearError() {
        coordinatorError.value = null
        capturer.clearError()
    }
}
