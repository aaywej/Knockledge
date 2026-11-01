# Feature Specification: Silero VAD Audio Pre-Processing

**Feature Branch**: `009-silero-vad-preprocessing`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Pre-process audio using Silero VAD machine learning model to detect and retain voice segments while eliminating non-speech audio"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Voice-Retaining Rewind Audio Pre-Processing (Priority: P1)

A user triggers a rewind after a discussion in a noisy environment. The system processes the audio buffer through a neural voice activity detector. The preprocessor isolates spoken voice segments and removes dead air and ambient noises like keystrokes and table knocks. The system sends only normalized speech audio to the AI transcription service.

**Why this priority**: Filtering non-speech noises improves AI transcription accuracy and reduces AI token consumption. This provides the primary functional value.

**Independent Test**: Trigger a rewind on audio containing speech mixed with keyboard clicks and table taps. Verify that the preprocessor retains speech segments, removes non-speech noise, and routes conditioned speech to the AI.

**Acceptance Scenarios**:

1. **Given** recorded audio with spoken words and background keyboard typing, **When** rewind processing starts, **Then** the preprocessor identifies voice segments and excludes typing noise from the AI payload.
2. **Given** recorded audio with quiet voice passages, **When** pre-processing completes, **Then** the system normalizes peak volume levels across all retained speech segments.
3. **Given** valid speech in the rewind buffer, **When** pre-processing finishes, **Then** the AI pipeline receives conditioned voice audio and generates the summary.

---

### User Story 2 - Original Audio File Integrity (Priority: P2)

A user opens a saved note to play back the recorded session. The playback preserves the authentic, unedited microphone recording including natural pauses and room acoustics.

**Why this priority**: Users require unmodified audio recordings for historical accuracy and context. Pre-processing must never alter the archived audio file.

**Independent Test**: Record an audio session with background sounds, trigger a rewind, and listen to the saved note audio. Verify the audio file retains the original unedited sound.

**Acceptance Scenarios**:

1. **Given** an active recording session, **When** the preprocessor conditions audio for the AI pipeline, **Then** the system stores the unedited audio file in note storage.
2. **Given** a note in the recents list, **When** the user plays the note audio, **Then** the player plays the original unaltered recording.

---

### User Story 3 - Insufficient Speech Handling (Priority: P3)

A user triggers a rewind when only silence or ambient non-speech noise occurred. The neural voice detector detects no human speech and prevents unnecessary AI processing and empty note generation.

**Why this priority**: Halting execution when voice is absent saves battery, prevents wasted AI tokens, and avoids saving empty notes.

**Independent Test**: Trigger a rewind with only room silence or desk taps. Verify the system halts the AI pipeline and notifies the user.

**Acceptance Scenarios**:

1. **Given** recorded audio containing less than 1.0 second of detected human speech, **When** rewind pre-processing completes, **Then** the system aborts transcription and summarization.
2. **Given** an aborted rewind due to lack of speech, **When** processing halts, **Then** the system skips note creation and displays a notification.

---

### Edge Cases

- **Continuous Speech**: When recorded audio contains non-stop speech without pauses, the preprocessor retains the entire audio segment without clipping syllables.
- **Loud Non-Speech Impulses**: When sharp noises like door slams or table knocks occur, the neural classifier marks them as non-speech.
- **Short Speech Bursts**: When speech segments last less than 1.0 second in total, the system reports insufficient speech without crashing.
- **Low Signal-to-Noise Ratio**: When a speaker speaks quietly in ambient noise, the classifier retains voice passages while attenuating background hum.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST evaluate rewind audio frames with an on-device Silero VAD neural network.
- **FR-002**: The system MUST retain all audio frames classified as human speech.
- **FR-003**: The system MUST exclude non-speech audio segments exceeding 1,500 milliseconds from the AI processing payload.
- **FR-004**: The system MUST apply 300 milliseconds of silence padding around retained speech segments to preserve word boundaries.
- **FR-005**: The system MUST normalize the peak volume of conditioned speech segments to the target headroom level.
- **FR-006**: The system MUST preserve the original recorded audio snapshot in storage without modification.
- **FR-007**: When total detected speech duration is less than 1.0 second, the system MUST abort AI transcription and summarization.
- **FR-008**: When total detected speech duration is less than 1.0 second, the system MUST skip note creation and notify the user.

### Key Entities

- **Original Audio Snapshot**: The unmodified recorded audio file captured from the microphone buffer, preserved permanently for user note playback.
- **Conditioned Voice Audio**: An optimized temporary audio payload containing only verified voice segments with normalized gain, passed strictly to downstream AI models.
- **Speech Probability Map**: Time-indexed confidence values indicating the presence of human voice across audio intervals.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Pre-processing excludes at least 90% of non-speech ambient noises such as keyboard typing and desk knocks from the rewind AI payload.
- **SC-002**: Pre-processed voice audio preserves 100% of spoken words without clipping speech onset or offset.
- **SC-003**: On-device neural pre-processing finishes in under 500 milliseconds for a 30-second audio window.
- **SC-004**: 100% of saved note audio files retain original duration, pauses, and waveforms without modification.
- **SC-005**: 100% of audio buffers with less than 1.0 second of speech cleanly abort AI processing without errors.

## Assumptions

- Audio recording captures 16 kHz 16-bit mono PCM.
- The Silero VAD model runs locally on device without network requests.
- The user granted explicit approval for adding the on-device inference runtime dependency.
- Ambient silence padding of 300 milliseconds provides sufficient margin to prevent clipping speech transitions.
