package group.two.knockledge

import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.AudioGain
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.RewindStage
import group.two.knockledge.pipeline.TranscriptResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Immutable snapshot of the session, published to the UI via [KnockledgeController.state]. */
data class KnockledgeState(
    val isRunning: Boolean = false,
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
 * The whole session state machine, shared by Android and iOS.
 *
 * Every buffer access runs on [audioDispatcher] (single thread), so the ring
 * buffer needs no locks.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class KnockledgeController(
    private val audioSource: AudioSource,
    private val hooks: SessionHooks,
    private val pipeline: RewindPipeline,
    private val recents: RecentsRepository,
    private val buffer: LocalAudioRingBuffer,
    private val noteSink: NoteSink? = null,
    windowSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.WINDOW_SECONDS),
    private val minAudioSaveSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.MIN_REWIND_SECONDS),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioDispatcher = Dispatchers.Default.limitedParallelism(1)
    private var level = 0f
    private var captureJob: Job? = null
    private val pipelineLock = Mutex()
    private var chunkCount = 0L
    private var lastDb = -120.0
    private var silentMillis = 0L
    private var silenceReported = false

    private val _state = MutableStateFlow(KnockledgeState(minSaveSeconds = minAudioSaveSeconds))

    /** Current session state, observed by the UI. */
    val state: StateFlow<KnockledgeState> = _state.asStateFlow()

    private val _created = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    init {
        scope.launch(audioDispatcher) {
            windowSeconds.collect { seconds ->
                buffer.setWindowSeconds(seconds)
                _state.update { it.copy(windowMillis = seconds * 1000L) }
            }
        }
    }

    /** Emits the id of each new Recent, so the UI can open it. */
    val createdRecents: SharedFlow<Long> = _created.asSharedFlow()

    /** Starts capturing audio into the ring buffer. No-op when already running. */
    fun start() {
        if (captureJob?.isActive == true) return
        _state.value = KnockledgeState(
            isRunning = true,
            windowMillis = _state.value.windowMillis,
            minSaveSeconds = minAudioSaveSeconds,
        )
        silentMillis = 0L
        silenceReported = false
        hooks.onSessionStarted()
        captureJob = scope.launch(audioDispatcher) {
            try {
                audioSource.frames(RewindPolicy.SAMPLE_RATE)
                    .buffer(capacity = 64)
                    .collect { chunk ->
                        checkForBlockedMic(chunk)
                        buffer.write(chunk)
                        pushLevel(chunk)
                        chunk.fill(0)
                        val status = if (++chunkCount % 10L == 0L) audioSource.status() else null
                        _state.update {
                            val withStatus = if (status != null) {
                                it.copy(inputLabel = status.label, inputWarning = status.warning)
                            } else {
                                it
                            }
                            withStatus.copy(
                                bufferedMillis = buffer.bufferedMillis(),
                                level = level,
                                inputDb = lastDb.toInt(),
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                reportError(t.message ?: "Microphone stopped unexpectedly.")
                stop()
            }
        }
    }

    /** Stops capturing and clears the buffer. No-op when not running. */
    fun stop() {
        val job = captureJob ?: return
        captureJob = null
        _state.update {
            it.copy(
                isRunning = false, bufferedMillis = 0L, level = 0f,
                inputLabel = null, inputDb = null, inputWarning = null,
            )
        }
        scope.launch(audioDispatcher) {
            job.cancelAndJoin()
            buffer.clear()
            level = 0f
        }
        hooks.onSessionStopped()
    }

    /** Starts when stopped, stops when running. */
    fun toggle() = if (_state.value.isRunning) stop() else start()

    /**
     * Captures the last (up to) 5 minutes right now and summarizes it. Windows overlap:
     * nothing is cut, so two rewinds a minute apart share four minutes of audio.
     * Snapshots are taken instantly; summaries run one at a time so only one model is
     * ever loaded. Ignored until 3 minutes (or the whole window, if shorter) are cached.
     */
    fun rewind(trigger: TriggerType = TriggerType.GESTURE) {
        if (!_state.value.canRewind) return
        _state.update { it.copy(pendingRewinds = it.pendingRewinds + 1) }
        scope.launch {
            var wav: File? = null
            try {
                wav = withContext(audioDispatcher) { buffer.snapshot() }
                val pcm = readPcm(wav)
                val endedAt = nowMillis()
                val durationMillis = pcm.size * 1000L / RewindPolicy.SAMPLE_RATE
                var transcript: TranscriptResult? = null
                pipelineLock.withLock {
                    val summary = pipeline.run(
                        pcm,
                        RewindPolicy.SAMPLE_RATE,
                        durationMillis,
                        onStage = { stage -> _state.update { it.copy(stage = stage) } },
                        onTranscript = { transcript = it },
                    )
                    val recent = recents.add(summary, endedAt - durationMillis, endedAt)
                    _created.tryEmit(recent.id)
                    transcript?.let { t ->
                        runCatching { noteSink?.record(wav, endedAt - durationMillis, endedAt, trigger, t, summary) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                reportError(t.message ?: "Could not summarize that rewind.")
            } finally {
                wav?.delete()
                _state.update {
                    val left = (it.pendingRewinds - 1).coerceAtLeast(0)
                    it.copy(pendingRewinds = left, stage = if (left == 0) null else it.stage)
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
    fun reportError(message: String) = _state.update { it.copy(error = message) }

    /** Dismisses the current error. */
    fun clearError() = _state.update { it.copy(error = null) }

    /**
     * Real microphones always pick up some noise. Several seconds of exact zeros means the
     * OS is feeding silence: mic access switched off, or another app (a call, recorder or
     * voice assistant) holding the microphone.
     */
    private fun checkForBlockedMic(chunk: ShortArray) {
        if (!AudioGain.isDigitalSilence(chunk)) {
            silentMillis = 0L
            if (silenceReported) {
                // Sound is back: drop the warning so it can show again next time.
                silenceReported = false
                _state.update { if (it.error == SILENCE_MESSAGE) it.copy(error = null) else it }
            }
            return
        }
        silentMillis += chunk.size * 1000L / RewindPolicy.SAMPLE_RATE
        if (silentMillis >= 4_000L && !silenceReported) {
            silenceReported = true
            reportError(SILENCE_MESSAGE)
        }
    }

    /**
     * Maps chunk loudness to 0..1 on a decibel scale, so normal speech (around -30 dBFS)
     * fills about half the bar instead of barely moving a linear meter.
     * Bars rise instantly and fall back gently.
     */
    private fun pushLevel(chunk: ShortArray) {
        if (chunk.isEmpty()) return
        var sum = 0.0
        for (s in chunk) sum += s.toDouble() * s.toDouble()
        val rms = sqrt(sum / chunk.size) / Short.MAX_VALUE
        val db = 20.0 * log10(rms.coerceAtLeast(1e-6))
        lastDb = db
        val target = ((db - FLOOR_DB) / (CEIL_DB - FLOOR_DB)).toFloat().coerceIn(0f, 1f)
        // Rises instantly, falls back gently, like a meter with ballistics.
        level = if (target >= level) target else level * 0.6f + target * 0.4f
    }

    private companion object {
        const val SILENCE_MESSAGE =
            "The microphone is sending pure silence. During a phone or video call the system gives the mic " +
                "to the call app only; Knockledge hears again when the call ends. Otherwise check Microphone access " +
                "in Quick Settings / Control Center and close any recorder. Emulator: adb emu avd hostmicon."
        const val FLOOR_DB = -72.0 // near silence
        const val CEIL_DB = -24.0  // close, normal speech
    }
}
