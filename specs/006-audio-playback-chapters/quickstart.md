# Quickstart: Audio Playback with Interactive Chapters

This quickstart guides developers through verifying audio playback, chapter markers, and scrub bar navigation.

## Prerequisites

- Android Virtual Device (API 34+) or connected physical device running Android 10+.
- Knockledge codebase built with `./gradlew assembleDebug`.
- A sample 16 kHz WAV audio file (or an existing saved note snapshot).

---

## Scenario 1: Unit Test Verification of Player State & Chapter Seeking

Run JVM unit tests verifying that seeking correctly updates `currentPositionMs` and calculates `activeChapter`.

```powershell
./gradlew testDebugUnitTest --tests "group.two.knockledge.audio.AndroidAudioPlayerTest"
```

**Expected Outcome**:
- `testPlayLocalFile_startsPlayback`: Passes. State transitions to `isPlaying = true`.
- `testSeekTo_updatesActiveChapter`: Passes. State selects chapter matching position $t \ge \text{startTimeMs}$.
- `testSeekPastDuration_clampsToEnd`: Passes. Position clamped to duration.

---

## Scenario 2: End-to-End Compose Preview Validation

Render Compose preview components to verify that chapter divisions appear on the progress slider:

```powershell
android studio render-compose-preview --file androidApp/src/main/kotlin/group/two/knockledge/ui/playback/AudioPlaybackControls.kt
```

**Verification Steps**:
1. Open the preview for `AudioPlaybackControlsPreview`.
2. Verify the progress bar contains distinct visual tick marks at chapter boundary percentages.
3. Verify that the chapter list displays titles, timestamps (e.g. `01:15`), and bullet points.
4. Verify that the currently active chapter is visually highlighted.

---

## Scenario 3: Device Playback & Chapter Navigation Test

1. Launch Knockledge on a test device:
   ```powershell
   android run
   ```
2. Save a note snapshot using a double knock or tapping **Save now**.
3. Open the note detail screen.
4. Tap **Play**: verify clear audio output from device speakers.
5. Tap on the second chapter in the chapter list:
   - Verify playback jumps immediately to the chapter start timestamp.
   - Verify audio output seamlessly continues from the new position.
6. Drag the seekbar into another chapter section:
   - Verify the displayed active chapter title updates in real time.
