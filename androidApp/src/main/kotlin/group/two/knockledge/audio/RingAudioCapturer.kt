package group.two.knockledge.audio

import group.two.knockledge.core.AudioGain
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.core.RewindPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.log10
import kotlin.math.sqrt

/** Live state emitted by [RingAudioCapturer]. */
data class RingCaptureState(
    val isRunning: Boolean = false,
    /** Audio cached in the ring buffer, capped at the configured window. */
    val bufferedMillis: Long = 0L,
    val windowMillis: Long = RewindPolicy.WINDOW_MILLIS,
    /** Current input level, 0..1 on a decibel scale. Drives the VU meter. */
    val level: Float = 0f,
    /** Live input diagnostics: device, loudness, and any OS-level problem. */
    val inputLabel: String? = null,
    val inputDb: Int? = null,
    val inputWarning: String? = null,
    val error: String? = null,
)

/**
 * Captures continuous audio frames into a [LocalAudioRingBuffer].
 *
 * Every buffer access runs on [dispatcher] (single thread), so the ring
 * buffer needs no locks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RingAudioCapturer(
    private val audioSource: AudioSource,
    private val buffer: LocalAudioRingBuffer,
    windowSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.WINDOW_SECONDS),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var captureJob: Job? = null
    private var chunkCount = 0L
    private var level = 0f
    private var lastDb = -120.0
    private var silentMillis = 0L
    private var silenceReported = false

    private val _state = MutableStateFlow(RingCaptureState())

    /** Current capture state, observed by the session controller. */
    val state: StateFlow<RingCaptureState> = _state.asStateFlow()

    init {
        scope.launch {
            windowSeconds.collect { seconds ->
                buffer.setWindowSeconds(seconds)
                _state.update { it.copy(windowMillis = seconds * 1000L) }
            }
        }
    }

    /** Starts capturing audio into the ring buffer. No-op when already running. */
    fun start() {
        if (captureJob?.isActive == true) return
        _state.update { it.copy(isRunning = true) }
        silentMillis = 0L
        silenceReported = false
        captureJob = scope.launch {
            try {
                audioSource.frames(RewindPolicy.SAMPLE_RATE)
                    .buffer(capacity = 64)
                    .collect { chunk ->
                        checkForBlockedMic(chunk)
                        buffer.write(chunk)
                        pushLevel(chunk)
                        chunk.fill(0)
                        val status = if (++chunkCount % 10L == 0L) audioSource.status() else null
                        _state.update { current ->
                            val withStatus = if (status != null) {
                                current.copy(inputLabel = status.label, inputWarning = status.warning)
                            } else {
                                current
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
                isRunning = false,
                bufferedMillis = 0L,
                level = 0f,
                inputLabel = null,
                inputDb = null,
                inputWarning = null,
            )
        }
        scope.launch {
            job.cancelAndJoin()
            buffer.clear()
            level = 0f
        }
    }

    /** Returns a WAV file of the buffered audio, taken on the audio dispatcher. */
    suspend fun snapshot(): File = withContext(dispatcher) {
        buffer.snapshot()
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

    companion object {
        /** Warning shown when the OS feeds digital silence instead of real mic audio. */
        const val SILENCE_MESSAGE =
            "The microphone is sending pure silence. During a phone or video call the system gives the mic " +
                "to the call app only; Knockledge hears again when the call ends. Otherwise check Microphone access " +
                "in Quick Settings / Control Center and close any recorder. Emulator: adb emu avd hostmicon."
        const val FLOOR_DB = -72.0 // near silence
        const val CEIL_DB = -24.0  // close, normal speech
    }
}
