# Decouple Ring Capturing & 3-Tier Session Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple continuous audio ring buffer capturing into `RingAudioCapturer`, repurpose `KnockledgeController` as the unified session controller (coordinating audio, gestures, and rewinds), and convert `KnockRecordingViewModel` into a standard Android Jetpack `ViewModel`.

**Architecture:** Introduce `RingAudioCapturer` in `group.two.knockledge.audio` for thread-confined circular buffer ingestion and diagnostics. Repurpose `KnockledgeController` as a first-class domain session controller connecting `RingAudioCapturer`, `KnockGestureDetector`, and `RewindPipeline`. Delete the redundant `RecordingBackend` adapter, and refactor `KnockRecordingViewModel` to inherit `androidx.lifecycle.ViewModel` using `viewModelScope`.

**Tech Stack:** Android SDK, Kotlin Coroutines, StateFlow, Android Jetpack ViewModel, Dagger 2, JUnit 4 / Kotlin Test.

**Spec:** [docs/superpowers/specs/2026-10-31-decouple-ring-capturing-design.md](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/docs/superpowers/specs/2026-10-31-decouple-ring-capturing-design.md)

## Global Constraints

- Confine all ring buffer writes and snapshots to a single-threaded coroutine dispatcher.
- Use ASD-STE100 plain English in all code documentation and error strings.
- Do not add unapproved third-party dependencies.
- Use `viewModelScope` inside `KnockRecordingViewModel` without external CoroutineScope constructor parameters.
- Verify all changes with `./gradlew testDebugUnitTest`.

---

### Task 1: Create RingAudioCapturer and Unit Tests

**Files:**
- Create: `androidApp/src/main/kotlin/group/two/knockledge/audio/RingAudioCapturer.kt`
- Test: `androidApp/src/test/kotlin/two/knockledge/audio/RingAudioCapturerTest.kt`

**Interfaces:**
- Consumes: `AudioSource`, `LocalAudioRingBuffer`, `RewindPolicy`
- Produces: `RingAudioCapturer`, `RingCaptureState`

- [ ] **Step 1: Write the failing unit test**

Create `androidApp/src/test/kotlin/two/knockledge/audio/RingAudioCapturerTest.kt`:

```kotlin
package two.knockledge.audio

import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingAudioCapturerTest {

    private class FakeBuffer(var millis: Long = 0L) : LocalAudioRingBuffer {
        var writtenChunks = 0
        var cleared = false
        var windowSecondsSet = 0
        override fun write(chunk: ShortArray, length: Int) {
            writtenChunks++
            millis += 100L
        }
        override fun setWindowSeconds(seconds: Int) {
            windowSecondsSet = seconds
        }
        override fun bufferedMillis(): Long = millis
        override fun snapshot(): File = File.createTempFile("test_snap", ".wav")
        override fun clear() {
            cleared = true
            millis = 0L
        }
    }

    private class ChunkSource(private val count: Int = 3) : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = flow {
            repeat(count) {
                emit(ShortArray(1600) { 1000 })
            }
        }
    }

    @Test
    fun startCapturesChunksAndUpdatesState() = runBlocking {
        val buffer = FakeBuffer()
        val capturer = RingAudioCapturer(
            audioSource = ChunkSource(count = 3),
            buffer = buffer,
        )

        assertFalse(capturer.state.value.isRunning)
        capturer.start()
        assertTrue(capturer.state.value.isRunning)

        withTimeout(3000L) {
            while (buffer.writtenChunks < 3) delay(10L)
        }

        assertTrue(capturer.state.value.bufferedMillis > 0L)
        capturer.stop()
        assertFalse(capturer.state.value.isRunning)
        assertTrue(buffer.cleared)
    }

    @Test
    fun dynamicWindowUpdatesPropagateToBuffer() = runBlocking {
        val buffer = FakeBuffer()
        val windowSeconds = MutableStateFlow(60)
        val capturer = RingAudioCapturer(
            audioSource = ChunkSource(count = 0),
            buffer = buffer,
            windowSeconds = windowSeconds,
        )

        delay(50L)
        assertEquals(60, buffer.windowSecondsSet)

        windowSeconds.value = 180
        delay(50L)
        assertEquals(180, buffer.windowSecondsSet)
        assertEquals(180_000L, capturer.state.value.windowMillis)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.audio.RingAudioCapturerTest"
```
Expected: FAIL with compilation error (unresolved reference: `RingAudioCapturer`).

