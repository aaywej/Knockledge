# Quickstart: Silero VAD Audio Pre-Processing Validation

**Feature**: `009-silero-vad-preprocessing`  
**Date**: 2026-10-31  
**Status**: Ready for Implementation

This quickstart guides developers through testing and verifying neural voice detection, non-speech noise rejection, dead air truncation, and original audio preservation.

---

## 1. Prerequisites

- Android SDK installed with `adb` available.
- Clean working directory on branch `009-silero-vad-preprocessing`.
- Unit tests run via Gradle in PowerShell.

---

## 2. Fast JVM Unit Test Verification

Execute the test suites for neural voice activity detection:

```powershell
# Run SileroAudioPreprocessor and VadDetector unit tests
.\gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.pipeline.SileroAudioPreprocessorTest"

# Run existing AudioPreprocessor regression tests
.\gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.pipeline.AudioPreprocessorTest"

# Run RewindPipeline integration tests
.\gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.pipeline.RewindPipelineTest"
```

### Expected Results

- `SileroAudioPreprocessorTest`:
  - Neural/mock VAD correctly flags non-speech audio (typing, table knocks) as inactive.
  - Retains all frames with speech probability >= 0.5.
  - Preserves 300 ms silence padding around voice segments.
  - Truncates silent and non-speech gaps exceeding 1,500 ms down to 300 ms padding.
  - Scales speech audio to target peak headroom (`~29,490`).
  - Returns `AudioPreprocessResult.InsufficientSpeech` when speech duration is < 1.0 second.
  - Returns `AudioPreprocessResult.Success` when speech duration is >= 1.0 second.
- `AudioPreprocessorTest`:
  - Regression tests for `DefaultAudioPreprocessor` continue to pass.
- `RewindPipelineTest`:
  - Pipeline executes preprocessor stage, handles insufficient speech, and routes conditioned speech to transcription.

---

## 3. End-to-End Scenario Verification

### Scenario A: Speech with Non-Speech Background Noise (Keystrokes & Desk Knocks)
1. Provide recorded audio containing 10 seconds of spoken voice mixed with keyboard typing and desk thumps.
2. Execute `SileroAudioPreprocessor.process()`.
3. **Verify**:
   - Neural classifier marks typing and knocks as non-speech.
   - Retained audio contains only spoken phrases with 300 ms padding.
   - AI transcriber processes clean speech audio.

### Scenario B: Room Silence and Transient Clatter (< 1.0s Voice)
1. Provide audio with 5 seconds of room silence and a brief 200 ms book drop.
2. Execute `rewind()` in `KnockledgeController`.
3. **Verify**:
   - Preprocessor returns `AudioPreprocessResult.InsufficientSpeech`.
   - Transcriber and summarizer are **not** invoked.
   - Note is **not** saved to `NoteRepository`.
   - Controller updates `error` with `"No speech detected in recorded audio."`.

### Scenario C: Original Audio Preservation
1. Record a full session with conversational pauses and trigger a rewind.
2. Inspect the saved note in `NoteRepository`.
3. **Verify**:
   - The saved note audio file contains the authentic, unedited WAV recording.
   - Playback retains all original pauses and acoustics.

---

## 4. Live Check

The Silero VAD ONNX model is not bundled: on device it is downloaded via `ModelStore`
("Silero VAD (ONNX)", ~2.3 MB) from the Models screen; for JVM tests it is fetched by script.

1. Fetch the model for unit tests (one-time; the file is gitignored):

   ```powershell
   .\scripts\fetch-vad-model.ps1
   ```

2. Run the real ONNX inference test:

   ```powershell
   .\gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.pipeline.OnnxSileroVadDetectorModelTest"
   ```

3. On device: install the debug build, open the Models screen and let **Silero VAD (ONNX)**
   download, then record a session and trigger a rewind. The active detector is logged once:

   ```powershell
   adb logcat -s SileroVad:I
   # "voice detection: Silero VAD neural model"  -> ONNX path active
   # "voice detection: energy fallback, no Silero model" -> model missing, energy VAD in use
   ```
