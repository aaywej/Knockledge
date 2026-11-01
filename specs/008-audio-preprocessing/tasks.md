# Tasks: Audio Pre-Processing Pipeline Unit

**Input**: Design documents from `specs/008-audio-preprocessing/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`)

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Exact file paths included in all task descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Define the audio pre-processor contract and pipeline result models.

- [x] T001 Create `AudioPreprocessor` interface and `AudioPreprocessResult` sealed hierarchy (`Success` with `pcm: ShortArray`, `durationMillis: Long`, `sampleRate: Int`; `InsufficientSpeech` with `detectedSpeechMillis: Long`, `minimumRequiredMillis: Long = 1000L`) in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/AudioPreprocessor.kt`
- [x] T002 [P] Update `Pipeline.kt` to define `RewindStage.Preprocessing` and sealed `PipelineResult` (`Success(summary: Summary, transcript: TranscriptResult)`, `InsufficientSpeech(detectedSpeechMillis: Long, message: String)`) in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Pipeline.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement and test the core energy-based Voice Activity Detection and peak normalization engine.

**⚠️ CRITICAL**: Must complete before user story pipeline integration begins.

- [x] T003 Implement `DefaultAudioPreprocessor` in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/DefaultAudioPreprocessor.kt` using pure-Kotlin RMS Voice Activity Detection (20 ms frames, truncating dead air gaps > 1.5s down to 300 ms silence padding) and peak normalization targeting `29,490` amplitude (~ -1 dBFS headroom), returning `InsufficientSpeech` if retained speech duration is under `1,000` ms
- [x] T004 [P] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/pipeline/AudioPreprocessorTest.kt` verifying dead air eradication (>1.5s truncated to 300ms), volume normalization scaling to target headroom, and returning `InsufficientSpeech` when speech duration is < 1.0s

**Checkpoint**: Core audio pre-processor logic is independently tested and verified.

---

## Phase 3: User Story 1 - Audio Pre-Processing for Rewind AI (Priority: P1) 🎯 MVP

**Goal**: Integrate the pre-processor into `RewindPipeline` and handle insufficient speech abort in `KnockledgeController`.

**Independent Test**: Trigger a rewind with synthetic audio; verify dead air is eradicated before transcription, and verify that recordings with < 1.0s speech abort AI and notify the user via `reportError`.

### Tests for User Story 1

- [x] T005 [P] [US1] Write unit tests for `RewindPipeline` in `androidApp/src/test/kotlin/two/knockledge/pipeline/RewindPipelineTest.kt` verifying that `onStage(RewindStage.Preprocessing)` emits, `AudioPreprocessor.process()` executes, valid speech triggers transcription, and `InsufficientSpeech` aborts before transcription and summarization

### Implementation for User Story 1

- [x] T006 [US1] Refactor `RewindPipeline.run()` in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Pipeline.kt` to execute `AudioPreprocessor.process()` on input PCM, emit `RewindStage.Preprocessing`, pass normalized speech PCM to `Transcriber`, and return `PipelineResult.InsufficientSpeech` if speech is under 1.0s
- [x] T007 [US1] Update `KnockledgeController.rewind()` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt` to handle `PipelineResult`: on `PipelineResult.InsufficientSpeech`, call `reportError("No speech detected in recorded audio.")`, skip `recents.add` and `noteSink?.record()`, and delete the temporary WAV in `finally`
- [x] T008 [P] [US1] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/RewinderEmptyAudioTest.kt` verifying that audio captures with < 1.0s speech trigger error notification in `state.value.error`, skip note persistence, and delete the temporary audio snapshot file

**Checkpoint**: User Story 1 is fully functional and testable as an independent MVP.

---

## Phase 4: User Story 2 - Original Audio File Preservation (Priority: P2)

**Goal**: Guarantee that the saved audio file in user notes remains the complete, unmodified original recording.

**Independent Test**: Verify that the WAV file persisted by `SnapshotNoteSink` retains all original silent sections and original sample values.

### Tests for User Story 2

- [x] T009 [P] [US2] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/data/OriginalAudioPreservationTest.kt` confirming that pre-processing operates exclusively on an in-memory `ShortArray` and does not alter the bytes of the snapshot WAV file on disk

### Implementation for User Story 2

- [x] T010 [US2] Update `KnockledgeController.kt` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt` to verify that `readPcm(wav)` allocates a distinct `ShortArray` buffer, passes original `wav` directly to `noteSink?.record()`, and preserves full original audio for note playback

**Checkpoint**: User Stories 1 and 2 operate together; AI receives pre-processed audio while notes retain original recordings.

---

## Phase 5: User Story 3 - Unfiltered Transcript Flow (Priority: P3)

**Goal**: Completely remove the text filtering unit (`SensitiveFilter`) from the rewind pipeline, DI component, and preview graphs.

**Independent Test**: Transcribe audio containing sensitive tokens; verify that transcript text flows directly to summarization without redaction and `redactedCount` is zero.

### Implementation for User Story 3

- [x] T011 [P] [US3] Remove `SensitiveFilter` parameter, filtering stage, and redaction logic from `RewindPipeline` in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Pipeline.kt`, passing `labeled(transcript)` directly to `SummaryRequest` with `redactedCount = 0`
- [x] T012 [US3] Update Dagger provider `pipeline(transcriber: Transcriber, summarizer: Summarizer, preprocessor: AudioPreprocessor): RewindPipeline` in `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt` and provide `DefaultAudioPreprocessor()`
- [x] T013 [P] [US3] Update `KnockledgeGraph.kt` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeGraph.kt` to replace `filter: SensitiveFilter` with `preprocessor: AudioPreprocessor = DefaultAudioPreprocessor()`
- [x] T014 [P] [US3] Update existing tests referencing `SensitiveFilter` in `androidApp/src/test/kotlin/two/knockledge/KnockledgeNoteSinkTest.kt` and `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt` to remove filter parameters

**Checkpoint**: Text filtering unit is completely eliminated from the pipeline.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end verification and quality regression checks.

- [x] T015 [P] Run full test suite with `.\gradlew testDebugUnitTest` and confirm all pipeline, controller, and note sink tests pass
- [x] T016 Execute quickstart validation scenarios defined in `specs/008-audio-preprocessing/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 models — BLOCKS user story integration.
- **User Story 1 (Phase 3)**: Depends on Phase 2 (`DefaultAudioPreprocessor`).
- **User Story 2 (Phase 4)**: Depends on Phase 3 (`KnockledgeController` integration).
- **User Story 3 (Phase 5)**: Can proceed in parallel with or after Phase 3.
- **Polish (Phase 6)**: Depends on completion of all user story phases.

### Parallel Opportunities

- `T001` and `T002` can be developed in parallel (separate files).
- `T003` and `T004` can proceed with TDD in parallel.
- `T005`, `T008`, and `T009` test writing can run in parallel with implementation tasks.
- `T011`, `T013`, and `T014` can run in parallel once `Pipeline.kt` signature is updated.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1 (Contracts & Models: `T001`, `T002`).
2. Complete Phase 2 (VAD & Normalization: `T003`, `T004`).
3. Complete Phase 3 (Pipeline Integration & Skip Logic: `T005`–`T008`).
4. **Validate MVP**: Run `AudioPreprocessorTest` and `RewinderEmptyAudioTest`.

### Incremental Delivery
1. Deliver MVP (US1: Audio Pre-Processing & Empty Audio Skip).
2. Deliver US2 (Original Audio Preservation verification).
3. Deliver US3 (Complete Removal of `SensitiveFilter`).
4. Run regression verification (`T015`, `T016`).