- [ ] **Step 3: Write RingAudioCapturer implementation**

Create `androidApp/src/main/kotlin/group/two/knockledge/audio/RingAudioCapturer.kt`:

```kotlin
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
    val bufferedMillis: Long = 0L,
    val windowMillis: Long = RewindPolicy.WINDOW_MILLIS,
    val level: Float = 0f,
    val inputLabel: String? = null,
    val inputDb: Int? = null,
    val inputWarning: String? = null,
    val error: String? = null,
)

/**
 * Captures continuous audio frames into a [LocalAudioRingBuffer].
 *
 * Runs all buffer writes and snapshots on a dedicated single-threaded dispatcher.
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
    val state: StateFlow<RingCaptureState> = _state.asStateFlow()

    init {
        scope.launch {
            windowSeconds.collect { seconds ->
                buffer.setWindowSeconds(seconds)
                _state.update { it.copy(windowMillis = seconds * 1000L) }
            }
        }
    }

    /** Starts continuous capture into the ring buffer. */
    fun start() {
        if (captureJob?.isActive == true) return
        _state.update {
            it.copy(
                isRunning = true,
                windowMillis = _state.value.windowMillis,
            )
        }
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

    /** Stops capturing and clears the ring buffer. */
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

    /** Takes a snapshot WAV file from the buffer on the audio dispatcher. */
    suspend fun snapshot(): File = withContext(dispatcher) {
        buffer.snapshot()
    }

    /** Sets the live error message. */
    fun reportError(message: String) {
        _state.update { it.copy(error = message) }
    }

    /** Clears the current error message. */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun checkForBlockedMic(chunk: ShortArray) {
        if (!AudioGain.isDigitalSilence(chunk)) {
            silentMillis = 0L
            if (silenceReported) {
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

    private fun pushLevel(chunk: ShortArray) {
        if (chunk.isEmpty()) return
        var sum = 0.0
        for (s in chunk) sum += s.toDouble() * s.toDouble()
        val rms = sqrt(sum / chunk.size) / Short.MAX_VALUE
        val db = 20.0 * log10(rms.coerceAtLeast(1e-6))
        lastDb = db
        val target = ((db - FLOOR_DB) / (CEIL_DB - FLOOR_DB)).toFloat().coerceIn(0f, 1f)
        level = if (target >= level) target else level * 0.6f + target * 0.4f
    }

    companion object {
        const val SILENCE_MESSAGE =
            "The microphone is sending pure silence. During a phone or video call the system gives the mic " +
                "to the call app only; Knockledge hears again when the call ends. Otherwise check Microphone access " +
                "in Quick Settings / Control Center and close any recorder. Emulator: adb emu avd hostmicon."
        const val FLOOR_DB = -72.0
        const val CEIL_DB = -24.0
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.audio.RingAudioCapturerTest"
```
Expected: PASS with 2 tests completed.

- [ ] **Step 5: Verify working tree changes**

Run:
```powershell
git status --short
```
Expected: `RingAudioCapturer.kt` and `RingAudioCapturerTest.kt` listed in untracked files.

---

