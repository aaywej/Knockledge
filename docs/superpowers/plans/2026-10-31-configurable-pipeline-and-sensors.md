# Configurable Pipeline, Audio Threshold, and Sensors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make audio save threshold, knock sensor sensitivity, speech transcription mode, and AI comprehension mode user-configurable with dynamic reactive routing in local storage.

**Architecture:** Extend `AppSettings` with local `SharedPreferences` persistence for thresholds and modes. Wire reactive `StateFlow` streams into `RewinderController`, `AndroidKnockGestureDetector`, `CompositeTranscriber`, and a new `CompositeSummarizer` with `FirebaseAiSummarizer`. UI composables are excluded for now per explicit instruction.

**Tech Stack:** Kotlin, Coroutines, StateFlow, Android SharedPreferences, Dagger, Firebase AI Logic (Gemini Client), Whisper.cpp, JUnit4.

**Spec:** [docs/superpowers/specs/2026-10-31-configurable-pipeline-and-sensors-design.md](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/docs/superpowers/specs/2026-10-31-configurable-pipeline-and-sensors-design.md)

## Global Constraints
- **Local Persistence Only**: All settings must persist exclusively in local `SharedPreferences` via `PrefsStore`. Zero cloud dependency for settings.
- **Git Safety Rule**: Do not run `git commit` or `git push` autonomously. Keep changes in working tree until explicit user instruction.
- **No UI Composables Yet**: Defer UI components. Implement data models, controllers, pipeline routing, and unit tests only.
- **ASD-STE100 Rules**: Write simple direct Kotlin code and comments without confusing abstractions.

---

