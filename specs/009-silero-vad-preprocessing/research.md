# Research & Technology Decisions: Silero VAD Audio Pre-Processing

**Feature**: `009-silero-vad-preprocessing`  
**Date**: 2026-10-31  
**Status**: Completed

## 1. Machine Learning Model & Inference Engine

### Decision
Deploy the Silero VAD (v5) ONNX model (`silero_vad.onnx`) executed via the existing `onnxruntime-android` (`com.microsoft.onnxruntime:onnxruntime-android:1.20.0`) dependency.

### Rationale
- `onnxruntime-android` is already declared in `gradle/libs.versions.toml` and linked in `androidApp/build.gradle.kts`.
- Silero VAD is specifically trained for binary voice activity detection on 16 kHz audio, matching Knockledge's 16 kHz 16-bit mono PCM stream.
- Model size is approximately 2 MB, fitting within mobile resource budgets with sub-millisecond per-frame inference latency.
- Silero VAD effectively rejects non-speech sounds that fool simple RMS energy detectors, including table knocks, keyboard clicks, breathing, and air conditioning hum.

### Alternatives Considered
- **Google LiteRT / TensorFlow Lite**: Requires integrating an additional LiteRT model runtime or converting the model graph. Rejected because ONNX Runtime is already present.
- **WebRTC VAD (GMM)**: Statistical Gaussian mixture model. Requires C++ NDK compilation and does not generalize as well to non-stationary acoustic noise. Rejected.
- **Pure Energy RMS VAD (`DefaultAudioPreprocessor`)**: Retained as a graceful fallback when the neural model file or native ONNX runtime is unavailable (e.g., in local JVM unit tests).

---

## 2. Frame Sizing, Recurrent State, & Probability Gating

### Decision
Feed 512-sample chunks (32 ms at 16 kHz) sequentially into Silero VAD with state preservation across chunks. Classify frames with a speech probability >= 0.5 as active voice. Truncate non-speech runs longer than 1,500 ms to 300 ms ambient padding.

### Rationale
- Silero VAD v5 architecture requires sequential 512-sample inputs at 16 kHz and maintains internal recurrent states (`state` tensor of shape `[2, 1, 128]`).
- Resetting state at each chunk destroys temporal context and causes speech onset clipping. Passing state between sequential chunks accurately models conversational phrases.
- A 0.5 probability threshold provides high precision against transient acoustic noise while maintaining sensitivity to natural speech.
- 300 ms silence padding prevents cutting off trailing consonants and natural word transitions.

### Alternatives Considered
- **Independent 20 ms Framing**: Mismatches Silero's 512-sample requirement and loses recurrent temporal context. Rejected.
- **Aggressive Threshold (0.8)**: Suppresses soft voices and clips sentence endings. Rejected.
- **Sensitive Threshold (0.2)**: Admits ambient background noise and table vibrations. Rejected.

---

## 3. Pluggable Detector Architecture for JVM Testability

### Decision
Extract a `VadDetector` interface with two implementations:
1. `OnnxSileroVadDetector`: Production implementation executing `silero_vad.onnx` via `OrtEnvironment` and `OrtSession`.
2. `EnergyVadDetector` / `TestVadDetector`: Pure-Kotlin fallback implementation for fast JVM unit tests (`./gradlew test`) without native ONNX library requirements.

Create `SileroAudioPreprocessor` implementing the existing `AudioPreprocessor` contract.

### Rationale
- JVM unit tests run on host developer machines without Android ARM/x86 native `.so` libraries. Calling native ONNX runtime directly on JVM without desktop binaries throws `UnsatisfiedLinkError`.
- Decoupling the VAD detector behind `VadDetector` allows testing dead air truncation, 1.0s threshold rejection, and peak normalization on JVM while executing full neural inference on Android devices.
- Adheres directly to Karpathy Discipline (verifiable tests) and Ponytail Simplicity.

### Alternatives Considered
- **Direct Native ONNX Calls in Single Class**: Breaks fast JVM unit tests (`./gradlew test`), requiring slow emulator instrumentation tests. Rejected.

---

## 4. Pipeline Integration & Backward Compatibility

### Decision
`SileroAudioPreprocessor` implements the existing `AudioPreprocessor` interface. Provide `SileroAudioPreprocessor` in Dagger DI (`KnockledgeComponent`) and inject it into `RewindPipeline`.

### Rationale
- `RewindPipeline` already interacts with `AudioPreprocessor` through `process(pcm, sampleRate): AudioPreprocessResult`.
- Preserves the existing `AudioPreprocessResult.Success` and `AudioPreprocessResult.InsufficientSpeech` contracts.
- Requires zero changes to `RewindPipeline.kt` or `KnockledgeController.kt`.

### Alternatives Considered
- **Creating a Separate Pipeline**: Duplicates rewind pipeline code. Rejected.

---

## 5. Original Audio Preservation & Headroom Normalization

### Decision
Preserve the existing peak normalization logic (`TARGET_PEAK_AMPLITUDE = 29,490`) and guarantee that the original audio snapshot WAV file produced by `AudioRingBuffer.snapshot()` remains completely untouched.

### Rationale
- Pre-processed audio is strictly a temporary in-memory `ShortArray` passed to AI transcription.
- The user's saved note stores the original unmodified audio recording, preserving all natural pauses and background ambience for authentic playback.
