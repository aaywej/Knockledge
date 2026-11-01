# Design Specification: Configurable AI Pipeline, Audio Threshold, and Sensors

## 1. Overview
This specification defines the configuration architecture for Knockledge. The system replaces hard-coded audio save thresholds, knock sensor sensitivity levels, and AI model routing with user-configurable settings. All settings persist strictly on the device using Android SharedPreferences (`PrefsStore`). The runtime dynamically routes transcription and comprehension requests without mutating stored settings when network connectivity changes.

## 2. Architecture & Data Model

### 2.1 Settings Schema (`AppSettings`)
All keys persist in `PrefsStore` via `KeyValueStore`:
- `min_audio_save_seconds` (`Int`): Range `5` to `60`, default `5`.
- `knock_sensor_threshold` (`Float`): Default `6.0f` (Medium). High: `4.0f`, Medium: `6.0f`, Low: `8.0f`.
- `transcription_mode` (`String`): `"auto"` (default), `"online"`, `"local"`.
- `transcription_local_model` (`String`): Default `"whisper"`.
- `comprehension_mode` (`String`): `"auto"` (default), `"online"`, `"local"`.
- `summary_model` (`String`): Default `ModelCatalog.summarizerFor(totalRamBytes).id` (`"qwen2b"` or `"qwen08b"`).

### 2.2 Audio Save Threshold Subsystem
- `RewindPolicy.MIN_REWIND_SECONDS` updates to default `5`.
- `RewinderController` observes `minAudioSaveSeconds: StateFlow<Int>` from `AppSettings`.
- `RewinderController.canRewind` evaluates:
  `bufferedMillis >= minOf(minAudioSaveMillis, windowMillis)`
- `KnockledgeService` notification updates countdown and progress according to dynamic minimum save duration.

### 2.3 Knock Sensor Threshold Subsystem
- `AndroidKnockGestureDetector` receives `knockThreshold: StateFlow<Float>` from `AppSettings`.
- When threshold updates, `tap.maxTapPeakThreshold` updates dynamically.

### 2.4 AI Model Routing Subsystems

#### Transcription (`CompositeTranscriber`)
- Accepts `mode: () -> String`.
- `ONLINE`: Executes `FirebaseAiTranscriber` (cloud Gemini).
- `LOCAL`: Executes `WhisperTranscriberAdapter` (on-device Whisper).
- `AUTO`: Executes `FirebaseAiTranscriber` if network available, falls back to `WhisperTranscriberAdapter` on failure or offline.

#### Comprehension (`CompositeSummarizer` & `FirebaseAiSummarizer`)
- New `FirebaseAiSummarizer` implements `Summarizer` using Firebase AI Logic (`FirebaseGeminiClient`) returning structured `Summary`.
- `CompositeSummarizer` accepts:
  - `online: Summarizer` (`FirebaseAiSummarizer`)
  - `offline: Summarizer` (`OnDeviceSummarizer`)
  - `mode: () -> String`
  - `isOnline: () -> Boolean`
- `ONLINE`: Executes cloud summarizer.
- `LOCAL`: Executes on-device summarizer.
- `AUTO`: Executes cloud summarizer if online, falls back to on-device summarizer when offline.

## 3. Scope Boundary
- UI Composable screens and widgets are explicitly deferred and excluded from this phase.
- Only data models, controllers, pipelines, dependency injection, and unit tests are included.