### Task 1: Expand `AppSettings` with Schema Keys and Defaults

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`
- Create: `androidApp/src/test/kotlin/two/knockledge/AppSettingsTest.kt`

**Interfaces:**
- Consumes: `KeyValueStore`, `MemoryStore`, `RewindPolicy`
- Produces:
  - `AppSettings.minAudioSaveSeconds: StateFlow<Int>` and `setMinAudioSaveSeconds(seconds: Int)`
  - `AppSettings.knockSensorThreshold: StateFlow<Float>` and `setKnockSensorThreshold(threshold: Float)`
  - `AppSettings.transcriptionMode: StateFlow<String>` and `setTranscriptionMode(mode: String)`
  - `AppSettings.transcriptionLocalModel: StateFlow<String>` and `setTranscriptionLocalModel(modelId: String)`
  - `AppSettings.comprehensionMode: StateFlow<String>` and `setComprehensionMode(mode: String)`

- [ ] **Step 1: Write the failing test**

Create `androidApp/src/test/kotlin/two/knockledge/AppSettingsTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsValuesArePopulatedCorrectly() {
        val store = MemoryStore()
        val settings = AppSettings(store, defaultSummaryModel = "qwen2b")

        assertEquals(5, settings.minAudioSaveSeconds.value)
        assertEquals(6.0f, settings.knockSensorThreshold.value, 0.001f)
        assertEquals("auto", settings.transcriptionMode.value)
        assertEquals("whisper", settings.transcriptionLocalModel.value)
        assertEquals("auto", settings.comprehensionMode.value)
        assertEquals("qwen2b", settings.summaryModel.value)
    }

    @Test
    fun settersUpdateAndPersistValues() {
        val store = MemoryStore()
        val settings = AppSettings(store, defaultSummaryModel = "qwen2b")

        settings.setMinAudioSaveSeconds(15)
        assertEquals(15, settings.minAudioSaveSeconds.value)
        assertEquals("15", store.getString("min_audio_save_seconds"))

        settings.setKnockSensorThreshold(4.0f)
        assertEquals(4.0f, settings.knockSensorThreshold.value, 0.001f)
        assertEquals("4.0", store.getString("knock_sensor_threshold"))

        settings.setTranscriptionMode("online")
        assertEquals("online", settings.transcriptionMode.value)
        assertEquals("online", store.getString("transcription_mode"))

        settings.setTranscriptionLocalModel("whisper_tiny")
        assertEquals("whisper_tiny", settings.transcriptionLocalModel.value)
        assertEquals("whisper_tiny", store.getString("transcription_local_model"))

        settings.setComprehensionMode("local")
        assertEquals("local", settings.comprehensionMode.value)
        assertEquals("local", store.getString("comprehension_mode"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.AppSettingsTest"`
Expected: Compilation failure or method not found on `AppSettings`.

- [ ] **Step 3: Write minimal implementation in `AppSettings.kt`**

Modify `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt` to add the properties, constants, and setter methods:

```kotlin
    private val _minAudioSaveSeconds = MutableStateFlow(
        store.getString(KEY_MIN_AUDIO_SAVE)?.toIntOrNull()?.coerceIn(5, 60) ?: DEFAULT_MIN_AUDIO_SAVE_SECONDS
    )
    val minAudioSaveSeconds: StateFlow<Int> = _minAudioSaveSeconds.asStateFlow()

    private val _knockSensorThreshold = MutableStateFlow(
        store.getString(KEY_KNOCK_THRESHOLD)?.toFloatOrNull() ?: DEFAULT_KNOCK_THRESHOLD
    )
    val knockSensorThreshold: StateFlow<Float> = _knockSensorThreshold.asStateFlow()

    private val _transcriptionMode = MutableStateFlow(
        store.getString(KEY_TRANSCRIPTION_MODE) ?: MODE_AUTO
    )
    val transcriptionMode: StateFlow<String> = _transcriptionMode.asStateFlow()

    private val _transcriptionLocalModel = MutableStateFlow(
        store.getString(KEY_TRANSCRIPTION_LOCAL_MODEL) ?: DEFAULT_TRANSCRIPTION_LOCAL_MODEL
    )
    val transcriptionLocalModel: StateFlow<String> = _transcriptionLocalModel.asStateFlow()

    private val _comprehensionMode = MutableStateFlow(
        store.getString(KEY_COMPREHENSION_MODE) ?: MODE_AUTO
    )
    val comprehensionMode: StateFlow<String> = _comprehensionMode.asStateFlow()

    fun setMinAudioSaveSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(5, 60)
        store.putString(KEY_MIN_AUDIO_SAVE, clamped.toString())
        _minAudioSaveSeconds.value = clamped
    }

    fun setKnockSensorThreshold(threshold: Float) {
        store.putString(KEY_KNOCK_THRESHOLD, threshold.toString())
        _knockSensorThreshold.value = threshold
    }

    fun setTranscriptionMode(mode: String) {
        store.putString(KEY_TRANSCRIPTION_MODE, mode)
        _transcriptionMode.value = mode
    }

    fun setTranscriptionLocalModel(modelId: String) {
        store.putString(KEY_TRANSCRIPTION_LOCAL_MODEL, modelId)
        _transcriptionLocalModel.value = modelId
    }

    fun setComprehensionMode(mode: String) {
        store.putString(KEY_COMPREHENSION_MODE, mode)
        _comprehensionMode.value = mode
    }

    companion object {
        const val MODE_AUTO = "auto"
        const val MODE_ONLINE = "online"
        const val MODE_LOCAL = "local"

        const val DEFAULT_MIN_AUDIO_SAVE_SECONDS = 5
        const val DEFAULT_KNOCK_THRESHOLD = 6.0f
        const val DEFAULT_TRANSCRIPTION_LOCAL_MODEL = "whisper"

        const val KEY_MIN_AUDIO_SAVE = "min_audio_save_seconds"
        const val KEY_KNOCK_THRESHOLD = "knock_sensor_threshold"
        const val KEY_TRANSCRIPTION_MODE = "transcription_mode"
        const val KEY_TRANSCRIPTION_LOCAL_MODEL = "transcription_local_model"
        const val KEY_COMPREHENSION_MODE = "comprehension_mode"
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.AppSettingsTest"`
Expected: PASS (2 tests pass).

---

### Task 2: Dynamic Audio Save Threshold in `RewindPolicy`, `RewinderController`, and `KnockledgeService`

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/core/RewindPolicy.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/RewinderController.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt`
- Create: `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt`

**Interfaces:**
- Consumes: `minAudioSaveSeconds: StateFlow<Int>`
- Produces: `RewinderController` with reactive save threshold, unlocking `canRewind` when `bufferedMillis >= minAudioSaveMillis`.

- [ ] **Step 1: Write the failing test**

Create `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.RewinderController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.InputStatus
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.pipeline.Pipeline
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RewinderSaveThresholdTest {
    private class DummyBuffer(var currentMillis: Long = 0L) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) {}
        override fun setWindowSeconds(seconds: Int) {}
        override fun bufferedMillis(): Long = currentMillis
        override fun snapshot(): File = File.createTempFile("dummy", ".wav")
        override fun clear() {}
    }

    private class DummySource : AudioSource {
        override fun frames(sampleRate: Int) = flowOf<ShortArray>()
        override suspend fun status(): InputStatus? = null
    }

    @Test
    fun canRewindObeysDynamicThreshold() {
        val dummyBuffer = DummyBuffer(4_000L) // 4 seconds buffered
        val minSaveSeconds = MutableStateFlow(5) // Requires 5 seconds
        val pipeline = RewindPipeline(PlaceholderTranscriber(), RegexSensitiveFilter(), PlaceholderSummarizer())

        val controller = RewinderController(
            audioSource = DummySource(),
            hooks = object : SessionHooks {},
            pipeline = pipeline,
            recents = InMemoryRecentsRepository(),
            buffer = dummyBuffer,
            minAudioSaveSeconds = minSaveSeconds,
        )

        controller.start()
        assertFalse("At 4s with 5s threshold, canRewind must be false", controller.state.value.canRewind)

        dummyBuffer.currentMillis = 5_000L
        // Re-evaluating or when 5000 reached
        minSaveSeconds.value = 4
        // With threshold updated to 4, 4s buffer unlocks
        assertTrue("At 4s with 4s threshold, canRewind must be true", controller.state.value.canRewind)
        controller.stop()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.RewinderSaveThresholdTest"`
Expected: Compilation failure (`minAudioSaveSeconds` parameter not found in `RewinderController`).

- [ ] **Step 3: Update `RewindPolicy.kt`, `RewinderController.kt`, and `KnockledgeService.kt`**

1. In `RewindPolicy.kt`:
Update `MIN_REWIND_SECONDS` to `5`:
```kotlin
    const val MIN_REWIND_SECONDS: Int = 5
    const val MIN_REWIND_MILLIS: Long = MIN_REWIND_SECONDS * 1000L
```

2. In `RewinderController.kt`:
Accept `minAudioSaveSeconds: StateFlow<Int> = MutableStateFlow(RewindPolicy.MIN_REWIND_SECONDS)` in constructor.
Update `RewinderState`:
```kotlin
data class RewinderState(
    val isRunning: Boolean = false,
    val bufferedMillis: Long = 0L,
    val windowMillis: Long = RewindPolicy.WINDOW_MILLIS,
    val minSaveMillis: Long = RewindPolicy.MIN_REWIND_MILLIS,
    val pendingRewinds: Int = 0,
    val level: Float = 0f,
    val stage: RewindStage? = null,
    val inputLabel: String? = null,
    val inputDb: Int? = null,
    val inputWarning: String? = null,
    val error: String? = null,
) {
    val canRewind: Boolean
        get() = isRunning && bufferedMillis >= minOf(minSaveMillis, windowMillis)

    val unlockProgress: Float
        get() = if (minSaveMillis <= 0L) 1f else (bufferedMillis.toFloat() / minSaveMillis).coerceIn(0f, 1f)

    val millisUntilUnlock: Long
        get() = (minSaveMillis - bufferedMillis).coerceAtLeast(0L)
}
```
In `RewinderController.init`:
```kotlin
scope.launch(audioDispatcher) {
    minAudioSaveSeconds.collect { sec ->
        _state.update { it.copy(minSaveMillis = sec * 1000L) }
    }
}
```

3. In `KnockledgeService.kt`:
Update status text and notification builder to use `model.minSaveMillis` or `formatDuration(model.millisUntilUnlock)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.RewinderSaveThresholdTest"`
Expected: PASS.

---

### Task 3: Dynamic Knock Sensor Sensitivity in `AndroidKnockGestureDetector`

**Files:**
- Modify: `sensor/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- Modify: `sensor/src/main/kotlin/group/two/knockledge/sensor/KnockGestureDetector.kt`
- Create: `sensor/src/test/kotlin/two/knockledge/DynamicKnockThresholdTest.kt`

**Interfaces:**
- Consumes: `setPeakThreshold(threshold: Float)`
- Produces: Updates `tap.maxTapPeakThreshold` directly on the heuristic detector instance.

- [ ] **Step 1: Write the failing test**

Create `sensor/src/test/kotlin/two/knockledge/DynamicKnockThresholdTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.sensor.AndroidKnockGestureDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class DynamicKnockThresholdTest {
    @Test
    fun setPeakThresholdUpdatesInternalTapThreshold() {
        // Test that setPeakThreshold modifies the detector's peak amplitude threshold
        val detector = AndroidKnockGestureDetector(null)
        detector.setPeakThreshold(4.0f)
        assertEquals(4.0f, detector.getPeakThreshold(), 0.001f)
        detector.setPeakThreshold(8.0f)
        assertEquals(8.0f, detector.getPeakThreshold(), 0.001f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew :sensor:test --tests "two.knockledge.DynamicKnockThresholdTest"`
Expected: FAIL (`setPeakThreshold` not found on `AndroidKnockGestureDetector`).

- [ ] **Step 3: Implement `setPeakThreshold` on `AndroidKnockGestureDetector`**

In `KnockGestureDetector.kt`:
```kotlin
interface KnockGestureDetector {
    fun startListening(onKnockDetected: () -> Unit)
    fun stopListening()
    val isListening: Boolean
    fun setPeakThreshold(threshold: Float) {}
    fun getPeakThreshold(): Float = 6.0f
}
```

In `AndroidKnockGestureDetector.kt`:
```kotlin
    override fun setPeakThreshold(threshold: Float) {
        tap.maxTapPeakThreshold = threshold
    }

    override fun getPeakThreshold(): Float = tap.maxTapPeakThreshold
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew :sensor:test --tests "two.knockledge.DynamicKnockThresholdTest"`
Expected: PASS.

---

### Task 4: Dynamic AI Transcription Routing in `CompositeTranscriber`

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/pipeline/CompositeTranscriber.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- Create: `androidApp/src/test/kotlin/two/knockledge/CompositeTranscriberRoutingTest.kt`

**Interfaces:**
- Consumes: `mode: () -> String` (`"auto"`, `"online"`, `"local"`)
- Produces: `CompositeTranscriber` routes to online or offline engine according to user mode.

- [ ] **Step 1: Write the failing test**

Create `androidApp/src/test/kotlin/two/knockledge/CompositeTranscriberRoutingTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeTranscriberRoutingTest {
    private class FakeEngine(val type: TranscriptionEngineType) : TranscriptionEngine {
        var callCount = 0
        override suspend fun process(segment: AudioSegment): TranscriptResult {
            callCount++
            return TranscriptResult(emptyList(), "test", false, "en", type)
        }
    }

    @Test
    fun routesAccordingToMode() = runBlocking {
        val online = FakeEngine(TranscriptionEngineType.CLOUD_GEMINI)
        val offline = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER)
        var currentMode = "auto"
        var isOnline = true

        val transcriber = CompositeTranscriber(
            online = online,
            offline = offline,
            isOnline = { isOnline },
            mode = { currentMode }
        )

        val pcm = ShortArray(16000) { 100 }

        // Mode: local -> must call offline even if online is available
        currentMode = "local"
        val r1 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r1.engine)
        assertEquals(1, offline.callCount)
        assertEquals(0, online.callCount)

        // Mode: online -> must call online
        currentMode = "online"
        val r2 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.CLOUD_GEMINI, r2.engine)
        assertEquals(1, online.callCount)

        // Mode: auto while offline -> falls back to offline
        currentMode = "auto"
        isOnline = false
        val r3 = transcriber.transcribe(pcm, 16000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r3.engine)
        assertEquals(2, offline.callCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.CompositeTranscriberRoutingTest"`
Expected: FAIL (`mode` parameter missing on `CompositeTranscriber`).

- [ ] **Step 3: Update `CompositeTranscriber.kt` and `KnockledgeComponent.kt`**

Modify `CompositeTranscriber.kt`:
```kotlin
class CompositeTranscriber(
    private val online: TranscriptionEngine,
    private val offline: TranscriptionEngine,
    private val isOnline: () -> Boolean,
    private val mode: () -> String = { "auto" },
    private val timeoutMillis: Long = 10_000,
) : Transcriber {
    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult {
        val conditioned = AudioConditioner.condition(pcm, sampleRate)
        if (conditioned.isEmpty()) {
            return TranscriptResult(emptyList(), "", false, "unknown", TranscriptionEngineType.LOCAL_WHISPER)
        }
        val segment = AudioSegment(conditioned, sampleRate, conditioned.size * 1000L / sampleRate, isGainNormalized = true)
        val currentMode = mode()

        if (currentMode == "local") {
            return offline.process(segment)
        }

        if (currentMode == "online" || (currentMode == "auto" && isOnline())) {
            try {
                return withTimeout(timeoutMillis) { online.process(segment) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
                if (currentMode == "online") throw e
            }
        }
        return offline.process(segment)
    }
}
```

In `KnockledgeComponent.kt`:
Pass `mode = { settings.transcriptionMode.value }` to `CompositeTranscriber`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.CompositeTranscriberRoutingTest"`
Expected: PASS.

---

### Task 5: Cloud AI Comprehension with `FirebaseAiSummarizer` and `CompositeSummarizer`

**Files:**
- Create: `androidApp/src/main/kotlin/group/two/knockledge/ai/FirebaseAiSummarizer.kt`
- Create: `androidApp/src/main/kotlin/group/two/knockledge/pipeline/CompositeSummarizer.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- Create: `androidApp/src/test/kotlin/two/knockledge/CompositeSummarizerTest.kt`

**Interfaces:**
- Consumes: `Summarizer`, `SummarizerPrompt`, `SummaryRequest`, `Summary`
- Produces: `CompositeSummarizer` dynamically selecting between cloud Gemini summarizer and on-device summarizer.

- [ ] **Step 1: Write the failing test**

Create `androidApp/src/test/kotlin/two/knockledge/CompositeSummarizerTest.kt`:

```kotlin
package two.knockledge

import group.two.knockledge.pipeline.CompositeSummarizer
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeSummarizerTest {
    private class MockSummarizer(val label: String) : Summarizer {
        override suspend fun summarize(request: SummaryRequest): Summary {
            return Summary(title = label, gist = "gist", points = listOf("p1"), redactedCount = 0)
        }
    }

    @Test
    fun routesComprehensionAccordingToMode() = runBlocking {
        val online = MockSummarizer("cloud")
        val offline = MockSummarizer("device")
        var mode = "auto"
        var isOnline = true

        val composite = CompositeSummarizer(
            online = online,
            offline = offline,
            isOnline = { isOnline },
            mode = { mode }
        )

        val req = SummaryRequest("sample text", false, 0, 5000L)

        // Local mode -> device
        mode = "local"
        assertEquals("device", composite.summarize(req).title)

        // Online mode -> cloud
        mode = "online"
        assertEquals("cloud", composite.summarize(req).title)

        // Auto mode while offline -> device
        mode = "auto"
        isOnline = false
        assertEquals("device", composite.summarize(req).title)

        // Auto mode while online -> cloud
        isOnline = true
        assertEquals("cloud", composite.summarize(req).title)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.CompositeSummarizerTest"`
Expected: FAIL (`CompositeSummarizer` not found).

- [ ] **Step 3: Implement `CompositeSummarizer.kt` and `FirebaseAiSummarizer.kt`**

1. Create `androidApp/src/main/kotlin/group/two/knockledge/pipeline/CompositeSummarizer.kt`:

```kotlin
package group.two.knockledge.pipeline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Routes summarization requests between [online] (Gemini cloud) and [offline] (on-device Qwen)
 * based on [mode] ("auto", "online", "local") and network status.
 */
class CompositeSummarizer(
    private val online: Summarizer,
    private val offline: Summarizer,
    private val isOnline: () -> Boolean,
    private val mode: () -> String = { "auto" },
    private val timeoutMillis: Long = 15_000,
) : Summarizer {
    override suspend fun summarize(request: SummaryRequest): Summary {
        val currentMode = mode()
        if (currentMode == "local") {
            return offline.summarize(request)
        }
        if (currentMode == "online" || (currentMode == "auto" && isOnline())) {
            try {
                return withTimeout(timeoutMillis) { online.summarize(request) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
                if (currentMode == "online") throw e
            }
        }
        return offline.summarize(request)
    }
}
```

2. Create `androidApp/src/main/kotlin/group/two/knockledge/ai/FirebaseAiSummarizer.kt`:

```kotlin
package group.two.knockledge.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.SummarizerPrompt
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.Summaries

/** Summarizes transcripts with cloud Gemini through Firebase AI Logic. */
class FirebaseAiSummarizer(
    private val outputLanguage: () -> String = { "auto" },
) : Summarizer {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.0-flash",
        )
    }

    override suspend fun summarize(request: SummaryRequest): Summary {
        val lang = outputLanguage()
        val systemPrompt = SummarizerPrompt.system(lang)
        val userPrompt = SummarizerPrompt.user(request, outputLanguage = lang)
        val fullPrompt = "$systemPrompt\n\n$userPrompt"

        return try {
            val response = model.generateContent(content { text(fullPrompt) })
            val replyText = response.text ?: return Summaries.nothingHeard(request.redactedCount, lang)
            Summaries.parse(replyText, request.redactedCount)
        } catch (e: Exception) {
            Summaries.nothingHeard(request.redactedCount, lang)
        }
    }
}
```

3. In `KnockledgeComponent.kt`:
Update `summarizer()` to wrap `OnDeviceSummarizer` and `FirebaseAiSummarizer` inside `CompositeSummarizer` with network check and `settings.comprehensionMode.value`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.CompositeSummarizerTest"`
Expected: PASS.

- [ ] **Step 5: Run full test suite verification**

Run: `.\gradlew testDebugUnitTest`
Expected: All tests PASS.

---

## Plan Review Checklist
1. **Spec Coverage**:
   - `min_audio_save_seconds` covered in Task 1 and Task 2.
   - `knock_sensor_threshold` covered in Task 1 and Task 3.
   - `transcription_mode` covered in Task 1 and Task 4.
   - `comprehension_mode` covered in Task 1 and Task 5.
2. **No Placeholders**: Complete implementation and test code provided for all tasks.
3. **Type Consistency**: Matches `AppSettings`, `RewinderController`, `CompositeTranscriber`, `Summarizer`, and `KnockGestureDetector` interfaces.
