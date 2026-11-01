# Feature Specification: Audio Pre-Processing Pipeline Unit

**Feature Branch**: `008-audio-preprocessing`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "completely remove filtering unit of the rewind pipeline. create a new unit in the pipeline that pre process the audio to apply dead airs eradication, audio normalization, and other processing to optimize the audio file before passing through AI. note that the original file must remain intact. the pre processing is only for the rewind pipeline"

## Clarifications

### Session 2026-10-31

- Q: What minimum active speech duration must remain after dead air eradication to proceed with AI processing and cache saving? → A: Require at least 1.0 second of active speech; if below 1.0 second, skip AI processing, skip note and cache saving, and inform the user.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Audio Pre-Processing for Rewind AI (Priority: P1)

A user triggers a rewind after a discussion with pauses and uneven volume. The system prepares the audio by removing dead air and normalizing volume levels before AI transcription and summarization. The user receives an accurate summary faster without waiting for silent periods to process.

**Why this priority**: Pre-processing audio improves AI transcription quality and cuts unnecessary processing time. It provides the primary functional value of this feature.

**Independent Test**: Trigger a rewind on recorded audio with silence and uneven speech levels. Verify that the AI receives normalized audio without dead air and generates the summary.

**Acceptance Scenarios**:

1. **Given** a recorded audio segment containing extended silent gaps, **When** a rewind triggers, **Then** the system removes dead air segments before sending audio to the AI engine.
2. **Given** audio with quiet speech passages, **When** the rewind processing starts, **Then** the system normalizes volume levels so speech is clear and uniform for AI recognition.
3. **Given** a rewind operation completes, **When** the recap card displays, **Then** the generated summary accurately reflects spoken content without delays caused by silent passages.

---

### User Story 2 - Original Audio File Preservation (Priority: P2)

A user accesses their saved note after triggering a rewind. The user plays back the recording and listens to the authentic audio capture. The playback preserves all natural pauses, original volume, and background context exactly as recorded.

**Why this priority**: Users need accurate historical records of spoken events for reference and legal integrity. Audio alterations must never overwrite permanent user archives.

**Independent Test**: Record an audio session, trigger a rewind, and inspect the saved audio file in the note archive. Verify the saved file retains all original silent sections and original volume levels.

**Acceptance Scenarios**:

1. **Given** an active audio recording session, **When** a rewind triggers and pre-processes audio for AI, **Then** the system stores the original unmodified audio recording in the user note archive.
2. **Given** a saved note in the recents list, **When** the user plays back the attached audio, **Then** the playback plays the original unedited audio stream with original pauses.

---

### User Story 3 - Unfiltered Transcript Flow (Priority: P3)

The rewind pipeline processes transcribed speech without passing through text redaction or keyword filtering units. The system delivers complete, unredacted speech text directly to the summarization stage.

**Why this priority**: Removing unnecessary text filtering units streamlines pipeline latency and eliminates unintended text alterations.

**Independent Test**: Transcribe audio containing sensitive words or technical phrases through the rewind pipeline. Verify that text flows directly to the summarizer without redactions or text masking.

**Acceptance Scenarios**:

1. **Given** transcribed speech from pre-processed audio, **When** the text moves to the summarizer, **Then** the system passes the full transcript without text filtering or redaction.
2. **Given** a generated summary recap, **When** the user reviews summary metrics, **Then** the system records zero redaction count and does not execute sensitive text filters.

---

### Edge Cases

- **Insufficient Speech Content**: When the rewind buffer contains less than 1.0 second of active speech after dead air eradication, the system skips AI processing, skips note and cache saving, and informs the user.
- **Continuous Speech**: When recorded audio contains no silent gaps, dead air eradication preserves all continuous audio content without clipping speech edges.
- **Extreme Audio Clamping**: When input audio contains loud transient spikes, volume normalization scales gain smoothly without inducing audio distortion.
- **Short Audio Buffer**: When a rewind triggers at the minimum duration threshold with valid speech, pre-processing completes reliably without producing empty buffers.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST remove text filtering and sensitive keyword redaction units from the rewind processing pipeline.
- **FR-002**: The system MUST insert an audio pre-processing unit into the rewind pipeline before AI transcription.
- **FR-003**: The audio pre-processing unit MUST detect and eradicate dead air segments exceeding standard speech pause thresholds from the rewind audio payload.
- **FR-004**: The audio pre-processing unit MUST apply volume normalization to optimize audio speech clarity before AI processing.
- **FR-005**: The system MUST preserve the original recorded audio file in full without modifications, cuts, or gain changes.
- **FR-006**: The system MUST apply audio pre-processing strictly to temporary audio data routed to the rewind AI pipeline.
- **FR-007**: The system MUST pass the unredacted transcript directly from transcription to summarization.
- **FR-008**: When remaining active speech after dead air eradication is less than 1.0 second, the system MUST skip AI transcription and summarization.
- **FR-009**: When remaining active speech after dead air eradication is less than 1.0 second, the system MUST skip note creation and cache saving.
- **FR-010**: When remaining active speech after dead air eradication is less than 1.0 second, the system MUST report user feedback that no speech content was detected.

### Key Entities

- **Original Audio Snapshot**: The unmodified recorded audio file captured from the microphone buffer, preserved permanently for user note playback.
- **Pre-Processed Rewind Audio**: An optimized temporary audio representation with dead air eradicated and levels normalized, used exclusively for AI processing.
- **Rewind Pipeline Context**: The operational workflow that coordinates audio pre-processing, transcription, and summarization without intermediate text filtering.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Audio pre-processing reduces rewind audio payload duration by at least 20% on recordings containing conversational pauses.
- **SC-002**: AI transcription and summarization processing latency improves by at least 15% due to reduced audio duration and normalized input.
- **SC-003**: 100% of saved note audio files retain original duration, pauses, and waveforms without modification.
- **SC-004**: Rewind pipeline executes zero text redaction operations across all transcript processing runs.
- **SC-005**: 100% of rewind captures containing less than 1.0 second of active speech skip AI processing and note persistence without system errors.

## Assumptions

- Standard conversational speech pauses under 500 milliseconds remain intact to preserve speech cadence and natural cadence.
- Silence exceeding 1.5 seconds classifies as dead air eligible for truncation or removal in the AI processing copy.
- Peak or loudness normalization targets standard conversational speech intelligibility levels.
- The original audio file format and storage location remain unchanged.
- User feedback for empty audio uses existing non-blocking status or error signaling channels without new Compose UI components.