### Task 2: Repurpose KnockledgeController as Session Controller

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`
- Create: `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerSessionTest.kt`

**Interfaces:**
- Consumes: `RingAudioCapturer`, `KnockGestureDetector`, `RewindPipeline`, `RecentsRepository`, `NoteSink`, `SessionHooks`
- Produces: `KnockledgeController`, `KnockledgeState`

- [ ] **Step 1: Write unit tests for session-coordinating KnockledgeController**

Create `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerSessionTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngineType
import group.two.knockledge.sensor.KnockGestureDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnockledgeControllerSessionTest {

    private class FakeSource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = flow { emit(ShortArray(16)) }
    }

    private class FakeBuffer(private val wav: File) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) = Unit
        override fun setWindowSeconds(seconds: Int) = Unit
        override fun bufferedMillis(): Long = 3_000L
        override fun snapshot(): File = wav
        override fun clear() = Unit
    }

    private class FakeDetector : KnockGestureDetector {
        var isListening = false
        private var onKnockCallback: (() -> Unit)? = null
        override fun startListening(onKnock: () -> Unit) {
            isListening = true
            onKnockCallback = onKnock
        }
        override fun stopListening() {
            isListening = false
            onKnockCallback = null
        }
        override fun setPeakThreshold(threshold: Float) = Unit
        fun triggerKnock() = onKnockCallback?.invoke()
    }

    private fun wav(): File {
        val f = File.createTempFile("snap", ".wav")
        f.writeBytes(ByteArray(44 + 32_000))
        return f
    }

    @Test
    fun startAndStopControlBothAudioAndKnockDetector() = runBlocking {
        val capturer = RingAudioCapturer(FakeSource(), FakeBuffer(wav()))
        val detector = FakeDetector()
        val pipeline = RewindPipeline(PlaceholderTranscriber(), RegexSensitiveFilter(), PlaceholderSummarizer())
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = detector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            hooks = NoopSessionHooks,
        )

        assertFalse(controller.state.value.isRunning)
        assertFalse(detector.isListening)

        controller.start()
        assertTrue(controller.state.value.isRunning)
        assertTrue(detector.isListening)

        controller.stop()
        assertFalse(controller.state.value.isRunning)
        assertFalse(detector.isListening)
    }

    @Test
    fun knockGestureTriggersRewindAndPersistsNote() = runBlocking {
        val latch = CountDownLatch(1)
        var recordedTrigger: TriggerType? = null
        val sink = NoteSink { _, _, _, trigger, _, _ ->
            recordedTrigger = trigger
            latch.countDown()
        }
        val pipeline = RewindPipeline(
            object : Transcriber {
                override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult =
                    TranscriptResult(emptyList(), "test transcript", false, "en", TranscriptionEngineType.LOCAL_WHISPER)
            },
            RegexSensitiveFilter(),
            object : group.two.knockledge.pipeline.Summarizer {
                override suspend fun summarize(request: SummaryRequest): Summary = Summary("title", "gist", emptyList(), 0)
            },
        )
        val capturer = RingAudioCapturer(FakeSource(), FakeBuffer(wav()))
        val detector = FakeDetector()
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = detector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            noteSink = sink,
            hooks = NoopSessionHooks,
            minAudioSaveSeconds = MutableStateFlow(2),
        )

        controller.start()
        withTimeout(5000L) {
            while (!controller.state.value.canRewind) delay(10L)
        }

        detector.triggerKnock()
        assertTrue(latch.await(5, TimeUnit.SECONDS), "NoteSink was not notified on gesture")
        assertEquals(TriggerType.GESTURE, recordedTrigger)
        controller.stop()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.KnockledgeControllerSessionTest"
```
Expected: FAIL with compilation error (constructor arguments mismatch).

- [ ] **Step 3: Update KnockledgeController.kt**

Edit `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`:

```kotlin
package group.two.knockledge

import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.RewindStage
import group.two.knockledge.pipeline.TranscriptResult
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

/** Immutable snapshot of the session, published to UI and notification service. */
data class KnockledgeState(
    val isRunning: Boolean = false,
    val isGestureActive: Boolean = false,
    val bufferedMillis: Long = 0L,
    val windowMillis: Long = RewindPolicy.WINDOW_MILLIS,
    val pendingRewinds: Int = 0,
    val level: Float = 0f,
    val stage: RewindStage? = null,
    val inputLabel: String? = null,
    val inputDb: Int? = null,
    val inputWarning: String? = null,
    val error: String? = null,
    val minSaveSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.MIN_REWIND_SECONDS),
) {
    val isSummarizing: Boolean
        get() = pendingRewinds > 0

    val minSaveMillis: Long
        get() = minSaveSeconds.value * 1000L

    val canRewind: Boolean
        get() = isRunning && bufferedMillis >= minOf(minSaveMillis, windowMillis)

    val unlockProgress: Float
        get() = if (minSaveMillis <= 0L) 1f else (bufferedMillis.toFloat() / minSaveMillis).coerceIn(0f, 1f)

    val millisUntilUnlock: Long
        get() = (minSaveMillis - bufferedMillis).coerceAtLeast(0L)
}

