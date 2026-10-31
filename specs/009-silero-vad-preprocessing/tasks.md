# Tasks: Silero VAD Audio Pre-Processing

**Input**: Design documents from `specs/009-silero-vad-preprocessing/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/vad-detector-contract.kt](contracts/vad-detector-contract.kt)

## Format: `[ID] [P?] [Story] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to ([US1], [US2], [US3])
- Exact file paths included in all task descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependency verification and build configuration

- [x] T001 Verify ONNX Runtime dependency `libs.onnxruntime.android` in `androidApp/build.gradle.kts`
- [x] T002 [P] Configure Silero VAD model asset directory and build configuration in `androidApp/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core VAD abstractions and detector implementations required by all user stories

**⚠️ CRITICAL**: Foundational phase must be complete before User Story implementation begins

- [x] T003 Create `VadDetector` interface, `SileroVadConfig` data class, and `FallbackEnergyVadDetector` implementation in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/VadDetector.kt`
- [x] T004 [P] Implement `OnnxSileroVadDetector` wrapping `OrtSession` with recurrent state management and 512-sample chunking in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/OnnxSileroVadDetector.kt`

---

## Phase 3: User Story 1 - Voice-Retaining Rewind Audio Pre-Processing (Priority: P1) 🎯 MVP

**Goal**: Pre-process rewind audio using neural VAD to isolate human voice, remove non-speech ambient sounds (keystrokes, desk knocks), truncate dead air gaps exceeding 1,500 ms down to 300 ms padding, and normalize peak amplitude to `29,490` headroom.

**Independent Test**: Execute `SileroAudioPreprocessorTest` verifying that audio containing speech and non-speech sounds retains only voice passages with volume normalized to `29,490` headroom and routes clean speech to AI transcription.

### Tests for User Story 1

- [x] T005 [P] [US1] Write unit tests for neural VAD speech isolation, non-speech noise rejection, dead air truncation, and volume normalization in `androidApp/src/test/kotlin/two/knockledge/pipeline/SileroAudioPreprocessorTest.kt`

### Implementation for User Story 1

- [x] T006 [US1] Implement `SileroAudioPreprocessor` implementing `AudioPreprocessor` contract with 512-sample chunk evaluation, dead air truncation, and peak normalization targeting `29,490` headroom in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/SileroAudioPreprocessor.kt`
- [x] T007 [US1] Bind `SileroAudioPreprocessor` as the default `AudioPreprocessor` in Dagger DI module in `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- [x] T008 [P] [US1] Update `KnockledgeGraph.kt` preview and testing constructor defaults in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeGraph.kt`

**Checkpoint**: User Story 1 is functional and verifiable independently.

---

## Phase 4: User Story 2 - Original Audio File Integrity (Priority: P2)

**Goal**: Ensure that the user's saved note archive stores the authentic, unmodified recorded audio file without any pre-processing cuts or gain changes.

**Independent Test**: Record an audio session, trigger rewind pre-processing, and verify via unit tests that `NoteRepository` receives the unaltered WAV snapshot file with all original pauses and acoustics intact.

### Tests for User Story 2

- [x] T009 [P] [US2] Write unit test verifying that `RewindPipeline` and `KnockledgeController` preserve the original unmodified audio WAV snapshot in note storage during neural pre-processing in `androidApp/src/test/kotlin/two/knockledge/data/OriginalAudioPreservationTest.kt`

### Implementation for User Story 2

- [x] T010 [US2] Verify `SnapshotNoteSink` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt` stores the unaltered snapshot WAV file while passing conditioned PCM to AI models

**Checkpoint**: User Stories 1 and 2 operate reliably together without modifying archived user audio.

---

## Phase 5: User Story 3 - Insufficient Speech Handling (Priority: P3)

**Goal**: When detected voice content totals less than 1.0 second, abort AI transcription and summarization, skip note creation and cache saving, and inform the user.

**Independent Test**: Trigger rewind on audio with less than 1.0 second of speech or room silence; verify that `AudioPreprocessResult.InsufficientSpeech` halts the pipeline and notifies the user via the error channel without saving a note.

### Tests for User Story 3

- [x] T011 [P] [US3] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/pipeline/SileroAudioPreprocessorTest.kt` verifying that total speech duration < 1,000 ms returns `AudioPreprocessResult.InsufficientSpeech`
- [x] T012 [US3] Verify `RewindPipeline` handles `AudioPreprocessResult.InsufficientSpeech` by aborting transcription and emitting `PipelineResult.InsufficientSpeech` in `androidApp/src/test/kotlin/two/knockledge/pipeline/RewindPipelineTest.kt`

**Checkpoint**: All three user stories operate independently and handle boundary conditions.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Code documentation and end-to-end regression validation

- [x] T013 [P] Add KDoc documentation to `VadDetector.kt`, `OnnxSileroVadDetector.kt`, and `SileroAudioPreprocessor.kt` in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/`
- [x] T014 Execute full JVM unit test verification suite with `.\gradlew :androidApp:testDebugUnitTest`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2. Delivers working MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 2. Validates unedited audio preservation.
- **User Story 3 (Phase 5)**: Depends on Phase 3. Validates < 1.0s speech rejection.
- **Polish (Phase 6)**: Depends on all user stories being complete.

### Parallel Opportunities

- T001 and T002 can run in parallel.
- T003 and T004 can run in parallel once contract is drafted.
- T005, T008, and T009 can be developed in parallel across test and UI graph files.
- T011 and T013 can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (dependency checks).
2. Complete Phase 2: Foundational (`VadDetector` and `OnnxSileroVadDetector`).
3. Complete Phase 3: User Story 1 (`SileroAudioPreprocessor` and DI binding).
4. **STOP and VALIDATE**: Run `SileroAudioPreprocessorTest` to verify speech retention, non-speech rejection, and normalization.

### Incremental Delivery

1. Verify Foundational VAD abstractions.
2. Deliver User Story 1: Neural voice conditioning and dead air removal.
3. Deliver User Story 2: Verify authentic original audio file archiving.
4. Deliver User Story 3: Clean abort and user feedback on silent audio buffers.
5. Polish KDoc and run full test gate.
