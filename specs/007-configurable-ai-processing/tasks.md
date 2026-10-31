---
description: "Task list for configurable AI processing and manual trigger implementation"
---

# Tasks: Configurable AI Processing and Manual Trigger

**Input**: Design documents from `specs/007-configurable-ai-processing/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and contract definitions

- [X] T001 [P] Create note processing state contract and UI state models in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/NoteProcessingController.kt`
- [X] T002 [P] Define UI string resources for AI processing states, toggles, and errors in `androidApp/src/main/res/values/strings.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Settings persistence and audio-only snapshot persistence required across all user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 [P] Add unit tests for `AppSettings.autoAiProcessingEnabled` default (`false`) and persistence in `androidApp/src/test/kotlin/two/knockledge/AppSettingsTest.kt`
- [X] T004 Add `autoAiProcessingEnabled` StateFlow and `setAutoAiProcessingEnabled()` to `AppSettings` in `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`
- [X] T005 [P] Add unit test for `recordAudioOnly` in `androidApp/src/test/kotlin/two/knockledge/data/SnapshotNoteSinkTest.kt`
- [X] T006 Implement `recordAudioOnly` in `SnapshotNoteSink` to persist audio without invoking transcript or summary in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/SnapshotNoteSink.kt`

**Checkpoint**: Foundation ready - settings and decoupled audio storage operational

---

## Phase 3: User Story 1 - Default Manual AI Processing on Demand (Priority: P1) 🎯 MVP

**Goal**: Audio buffer capture persists audio immediately without executing AI pipeline when auto AI processing is disabled. Users can trigger transcription and summarization on demand from the UI.

**Independent Test**: Save an audio snapshot with default settings. Verify note has audio metadata and null transcript/summary. Open the note, trigger processing, and verify transcript and summary attach.

### Tests for User Story 1

- [X] T007 [P] [US1] Write unit test verifying `KnockledgeController.rewind()` saves audio-only note when `autoAiProcessingEnabled` is false in `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerAutoAiTest.kt`
- [X] T008 [P] [US1] Write unit test for `NoteProcessingViewModel` verifying transitions through `Transcribing` and `Summarizing` stages in `androidApp/src/test/kotlin/two/knockledge/NoteProcessingViewModelTest.kt`

### Implementation for User Story 1

- [X] T009 [US1] Update `KnockledgeController.rewind()` to evaluate `settings.autoAiProcessingEnabled.value`, executing `recordAudioOnly` when false in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`
- [X] T010 [US1] Implement `NoteProcessingViewModel` to load note audio, run `RewindPipeline.run()`, and attach transcript and summary via `NoteRepository` in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/NoteProcessingViewModel.kt`
- [X] T011 [P] [US1] Implement stateless `NoteProcessingControls` composable with `@Preview` showing IDLE, processing progress, and completion in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/NoteProcessingControls.kt`

**Checkpoint**: User Story 1 complete and independently testable as MVP

---

## Phase 4: User Story 2 - User-Configurable Automatic Processing Setting (Priority: P2)

**Goal**: Users can enable automatic AI processing in application settings so that subsequent audio captures transcribe and summarize immediately.

**Independent Test**: Enable automatic AI processing in settings, capture audio, and verify transcript and summary generate automatically upon save.

### Tests for User Story 2

- [X] T012 [P] [US2] Write unit test verifying `KnockledgeController.rewind()` executes `RewindPipeline` when `autoAiProcessingEnabled` is true in `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerAutoAiTest.kt`

### Implementation for User Story 2

- [X] T013 [US2] Add automatic AI processing toggle property and handler in `androidApp/src/main/kotlin/group/two/knockledge/ai/AiControls.kt`
- [X] T014 [US2] Add toggle switch for automatic AI processing with stateless `@Preview` in `androidApp/src/main/kotlin/group/two/knockledge/ui/settings/AiProcessingSettingsControl.kt`

**Checkpoint**: User Stories 1 and 2 functional independently

---

## Phase 5: User Story 3 - Resilient Processing State and Error Handling (Priority: P3)

**Goal**: Prevent duplicate concurrent processing jobs and provide failure reporting with retry capability in the UI.

**Independent Test**: Trigger note processing during an error condition, verify error message and retry button in UI, tap retry and verify processing restarts.

### Tests for User Story 3

- [X] T015 [P] [US3] Write unit test in `androidApp/src/test/kotlin/two/knockledge/NoteProcessingViewModelTest.kt` verifying deduplication and error handling

### Implementation for User Story 3

- [X] T016 [US3] Add deduplication guard and catch blocks in `NoteProcessingViewModel` in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/NoteProcessingViewModel.kt`
- [X] T017 [US3] Add error banner and retry button to `NoteProcessingControls` in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/NoteProcessingControls.kt`

**Checkpoint**: All user stories functional with full error recovery

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Code quality, documentation, and regression verification

- [X] T018 [P] Add KDoc comments for all public classes and methods in `androidApp/src/main/kotlin/group/two/knockledge/ui/processing/`
- [X] T019 Run unit test suite `./gradlew testDebugUnitTest` and compile check `./gradlew compileDebugKotlin` to verify all tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Can start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1. Blocks all User Stories.
- **Phase 3 (User Story 1 - MVP)**: Depends on Phase 2. Delivers core manual AI trigger and audio-only saving.
- **Phase 4 (User Story 2)**: Depends on Phase 2 and Phase 3. Adds settings toggle.
- **Phase 5 (User Story 3)**: Depends on Phase 3. Adds error handling and retry UI.
- **Phase 6 (Polish)**: Depends on all user stories being complete.

### Parallel Opportunities

- T001 and T002 can run in parallel.
- T003 and T005 can run in parallel.
- T007 and T008 can run in parallel.
- T011 can run in parallel with T009/T010.
- T012 can run in parallel with T015.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational).
2. Implement Phase 3 (User Story 1).
3. Validate User Story 1 independently with `./gradlew testDebugUnitTest --tests "two.knockledge.KnockledgeControllerAutoAiTest"`.
4. Deploy / demo the MVP.

### Incremental Delivery

1. Setup + Foundational -> Audio-only saving and settings persistence ready.
2. User Story 1 -> Default manual trigger functional (MVP).
3. User Story 2 -> Settings toggle allows users to re-enable auto processing.
4. User Story 3 -> Resilient error states and retry buttons in UI.
5. Polish -> Documentation and full verification.
