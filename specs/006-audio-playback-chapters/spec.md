# Feature Specification: Audio Playback with Interactive Chapters

**Feature Branch**: `006-audio-playback-chapters`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "okay use mediaplayer then. create a spec for everything we talked about"

## User Scenarios & Testing

### User Story 1 - Audio Playback for Saved Notes (Priority: P1)

Users need to listen to saved audio recordings from their captured notes. The user opens a note, taps play, and listens to the recorded audio. The user can pause playback and drag the seekbar to any point in the recording.

**Why this priority**: Listening to recorded audio provides the baseline validation for captured lectures and discussions.

**Independent Test**: Open a saved note with local audio. Tap play, verify audio output, pause playback, and verify that dragging the seekbar changes playback position.

**Acceptance Scenarios**:

1. **Given** a saved note with local audio, **When** the user taps play, **Then** audio starts playing and the progress bar updates continuously.
2. **Given** actively playing audio, **When** the user taps pause, **Then** audio stops immediately and the position remains unchanged.
3. **Given** audio in any state, **When** the user drags the seekbar, **Then** playback position updates to the selected timestamp.

---

### User Story 2 - Interactive Chapter Navigation in Playback (Priority: P2)

Users need to jump directly to specific topics within a recording without manual scrubbing. The playback progress bar displays visual chapter divisions. A list of chapters below the player shows titles and start timestamps. Tapping any chapter immediately seeks playback to that chapter start time.

**Why this priority**: Chapter navigation lets users locate relevant lecture segments quickly, matching the YouTube chapter experience.

**Independent Test**: Load a note with three chapter markers. Verify that the progress bar displays chapter divisions. Tap the second chapter and confirm playback jumps to the correct timestamp.

**Acceptance Scenarios**:

1. **Given** a note with chapter markers, **When** the user views the player, **Then** the progress bar displays visual chapter dividers.
2. **Given** a list of chapters, **When** the user taps a chapter item, **Then** audio seeks to the chapter start timestamp and continues playback.
3. **Given** active playback across chapter boundaries, **When** playback passes a chapter start time, **Then** the UI highlights the active chapter.

---

### User Story 3 - Automated Chapter Marker Generation (Priority: P3)

When generating note summaries, the system structures long recordings into timestamped chapters. The AI pipeline analyzes transcript timestamps and topic shifts to produce distinct chapter titles with start times and summary bullets.

**Why this priority**: Manual chapter creation burdens users. Automated chapters structure long recordings effortlessly.

**Independent Test**: Provide an audio transcript longer than 60 seconds. Trigger summary generation and verify that output contains timestamped chapter markers.

**Acceptance Scenarios**:

1. **Given** a processed transcript with timestamped speech, **When** summarization completes, **Then** the system attaches structured chapter markers to the note.
2. **Given** a recording with multiple topic transitions, **When** chapter generation runs, **Then** each chapter marker includes a valid start time, title, and key takeaways.

---

### Edge Cases

- What happens when the local audio file is missing or deleted? The system displays an error notice and disables playback controls.
- What happens when a note has no chapter markers? The progress bar displays a continuous unsegmented seekbar without chapter divisions.
- What happens when an incoming phone call occurs during playback? The system pauses audio playback automatically and resumes only upon explicit user action.
- What happens when the user seeks past the total audio duration? The system clamps playback position to the end of the recording and pauses.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST play local audio files associated with saved notes.
- **FR-002**: The system MUST provide play, pause, and seek controls for audio playback.
- **FR-003**: The system MUST display current playback position and total recording duration in minutes and seconds.
- **FR-004**: The system MUST render chapter dividers along the playback progress bar when chapter markers exist.
- **FR-005**: The system MUST seek audio playback to the start timestamp when a user selects a chapter marker.
- **FR-006**: The system MUST indicate the currently active chapter during playback in real time.
- **FR-007**: The system MUST generate timestamped chapter markers during AI note summarization.
- **FR-008**: The system MUST persist chapter markers with note summary metadata in local storage and remote database sync.

### Key Entities

- **Playback Session**: Current playback state including playing status, current position timestamp, and total duration.
- **Chapter Marker**: A timestamped segment containing start time in milliseconds, section title, and key summary bullets.
- **Note Audio**: The audio recording file linked to a note with local file path and optional remote URL.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Audio playback starts within 200 milliseconds of tapping the play control.
- **SC-002**: Chapter seek operations transition audio within 100 milliseconds without playback interruptions.
- **SC-003**: Playback progress updates the seekbar at least 10 times per second during active playback.
- **SC-004**: 100% of generated notes from audio longer than 60 seconds produce at least one timestamped chapter marker.

## Assumptions

- Recorded audio files exist in standard uncompressed 16 kHz WAV format on device storage.
- Audio playback operates offline without network connectivity when local audio files exist.
- Platform audio session APIs manage device speaker routing and audio focus interruptions.
