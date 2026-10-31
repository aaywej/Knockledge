# Phase 0 Research: Configurable AI Processing and Manual Trigger

## Context & Problem Statement

Currently, triggering an audio buffer save (via double-knock gesture, manual button, or service notification) calls `KnockledgeController.rewind()`. This immediately and unconditionally executes `RewindPipeline.run()`, which performs speech transcription followed by AI summarization before saving the note.

The requirement states:
1. Automatic transcription and summarization must be user configurable.
2. By default, automatic AI processing must be disabled (off).
3. Audio capture must save the raw audio note immediately.
4. AI processing (transcription and summarization) must be triggered on demand by the user in the UI.

## Technical Decisions

### Decision 1: Persistence of the Auto-Processing Preference

- **Decision**: Add `autoAiProcessingEnabled: StateFlow<Boolean>` and `setAutoAiProcessingEnabled(enabled: Boolean)` to `AppSettings` backed by `KeyValueStore` (SharedPreferences).
- **Default Value**: `false` (disabled by default, matching requirements).
- **Key**: `"auto_ai_processing_enabled"`
- **Rationale**: Follows existing settings architecture (`transcriptionMode`, `bufferDurationSeconds`, `summaryModel`, etc.) and ensures persistent configuration across app restarts.
- **Alternatives Considered**:
  - In-memory toggle: Rejected because user preference would reset every session.
  - Room database configuration table: Rejected as overkill violating Ponytail simplicity; `KeyValueStore` is already the established settings mechanism.

### Decision 2: Audio Snapshot Saving Decoupled from Pipeline Execution

- **Decision**: Separate audio snapshot saving from immediate pipeline execution in `KnockledgeController` and `SnapshotNoteSink`.
  - When `autoAiProcessingEnabled` is `false`:
    - `KnockledgeController.rewind()` takes the audio snapshot (`buffer.snapshot()`).
    - Calculates audio duration and start/end timestamps.
    - Saves the audio note via `NoteRepository.createFromSnapshot()`.
    - Skips `pipeline.run()`.
  - When `autoAiProcessingEnabled` is `true`:
    - Proceeds with the full pipeline run (`pipeline.run()`) and records the note with transcript and summary via `SnapshotNoteSink.record()`.
- **Rationale**: Immediate audio saving ensures zero latency and low battery/network consumption on buffer capture.
- **Alternatives Considered**:
  - Always run pipeline in background and ignore results: Rejected because it wastes compute, battery, and API tokens.
  - Create note first in all cases, then run pipeline conditionally: This is the cleanest pattern and aligns with Unit 2's `createFromSnapshot()` -> `attachTranscript()` -> `attachSummary()`.

### Decision 3: On-Demand AI Processing Invocation

- **Decision**: Expose an on-demand processing action in `NoteRepository` or a dedicated coordinator/ViewModel (e.g. `NoteDetailsViewModel` or `KnockledgeController.processNoteAi(noteId)`).
  - The method reads the note's audio file (`AudioMetadata.localFilePath`).
  - Converts audio WAV to PCM.
  - Executes `RewindPipeline.run()` with progress reporting (`Transcribing`, `Summarizing`).
  - Calls `NoteRepository.attachTranscript()` and `NoteRepository.attachSummary()`.
- **Rationale**: Direct integration with existing `RewindPipeline` and `NoteRepository` without introducing speculative domain layers, complying with Constitution Principle VII.
- **Alternatives Considered**:
  - Background WorkManager job: Deferred for MVP hackathon scope; ViewModel coroutine scope with in-memory state provides immediate UI responsiveness and simple cancellation.

### Decision 4: Concurrency & State Management

- **Decision**: Track in-progress note processing by note ID using a concurrent set / StateFlow (e.g., `processingNoteIds: StateFlow<Set<String>>`) or note-scoped processing state enum (`IDLE`, `TRANSCRIBING`, `SUMMARIZING`, `FAILED`).
- **Rationale**: Prevents accidental concurrent triggers if the user repeatedly taps the "Process" button in the UI.
- **Alternatives Considered**:
  - Global boolean flag: Rejected because it blocks viewing other notes while one note is processing.
