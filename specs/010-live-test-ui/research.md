# Research & Technology Decisions: Utilitarian Material 3 Live Test UI

**Feature**: `010-live-test-ui`  
**Date**: 2026-11-01  
**Status**: Completed

## 1. UI Shell & Navigation Architecture

### Decision
Implement a single-activity Material 3 utilitarian shell in `MainActivity` using `Scaffold` and `TabRow` (or `NavigationBar`) with three explicit test tabs:
1. **RECORD**: Real-time audio buffer progress, start/stop recording session, manual snapshot trigger, buffer window slider, and real-time knock event sensor feedback.
2. **NOTES**: Reactive list of captured notes from `NoteRepository`, note selection, playback controls (`AudioPlaybackControls`) with chapter markers, and on-demand AI processing controls (`NoteProcessingControls`).
3. **SETTINGS**: Authentication panel (`AuthScreen` integration with user profile status), automatic AI processing toggle (`AiProcessingSettingsControl`), buffer duration settings, and knock sensitivity threshold.

### Rationale
- The user explicitly requested: "use material 3. maintain a lean build of the ui. no fancy styling. just utilitarian".
- A state-driven tabbed layout provides direct access to all system functions within 1 tap without navigating complex hierarchies or introducing third-party navigation libraries.
- Fits strictly within Ponytail Simplicity (Ponytail Principle: shortest working solution, no extra dependencies) and the 3-day hackathon limit (Constitution Principle I).

### Alternatives Considered
- **Jetpack Navigation Compose (`NavHost`) with multiple route destinations**: Introduces unnecessary routing ceremony, bundle arguments, and transition animations. Rejected to keep the build lean and utilitarian.
- **Single Long Scrolling Page**: Unusable on mobile screens when inspecting long transcripts while simultaneously monitoring audio playback and recording buffer levels. Rejected.
- **Drawer Navigation**: Adds hidden navigation states and extra gesture interception that conflicts with device accelerometer test handling. Rejected.

---

## 2. Knock Detection Feedback & Real-Time Sensor Verification

### Decision
Expose real-time knock detection status through a lightweight reactive pulse in the UI:
- When `AndroidKnockGestureDetector` triggers a knock event, update a high-priority UI timestamp state (`lastKnockEpochMs`).
- Display an instantaneous Material 3 `AssistChip` or `Badge` ("KNOCK DETECTED" with timestamp) that highlights prominently for 1.5 seconds.
- Provide a "Simulate Knock Gesture" button alongside hardware detection so developers can verify rewind capture pipelines both on emulators (without hardware accelerometers) and on physical hardware.

### Rationale
- Allows testers on both physical hardware and emulators to verify that accelerometer triggers propagate immediately to the recording buffer and note creation pipelines.
- Gives immediate visual confirmation before audio processing finishes.

### Alternatives Considered
- **Logcat Output Only**: Current implementation uses `Log.i("KnockTest", "KNOCK DETECTED LIVE")`. Invisible to a tester holding the phone away from a development computer. Rejected.
- **Android Toast Notification**: Asynchronous, throttled by the OS, and easy to miss during active testing. Rejected.

---

## 3. Component Reuse & Utilitarian Material 3 Styling

### Decision
Reuse existing pre-built UI components directly without creating redundant wrappers:
- `RecordingControls` for buffer time and snapshot saving.
- `AudioPlaybackControls` for audio play/pause, time scrubbing, and chapter markers.
- `NoteProcessingControls` for transcription/summary progress and error retry.
- `AiProcessingSettingsControl` for toggling auto-AI processing.
- `AuthScreen` for sign-in/registration/sign-out.
- Standard Material 3 controls (`Card`, `Text`, `Button`, `LinearProgressIndicator`, `Divider`, `ListItem`) for the note list and status metrics.
- Zero decorative graphics, vector animations, or custom draw modifiers.

### Rationale
- Follows Ponytail Simplicity: reuse existing code and helpers in the repository; write the shortest working solution.
- Eliminates duplicate code and satisfies the "no fancy styling, just utilitarian" constraint.
- All strings sourced from `strings.xml`.

### Alternatives Considered
- **Custom-drawn Canvas Waveforms & Visualizers**: Visually appealing but speculative, memory-intensive, and violates hackathon time limits. Rejected.

---

## 4. ViewModel & Repository Integration

### Decision
Maintain direct ViewModel-to-Repository and Controller bindings:
- `KnockRecordingViewModel` manages the active recording and buffer status via `KnockledgeController`.
- `NotePlaybackViewModel` manages audio playback via `AudioPlayer`.
- `NoteProcessingViewModel` manages transcription and summary triggering via `NoteRepository` and `RewindPipeline`.
- `SettingsViewModel` manages runtime preferences via `AppSettings`.
- `AuthViewModel` manages authentication via `AuthRepository`.
- Introduce a lightweight `LiveTestCoordinator` / state holder in `MainActivity` to coordinate note selection and active tab.

### Rationale
- Adheres directly to Constitution Principle VII: "Direct ViewModel-to-Repository communication MUST serve as the default architecture. Consider domain use cases or interactors ONLY when multiple ViewModels share complex business logic... Never introduce single-use or passthrough interactors."
- Retains existing tested ViewModels without breaking their contracts.

### Alternatives Considered
- **Creating a Domain Interactor for UI Tab Management**: Explicitly prohibited by Constitution Principle VII.