/**
 * Controller for the core Knockledge session.
 *
 * Coordinates audio ring capture, accelerometer knock detection, and rewind AI processing.
 */
@OptIn(ExperimentalTime::class)
class KnockledgeController(
    private val capturer: RingAudioCapturer,
    private val knockDetector: KnockGestureDetector,
    private val pipeline: RewindPipeline,
    private val recents: RecentsRepository,
    private val noteSink: NoteSink? = null,
    private val hooks: SessionHooks = NoopSessionHooks,
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
    val createdRecents: SharedFlow<Long> = _created.asSharedFlow()

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

    /** Starts audio capture and gesture detection. */
    fun start() {
        if (state.value.isRunning) return
        capturer.start()
        knockDetector.startListening { rewind(TriggerType.GESTURE) }
        isGestureListening.value = true
        hooks.onSessionStarted()
    }

    /** Stops audio capture and gesture detection. */
    fun stop() {
        knockDetector.stopListening()
        isGestureListening.value = false
        capturer.stop()
        hooks.onSessionStopped()
    }

    /** Toggles the session on or off. */
    fun toggle() = if (state.value.isRunning) stop() else start()

    /**
     * Captures a snapshot from the circular buffer and executes AI summarization.
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
                var transcript: TranscriptResult? = null
                pipelineLock.withLock {
                    val summary = pipeline.run(
                        pcm,
                        RewindPolicy.SAMPLE_RATE,
                        durationMillis,
                        onStage = { s -> stage.value = s },
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

    /** Publishes an error string. */
    fun reportError(message: String) {
        coordinatorError.value = message
    }

    /** Clears the current error. */
    fun clearError() {
        coordinatorError.value = null
        capturer.clearError()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.KnockledgeControllerSessionTest"
```
Expected: PASS with 2 tests passing.

---

### Task 3: Refactor KnockRecordingViewModel to Jetpack ViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/ui/KnockRecordingViewModel.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockRecordingViewModelTest.kt`

**Interfaces:**
- Consumes: `KnockledgeController`, `AppSettings`, `androidx.lifecycle.ViewModel`
- Produces: `KnockRecordingViewModel`

- [ ] **Step 1: Write updated unit tests for KnockRecordingViewModel**

Edit `androidApp/src/test/kotlin/two/knockledge/KnockRecordingViewModelTest.kt`:

```kotlin
package group.two.knockledge

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.KnockRecordingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KnockRecordingViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private class TestBuffer : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) = Unit
        override fun setWindowSeconds(seconds: Int) = Unit
        override fun bufferedMillis(): Long = 0L
        override fun snapshot(): File = File.createTempFile("snap", ".wav")
        override fun clear() = Unit
    }

    private class TestDetector : KnockGestureDetector {
        var isListening = false
        override fun startListening(onKnock: () -> Unit) { isListening = true }
        override fun stopListening() { isListening = false }
        override fun setPeakThreshold(threshold: Float) = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createController(source: AudioSource = AudioSource { emptyFlow() }): KnockledgeController {
        val capturer = RingAudioCapturer(source, TestBuffer())
        val pipeline = RewindPipeline(PlaceholderTranscriber(), RegexSensitiveFilter(), PlaceholderSummarizer())
        return KnockledgeController(
            capturer = capturer,
            knockDetector = TestDetector(),
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            hooks = NoopSessionHooks,
        )
    }

    @Test
    fun startAndStopReflectInUiState() {
        val controller = createController()
        val settings = AppSettings(MemoryStore(), "m")
        val vm = KnockRecordingViewModel(controller, settings)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isRecording)
        vm.startSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isRecording)

        vm.stopSession()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isRecording)
    }

    @Test
    fun bufferDurationClampingPropagatesToUiState() {
        val controller = createController()
        val settings = AppSettings(MemoryStore(), "m")
        val vm = KnockRecordingViewModel(controller, settings)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(300, vm.uiState.value.maxBufferSeconds)
        vm.setBufferDurationSeconds(120)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(120, vm.uiState.value.maxBufferSeconds)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
./gradlew testDebugUnitTest --tests "group.two.knockledge.KnockRecordingViewModelTest"
```
Expected: FAIL with compilation error (constructor signature mismatch).

- [ ] **Step 3: Update KnockRecordingViewModel.kt**

Edit `androidApp/src/main/kotlin/group/two/knockledge/ui/KnockRecordingViewModel.kt`:
Delete `RecordingBackend` and make `KnockRecordingViewModel` extend `androidx.lifecycle.ViewModel`:

```kotlin
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
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, KnockRecordingUiState())

    /** Starts the recording session. */
    override fun startSession() = controller.start()

    /** Stops the recording session. */
    override fun stopSession() = controller.stop()

    /** Saves a snapshot of the current buffer. */
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
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```powershell
./gradlew testDebugUnitTest --tests "group.two.knockledge.KnockRecordingViewModelTest"
```
Expected: PASS.

---

### Task 4: Update KnockledgeService, KnockledgeComponent, and KnockledgeGraph

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeGraph.kt`

**Interfaces:**
- Consumes: `RingAudioCapturer`, `KnockledgeController`
- Produces: Updated DI and simplified service

- [ ] **Step 1: Simplify KnockledgeService.kt**

Edit `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt`:
Remove manual knock gesture starting from `observe()` and `onDestroy()` since `controller.start()` and `controller.stop()` now control gestures:

```kotlin
    private fun observe() {
        if (observeJob != null) return
        scope.launch {
            settings.knockSensorThreshold.collect { knock.setPeakThreshold(it) }
        }
        observeJob = scope.launch {
            controller.state
                .map {
                    NotificationModel(
                        running = it.isRunning,
                        bufferedSeconds = it.bufferedMillis / 1000L,
                        canRewind = it.canRewind,
                        summarizing = it.isSummarizing,
                        minSaveMillis = it.minSaveMillis,
                        millisUntilUnlock = it.millisUntilUnlock,
                    )
                }
                .distinctUntilChanged()
                .collect { model ->
                    if (!model.running) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        notifications.notify(NOTIFICATION_ID, build(model))
                    }
                }
        }
    }

    /** Drops notification observation and resets foreground state. */
    override fun onDestroy() {
        DeviceAudioConsent.foregroundReady = false
        scope.cancel()
        super.onDestroy()
    }
```

- [ ] **Step 2: Update KnockledgeComponent.kt**

Edit `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`:
Add `@Provides @Singleton fun ringAudioCapturer(...)` and update `controller(...)`:

```kotlin
    /** Audio capturer that manages continuous ring buffer ingestion. */
    @Provides
    @Singleton
    fun ringAudioCapturer(
        audioSource: AudioSource,
        buffer: LocalAudioRingBuffer,
        settings: AppSettings,
    ): RingAudioCapturer =
        RingAudioCapturer(
            audioSource = audioSource,
            buffer = buffer,
            windowSeconds = settings.bufferDurationSeconds,
        )

    /** The session controller coordinating audio capture, knock gestures, and rewinds. */
    @Provides
    @Singleton
    fun controller(
        capturer: RingAudioCapturer,
        knock: KnockGestureDetector,
        hooks: SessionHooks,
        pipeline: RewindPipeline,
        recents: RecentsRepository,
        sink: NoteSink,
        settings: AppSettings,
    ): KnockledgeController =
        KnockledgeController(
            capturer = capturer,
            knockDetector = knock,
            pipeline = pipeline,
            recents = recents,
            noteSink = sink,
            hooks = hooks,
            minAudioSaveSeconds = settings.minAudioSaveSeconds,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )
```

- [ ] **Step 3: Update KnockledgeGraph.kt**

Edit `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeGraph.kt`:

```kotlin
    constructor(
        audioSource: AudioSource,
        hooks: SessionHooks,
        transcriber: Transcriber = PlaceholderTranscriber(),
        filter: SensitiveFilter = RegexSensitiveFilter(),
        summarizer: Summarizer = PlaceholderSummarizer(),
        recents: RecentsRepository = InMemoryRecentsRepository(),
        buffer: LocalAudioRingBuffer,
        knockDetector: group.two.knockledge.sensor.KnockGestureDetector = object : group.two.knockledge.sensor.KnockGestureDetector {
            override fun startListening(onKnock: () -> Unit) = Unit
            override fun stopListening() = Unit
            override fun setPeakThreshold(threshold: Float) = Unit
        },
    ) : this(
        controller = KnockledgeController(
            capturer = group.two.knockledge.audio.RingAudioCapturer(
                audioSource = audioSource,
                buffer = buffer,
            ),
            knockDetector = knockDetector,
            pipeline = RewindPipeline(transcriber, filter, summarizer),
            recents = recents,
            hooks = hooks,
        ),
        recents = recents,
        models = null,
        ai = null,
    )
```

- [ ] **Step 4: Compile Kotlin code to verify DI wiring**

Run:
```powershell
./gradlew compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

---

### Task 5: Update Existing Unit Tests and Verify

**Files:**
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockledgeNoteSinkTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt`

**Interfaces:**
- Consumes: Updated `KnockledgeController` constructor
- Produces: Passing test suite

- [ ] **Step 1: Update KnockledgeNoteSinkTest.kt**

In `androidApp/src/test/kotlin/two/knockledge/KnockledgeNoteSinkTest.kt`:
Update constructor invocation:

```kotlin
        val capturer = group.two.knockledge.audio.RingAudioCapturer(
            audioSource = FakeSource(),
            buffer = FakeBuffer(wav()),
        )
        val dummyDetector = object : group.two.knockledge.sensor.KnockGestureDetector {
            override fun startListening(onKnock: () -> Unit) = Unit
            override fun stopListening() = Unit
            override fun setPeakThreshold(threshold: Float) = Unit
        }
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = dummyDetector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            noteSink = sink,
            hooks = NoopSessionHooks,
            minAudioSaveSeconds = MutableStateFlow(2),
        )
```

- [ ] **Step 2: Update RewinderSaveThresholdTest.kt**

In `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt`:
Update constructor invocation:

```kotlin
        val capturer = group.two.knockledge.audio.RingAudioCapturer(
            audioSource = DummySource(chunks = 1),
            buffer = dummyBuffer,
        )
        val dummyDetector = object : group.two.knockledge.sensor.KnockGestureDetector {
            override fun startListening(onKnock: () -> Unit) = Unit
            override fun stopListening() = Unit
            override fun setPeakThreshold(threshold: Float) = Unit
        }
        val controller = KnockledgeController(
            capturer = capturer,
            knockDetector = dummyDetector,
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            hooks = NoopSessionHooks,
            minAudioSaveSeconds = minSaveSeconds,
        )
```

- [ ] **Step 3: Run targeted test suite**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.KnockledgeNoteSinkTest" --tests "two.knockledge.RewinderSaveThresholdTest"
```
Expected: PASS.

- [ ] **Step 4: Run full unit test suite for modified modules**

Run:
```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.audio.RingAudioCapturerTest" --tests "two.knockledge.KnockledgeControllerSessionTest" --tests "group.two.knockledge.KnockRecordingViewModelTest"
```
Expected: All tests PASS.

---

## Self-Review

1. **Spec coverage**: Covers `RingAudioCapturer`, repurposed `KnockledgeController` (session controller), and `KnockRecordingViewModel` (`ViewModel` extension) as designed in spec.
2. **Placeholder scan**: Zero placeholders, TODOs, or missing code blocks.
3. **Type consistency**: Method and property names match across all 5 tasks.
