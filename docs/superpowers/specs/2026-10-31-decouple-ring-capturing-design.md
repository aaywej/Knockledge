# 3-Tier Recording & Session Architecture Design

## Goal

Decouple ring audio capturing from `KnockledgeController`, repurpose `KnockledgeController` as the Knockledge session controller (coordinating audio, knock gestures, and rewinds), and convert `KnockRecordingViewModel` into a standard Android Jetpack `ViewModel`.

## Architecture Overview

The system transitions to a clean 3-tier architecture:

```
[UI Screen / Compose]
         │
         ▼
[KnockRecordingViewModel]  (Jetpack ViewModel, viewModelScope)
         │
         ▼
[KnockledgeController]     (Domain Session Controller: audio + knock gestures + rewinds)
      ├──> [RingAudioCapturer]        (Audio ingestion & circular ring buffer)
      ├──> [KnockGestureDetector]     (Accelerometer knock sensor detection)
      └──> [RewindPipeline] / Sinks   (AI processing, recents, note recording)
```

### 1. Tier 1: Audio Engine (`RingAudioCapturer`)
- **Package**: `group.two.knockledge.audio`
- **Class**: `RingAudioCapturer`
- **Dependencies**: `AudioSource`, `LocalAudioRingBuffer`, `windowSeconds: StateFlow<Int>`, `CoroutineDispatcher` (single-threaded).
- **Responsibilities**:
  - Ingests audio chunks from `AudioSource` into `LocalAudioRingBuffer`.
  - Runs all buffer writes and snapshots on a dedicated single-threaded dispatcher.
  - Computes VU meter decibel level ballistics.
  - Monitors for digital silence and reports blocked microphone warnings.
  - Updates buffer duration dynamically from `windowSeconds`.
  - Provides thread-safe snapshot extraction: `suspend fun snapshot(): File`.
  - Emits `StateFlow<RingCaptureState>` (`isRunning`, `bufferedMillis`, `windowMillis`, `level`, `inputLabel`, `inputDb`, `inputWarning`, `error`).

### 2. Tier 2: Session Controller (`KnockledgeController`)
- **Package**: `group.two.knockledge`
- **Class**: `KnockledgeController` (first-class domain class, not a typealias).
- **Dependencies**: `RingAudioCapturer`, `KnockGestureDetector`, `RewindPipeline`, `RecentsRepository`, `NoteSink?`, `SessionHooks`, `minAudioSaveSeconds: StateFlow<Int>`, `autoAiProcessingEnabled: StateFlow<Boolean>`.
- **Responsibilities**:
  - Controls the user-facing Knockledge listening session.
  - `start()`: starts audio capture via `RingAudioCapturer`, starts gesture detection via `KnockGestureDetector.startListening { rewind(TriggerType.GESTURE) }`, and triggers `SessionHooks.onSessionStarted()`.
  - `stop()`: stops gesture detection, stops audio capture, and triggers `SessionHooks.onSessionStopped()`.
  - `rewind(trigger)`: snapshots audio from `RingAudioCapturer`, runs `RewindPipeline` (transcription, summarization), records note to `NoteSink`, and saves to `RecentsRepository`.
  - Emits `StateFlow<KnockledgeState>` combining capture metrics, gesture listening status, rewind status, and errors.
  - Deletes redundant `RecordingBackend` adapter interface.

### 3. Tier 3: UI Presentation Layer (`KnockRecordingViewModel`)
- **Package**: `group.two.knockledge.ui`
- **Class**: `KnockRecordingViewModel : ViewModel(), KnockRecordingController`
- **Dependencies**: `KnockledgeController`, `AppSettings`.
- **Responsibilities**:
  - Inherits from `androidx.lifecycle.ViewModel`.
  - Replaces external `CoroutineScope` parameter with `viewModelScope`.
  - Implements `KnockRecordingController` interface for UI and Compose preview contract.
  - Exposes `uiState: StateFlow<KnockRecordingUiState>` combining `KnockledgeController.state` and `AppSettings`.
  - Delegates UI intents (`startSession()`, `stopSession()`, `triggerManualSave()`, `setBufferDurationSeconds()`) directly to `KnockledgeController` and `AppSettings`.

## Service Simplification
- In `KnockledgeService`, remove manual `KnockGestureDetector.startListening` in `observe()` and `onDestroy()`.
- `KnockledgeService` starts and stops `KnockledgeController`, which naturally handles gesture detection as part of the session lifecycle.

## Testing Strategy
1. **`RingAudioCapturerTest`**: Pure JVM tests verifying frame capture, buffer duration change, silence detection, and start/stop clearing.
2. **`KnockledgeControllerTest`**: JVM tests verifying session activation (audio + gestures), rewind triggering from gestures and manual calls, note sink persistence, and state aggregation.
3. **`KnockRecordingViewModelTest`**: JVM tests verifying `uiState` emission, `startSession()`, `stopSession()`, and manual saves via `KnockledgeController`.
