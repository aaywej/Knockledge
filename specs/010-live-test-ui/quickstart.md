# Quickstart: Utilitarian Material 3 Live Test UI

**Feature**: `010-live-test-ui`  
**Date**: 2026-11-01  
**Status**: Completed

## Overview
This document guides developers and testers through running and validating the unified Material 3 live test UI on a real Android device or emulator.

---

## 1. Prerequisites

- **Host Environment**: Android SDK 37 (API level 37), minSdk 26 (Android 8.0+), Java 17+
- **Target Device**: Physical Android device (recommended for accelerometer knock detection) or Android Virtual Device (AVD) with microphone enabled
- **Permissions**: `RECORD_AUDIO` granted at runtime

---

## 2. Fast JVM Verification

Run the test suite before deploying to verify that all business logic and ViewModels pass:

```powershell
./gradlew test
```

Expected output:
```text
BUILD SUCCESSFUL
All tests passing (including KnockRecordingViewModelTest, NoteProcessingViewModelTest, SettingsViewModelTest, etc.)
```

---

## 3. Deploying to Hardware or Emulator

Build and install the debug APK onto the connected device:

```powershell
./gradlew installDebug
```

Or launch directly using the Android CLI:

```powershell
android run
```

---

## 4. End-to-End Live Validation Scenarios

### Scenario 1: Utilitarian Material 3 Tab Navigation
1. Launch the app on the device.
2. Observe the clean, utilitarian Material 3 `Scaffold` with tabs: **RECORD**, **NOTES**, and **SETTINGS**.
3. Tap across each tab and confirm that the view switches instantaneously without screen flicker or lag.

### Scenario 2: Live Audio Recording & Buffer Verification
1. On the **RECORD** tab, observe the initial state ("Buffered 0s of 300s", inactive).
2. Tap "Start Session" (grant microphone permission if prompted).
3. Speak into the microphone for 10-15 seconds.
4. Verify that "Buffered Xs of Ys" increments in real time.
5. Move the buffer window slider to 60s and verify that the target capacity updates immediately.

### Scenario 3: Live Knock Gesture Capture
1. While recording is active, firmly knock twice on the back of the phone body or tap the "Simulate Knock Gesture" button.
2. Confirm the immediate visual feedback: "KNOCK DETECTED!" pulse chip appears within 500 ms.
3. Observe the "Saving snapshot..." indicator while the rewind audio is extracted and saved to storage.
4. Confirm "Saved" status appears upon completion.

### Scenario 4: Note History & Utilitarian Audio Playback
1. Switch to the **NOTES** tab.
2. Verify that the note captured in Scenario 3 appears at the top of the list with timestamp and duration.
3. Tap the note item to open its playback details.
4. Press the Play button. Confirm that audio plays audibly through the device speaker.
5. Drag the seek slider; verify audio position seeks accurately.
6. If chapter markers exist, tap a chapter marker; verify playback jumps directly to that timestamp.

### Scenario 5: On-Demand AI Processing Inspection
1. With the note selected in the **NOTES** tab, scroll to the AI Intelligence section.
2. If the note is unprocessed, tap the "Run AI Processing" button.
3. Observe real-time stage indicators (Preprocessing -> Transcribing -> Summarizing -> Completed).
4. Verify that the generated full transcript, executive summary, and bullet points display clearly in plain Material 3 text cards.

### Scenario 6: Utilitarian Settings & Authentication
1. Switch to the **SETTINGS** tab.
2. Toggle the "Automatic AI Processing" switch off and on.
3. Return to the **RECORD** tab and back to **SETTINGS**; confirm that the switch state persists.
4. In the Account card, verify that current authentication status is clearly displayed.
5. Perform test sign-in or sign-out to verify authentication state updates reactively.
