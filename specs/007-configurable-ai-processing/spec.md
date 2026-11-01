# Feature Specification: Configurable AI Processing and Manual Trigger

**Feature Branch**: `007-configurable-ai-processing`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "auto transcription and summarization must be user configurable. by default, ai processing must be user triggered in the ui. /speckit-specify"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Default Manual AI Processing on Demand (Priority: P1)

Users capture audio through a gesture or manual save. The system saves the audio recording immediately without running speech transcription or AI summarization. Users open the note in the application and tap a button to trigger transcription and summary generation on demand.

**Why this priority**: Users save battery, bandwidth, and processing quotas by choosing when to process captured audio.

**Independent Test**: Save an audio snapshot with default settings. Verify that the note contains audio without transcript or summary. Tap the process action on the note screen. Verify that the system produces the transcript and summary.

**Acceptance Scenarios**:

1. **Given** default application settings, **When** the user captures a circular audio buffer, **Then** the system saves the audio note immediately with no transcription or AI summary.
2. **Given** an unprocessed audio note, **When** the user opens the note and selects the AI process action, **Then** the system executes speech transcription and AI summarization and displays the results.
3. **Given** an audio note undergoing processing, **When** the user views the note, **Then** the interface displays clear progress indicators for transcription and summarization.

---

### User Story 2 - User-Configurable Automatic Processing Setting (Priority: P2)

Users configure whether newly saved audio captures process automatically or wait for manual triggers. Users navigate to application settings and toggle automatic AI processing. The toggle defaults to off.

**Why this priority**: Users who desire automated hands-free transcription and summaries can re-enable immediate post-save processing.

**Independent Test**: Navigate to settings and enable automatic AI processing. Save an audio snapshot. Verify that the system executes transcription and summarization immediately without extra user action.

**Acceptance Scenarios**:

1. **Given** a new or existing installation, **When** the user navigates to settings, **Then** the automatic AI processing preference displays in the disabled state by default.
2. **Given** the user enables automatic AI processing in settings, **When** a buffer save occurs, **Then** the system triggers transcription and summarization immediately after saving the audio.
3. **Given** the user disables automatic AI processing in settings, **When** a buffer save occurs, **Then** the system saves only the audio recording and awaits manual processing.

---

### User Story 3 - Resilient Processing State and Error Handling (Priority: P3)

When on-demand or automated AI processing encounters errors or network loss, the system displays the failure state and allows the user to retry processing without losing the underlying audio.

**Why this priority**: Audio recordings must remain safe and accessible even when speech-to-text or summary generation fails.

**Independent Test**: Disconnect network while cloud processing is selected and trigger processing on a saved note. Verify that the system displays an error state and provides a retry option.

**Acceptance Scenarios**:

1. **Given** an unprocessed note, **When** on-demand processing fails due to network disconnection or timeout, **Then** the system marks the processing as failed and displays a retry action.
2. **Given** a failed processing state, **When** the user taps the retry action, **Then** the system restarts the transcription and summarization flow.
3. **Given** an active processing job on a note, **When** the user taps the process action again, **Then** the system ignores duplicate requests.

---

### Edge Cases

- What happens if the user leaves the note screen while manual AI processing is in progress? The processing continues in the background and updates the note record upon completion.
- What happens if the device reboots while manual AI processing runs? The note retains its audio file and resets to the unprocessed state so the user can re-trigger processing.
- What happens if the user triggers AI processing on an audio recording shorter than the minimum valid speech length? The system displays a validation warning and aborts the pipeline before invoking AI models.
- What happens if the audio note already has a transcript and summary? The interface displays the existing content and offers an option to regenerate the summary if desired.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide an application setting to toggle automatic AI processing after audio capture.
- **FR-002**: The system MUST set the default value of the automatic AI processing setting to disabled.
- **FR-003**: When automatic AI processing is disabled, the system MUST save the audio recording without initiating transcription or summarization.
- **FR-004**: The system MUST provide an explicit user action within the note view to trigger on-demand transcription and summarization for any unprocessed audio note.
- **FR-005**: The system MUST display real-time status indicators in the user interface during on-demand AI processing.
- **FR-006**: When automatic AI processing is enabled, the system MUST execute transcription and summarization immediately after saving an audio capture.
- **FR-007**: The system MUST preserve captured audio files and allow audio playback regardless of note processing status.
- **FR-008**: The system MUST prevent concurrent duplicate AI processing tasks on the same audio note.
- **FR-009**: The system MUST display an informative error message and provide a retry action if on-demand AI processing encounters a failure.

### Key Entities

- **AI Processing Configuration**: User preference specifying whether audio capture triggers immediate automated AI processing or requires manual initiation.
- **Note Processing State**: Lifecycle state of a note's AI content, tracking transitions between Unprocessed, Processing (Transcribing / Summarizing), Completed, and Failed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Saving an audio capture without automatic AI processing completes within 1 second.
- **SC-002**: 100% of newly installed application instances default to disabled automatic AI processing.
- **SC-003**: Users can initiate on-demand AI processing from the note view with a single tap.
- **SC-004**: The user interface displays updated processing progress within 500 milliseconds of state transitions.
- **SC-005**: 100% of captured audio files remain intact and playable even when AI processing fails or is deferred.

## Assumptions

- The existing transcription and summarization engines remain unchanged and support both automatic and on-demand invocation.
- Audio playback and audio metadata persistence function independently of transcription and summary presence.
- Defaulting automatic processing to disabled aligns with battery and data conservation priorities.
