# Feature Specification: Live Test UI

**Feature Branch**: `010-live-test-ui`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "hook up a simple and lean ui build just enough so i can test everthing live"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Live Audio Recording and Knock Capture (Priority: P1)

A user opens the application to test continuous recording and knock detection on the physical device. The interface shows current audio buffer fill levels and active recording status. The user knocks on the device to trigger a capture. The interface displays immediate visual confirmation and saves the audio segment.

**Why this priority**: Testing continuous recording and gesture capture live on hardware validates the core value proposition of the product.

**Independent Test**: Start the recording session, tap or knock the device, and observe the immediate capture confirmation on the screen.

**Acceptance Scenarios**:

1. **Given** an active recording session, **When** the user knocks on the device body, **Then** the screen displays capture confirmation within 500 milliseconds.
2. **Given** an active recording session, **When** the user presses the manual save button, **Then** the system saves the buffered audio window.
3. **Given** an empty audio buffer, **When** the user attempts a capture, **Then** the screen displays an insufficient audio notification.

---

### User Story 2 - Note History and Audio Playback (Priority: P2)

A user navigates to the notes list to inspect captured recordings. The user selects a saved note to review. The player interface plays back the audio, shows elapsed time, and allows seeking across detected chapter marks.

**Why this priority**: Verifying audio persistence and playback confirms that saved circular buffers produce valid, audible content.

**Independent Test**: Select a freshly captured note from the list, press play, and verify audible playback and chapter selection.

**Acceptance Scenarios**:

1. **Given** at least one saved note, **When** the user views the notes list, **Then** the system displays note titles, timestamps, and durations.
2. **Given** a selected note with audio, **When** the user presses play, **Then** the system streams the recorded audio through the speaker.
3. **Given** an active audio playback, **When** the user selects a chapter marker, **Then** the playback position jumps directly to that timestamp.

---

### User Story 3 - AI Note Details and Processing Inspection (Priority: P3)

A user inspects the intelligence generated for a selected note. The user reads the transcript, summary, and action items. If a note lacks processing, the user triggers on-demand AI processing and watches the progress.

**Why this priority**: Live testing of transcription and summarization verifies the AI pipeline outputs.

**Independent Test**: Open an unprocessed note, tap the process button, and verify that the interface displays the resulting transcript and summary.

**Acceptance Scenarios**:

1. **Given** a note with completed AI processing, **When** the user opens note details, **Then** the system displays the full transcript and summary.
2. **Given** a note without AI processing, **When** the user taps the process button, **Then** the system shows real-time progress indicators until completion.
3. **Given** a processing failure, **When** an error occurs, **Then** the system displays the error cause and a retry option.

---

### User Story 4 - User Authentication and Profile Status (Priority: P4)

A user checks authentication status to test local versus cloud-synchronized operation. The user can authenticate with test credentials or remain in guest mode.

**Why this priority**: Live testing requires verifying both offline guest operations and remote synchronization paths.

**Independent Test**: Navigate to the authentication panel, enter test credentials or select guest mode, and verify that profile status updates immediately.

**Acceptance Scenarios**:

1. **Given** an unauthenticated session, **When** the user provides valid credentials, **Then** the interface displays authenticated user details.
2. **Given** an authenticated session, **When** the user signs out, **Then** the interface returns to guest status without deleting local notes.

---

### User Story 5 - Unified Navigation and Settings (Priority: P5)

A user moves smoothly between recording controls, note history, and configuration switches through a simple, lean navigation shell. The user toggles automated AI processing without restarting the application.

**Why this priority**: A cohesive navigation shell allows testing all capabilities end-to-end without modifying code between tests.

**Independent Test**: Navigate across all primary sections, change the automatic AI processing switch, and verify that the setting persists across screen transitions.

**Acceptance Scenarios**:

1. **Given** the main application interface, **When** the user taps a navigation destination, **Then** the active view switches in under 300 milliseconds.
2. **Given** the settings section, **When** the user toggles automatic processing, **Then** subsequent captures follow the updated processing rule.

---

### Edge Cases

- What happens when microphone permissions are denied on launch? The interface displays an explanatory banner and a button to open system permission settings.
- What happens when a knock occurs while the buffer duration is below the minimum capture threshold? The interface shows a temporary message informing the user that insufficient audio exists.
- What happens when the device loses network connectivity during cloud AI processing? The interface displays a clear network error and preserves the raw audio file for later retry.
- What happens when the notes list contains zero entries? The screen displays a clear empty state guiding the user to record their first audio note.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a live interface showing continuous audio buffer duration, recording state, and manual capture trigger.
- **FR-002**: The system MUST display visible real-time feedback whenever the accelerometer detects a valid knock gesture.
- **FR-003**: The system MUST display a chronologically ordered list of captured audio notes with creation timestamps and durations.
- **FR-004**: The system MUST provide audio playback controls including play, pause, time scrub slider, and chapter selection.
- **FR-005**: The system MUST display transcript text, summary text, and chapter markers for processed notes.
- **FR-006**: The system MUST allow users to manually trigger AI transcription and summarization for unprocessed notes.
- **FR-007**: The system MUST show current authentication status and provide controls to authenticate or disconnect.
- **FR-008**: The system MUST provide configuration controls to toggle automatic AI processing upon capture and adjust buffer window duration.
- **FR-009**: The system MUST provide a lean navigation mechanism connecting recording, note history, note detail, and settings into one operable test application.

### Key Entities *(include if feature involves data)*

- **Note**: Represents a captured audio recording. Contains unique identifier, audio file reference, creation timestamp, duration, optional transcript, optional summary, and chapter markers.
- **Chapter Marker**: Represents an identified segment within note audio. Contains title, start time offset, and summary excerpt.
- **Recording State**: Represents the live audio buffer status. Contains active status, current buffered seconds, maximum buffer capacity, and capture status.
- **User Session**: Represents the active identity state. Contains authentication status, user identifier, and display name or guest indicator.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can start recording, perform a physical knock gesture, and observe the saved note in the list in under 30 seconds.
- **SC-002**: 100% of captured notes display playable audio controls immediately upon selection.
- **SC-003**: Users can switch between recording controls, note list, and settings in a single interaction.
- **SC-004**: The interface provides visual feedback for knock events within 500 milliseconds of gesture completion.
- **SC-005**: Initial test screen renders in under 2 seconds on target devices.

## Assumptions

- The target test device or emulator provides functioning microphone input and accelerometer sensor events.
- Default buffer window size defaults to 30 seconds when unconfigured.
- Cloud transcription and remote synchronization require active internet connectivity.
- The live test UI prioritizes immediate functionality and direct access to test controls over elaborate styling.
