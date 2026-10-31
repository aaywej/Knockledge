# Quickstart: Audio Pre-Processing Pipeline Unit Validation

**Feature**: `008-audio-preprocessing`  
**Date**: 2026-10-31  
**Status**: Ready for Execution

This quickstart guides developers through testing and verifying the audio pre-processing unit, empty audio handling, and the removal of the text filtering unit.

---

## 1. Prerequisites

- Android SDK installed with `adb` available.
- Clean working directory on branch `008-audio-preprocessing`.
- Unit tests run via Gradle in PowerShell.

---

## 2. Fast JVM Unit Test Verification

Execute the test suites for audio pre-processing and the rewind pipeline:

```powershell
# Run AudioPreprocessor unit tests (VAD, dead air eradication, normalization, 1.0s threshold)
.\gradlew testDebugUnitTest --tests "two.knockledge.pipeline.AudioPreprocessorTest"

# Run updated RewindPipeline unit tests (verifies Preprocessing -> Transcribe -> Summarize without filtering)
.\gradlew testDebugUnitTest --tests "two.knockledge.pipeline.RewindPipelineTest"

# Run controller save and skip logic tests
.\gradlew testDebugUnitTest --tests "two.knockledge.RewinderSaveThresholdTest"
```

### Expected Results
- `AudioPreprocessorTest`:
  - Eradicates silent gaps exceeding 1.5 seconds down to 300 ms padding.
  - Normalizes quiet audio peaks to target headroom (`~29,490`).
  - Returns `InsufficientSpeech` when total active speech is `< 1.0` second.
  - Returns `Success` when total active speech is `>= 1.0` second.
- `RewindPipelineTest`:
  - When pre-processor returns `InsufficientSpeech`, pipeline returns `PipelineResult.InsufficientSpeech` without invoking transcriber or summarizer.
  - When speech is valid, pipeline runs preprocessing, transcribes, and directly passes unredacted text to summarizer with `redactedCount = 0`.
- `KnockledgeControllerTest` / `RewinderSaveThresholdTest`:
  - When pipeline returns `InsufficientSpeech`, note persistence is skipped, `_state.error` is updated with `"No speech detected in recorded audio."`, and the temporary WAV is cleaned up.
  - When pipeline succeeds, note is recorded via `SnapshotNoteSink` with the original unaltered WAV file.

---

## 3. End-to-End Scenario Verification

### Scenario A: Conversational Audio with Long Pauses (Dead Air Cut & Normalization)
1. Provide a 60-second synthetic or recorded WAV containing 20 seconds of quiet speech and 40 seconds of silent gaps.
2. Trigger `rewind()` in `KnockledgeController`.
3. **Verify**:
   - Audio pre-processor trims the 40 seconds of silence down to ~6 seconds of padding.
   - AI transcriber receives ~26 seconds of normalized audio instead of 60 seconds.
   - Saved note in `NoteRepository` contains the original 60-second WAV file with all original pauses intact.

### Scenario B: Pure Silence / Empty Audio Snapshot (Skip AI & Inform User)
1. Provide a 5-second WAV containing pure silence or room noise below speech threshold.
2. Trigger `rewind()`.
3. **Verify**:
   - `AudioPreprocessor` returns `AudioPreprocessResult.InsufficientSpeech`.
   - Transcriber and summarizer are **not** called.
   - Note is **not** saved to `NoteRepository` or `RecentsRepository`.
   - Temporary snapshot WAV is deleted from disk.
   - `KnockledgeController.state.value.error` contains `"No speech detected in recorded audio."`.
