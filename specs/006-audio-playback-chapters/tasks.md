# Tasks: Audio Playback with Interactive Chapters

**Input**: Design documents from `/specs/006-audio-playback-chapters/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Every task includes exact file paths

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 [P] Create directory structure for audio playback and UI in `androidApp/src/main/kotlin/group/two/knockledge/audio/` and `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/`
- [x] T002 [P] Define UI string resources for playback controls in `androidApp/src/main/res/values/strings.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core interfaces and DI wiring that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Define `AudioPlaybackState` data class and `AudioPlayer` interface in `androidApp/src/main/kotlin/group/two/knockledge/audio/AudioPlayer.kt`
- [x] T004 [P] Define `NotePlaybackController` interface in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/NotePlaybackController.kt`
- [x] T005 Provide Dagger DI binding for `AudioPlayer` in `androidApp/src/main/kotlin/group/two/knockledge/di/AudioModule.kt`

**Checkpoint**: Core playback interfaces ready - user story implementation can begin

---

## Phase 3: User Story 1 - Audio Playback for Saved Notes (Priority: P1) 🎯 MVP

**Goal**: Enable users to listen to saved audio recordings with play, pause, progress slider, and duration display.

**Independent Test**: Load a note with local WAV audio, tap play, verify audio plays, verify pause stops audio, and verify seeking changes position.

### Tests for User Story 1 ⚠️

- [x] T006 [P] [US1] Write unit test for `AndroidAudioPlayer` local WAV file playback and 100 ms position polling in `androidApp/src/test/kotlin/two/knockledge/audio/AndroidAudioPlayerTest.kt`

### Implementation for User Story 1

- [x] T007 [US1] Implement `AndroidAudioPlayer` wrapping native `android.media.MediaPlayer` with coroutine position ticker in `androidApp/src/main/kotlin/group/two/knockledge/audio/AndroidAudioPlayer.kt`
- [x] T008 [US1] Implement `NotePlaybackViewModel` managing audio playback state and controls in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/NotePlaybackViewModel.kt`
- [x] T009 [US1] Implement basic `AudioPlaybackControls` composable with play, pause, seekbar, and duration display in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/AudioPlaybackControls.kt`

**Checkpoint**: User Story 1 is functional and testable independently as the MVP

---

## Phase 4: User Story 2 - Interactive Chapter Navigation in Playback (Priority: P2)

**Goal**: Render YouTube-style chapter divisions on the progress seekbar, list chapters with titles and timestamps, and support direct seeking to chapter start times.

**Independent Test**: Load a note with three chapter markers. Verify that the progress bar displays chapter divisions, tapping a chapter jumps playback to its `startTimeMs`, and the active chapter highlights automatically.

### Tests for User Story 2 ⚠️

- [x] T010 [P] [US2] Write unit test for chapter seeking and active chapter calculation where `activeChapter = max startTimeMs <= currentPositionMs` in `androidApp/src/test/kotlin/two/knockledge/audio/ChapterNavigationTest.kt`

### Implementation for User Story 2

- [x] T011 [US2] Add chapter tracking and reactive `activeChapter` calculation to `AndroidAudioPlayer` in `androidApp/src/main/kotlin/group/two/knockledge/audio/AndroidAudioPlayer.kt`
- [x] T012 [US2] Add `selectChapter(chapter: ChapterMarker)` forwarding logic to `NotePlaybackViewModel` in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/NotePlaybackViewModel.kt`
- [x] T013 [US2] Implement visual chapter divisions on the progress slider and selectable chapter list in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/AudioPlaybackControls.kt`
- [x] T014 [P] [US2] Add stateless Compose `@Preview` functions for chapter playback controls in `androidApp/src/main/kotlin/group/two/knockledge/ui/playback/AudioPlaybackControls.kt`

**Checkpoint**: User Stories 1 and 2 work independently with full interactive chapter seeking

---

## Phase 5: User Story 3 - Automated Chapter Marker Generation (Priority: P3)

**Goal**: Generate timestamped chapter markers during AI note summarization and persist them in Room and Firestore.

**Independent Test**: Provide an audio transcript longer than 60 seconds. Trigger summary generation and verify that output contains timestamped chapter markers.

### Tests for User Story 3 ⚠️

- [x] T015 [P] [US3] Write unit test for AI chapter marker generation in `androidApp/src/test/kotlin/two/knockledge/FirebaseAiTranscriberTest.kt`

### Implementation for User Story 3

- [x] T016 [US3] Update prompt and response schema in `FirebaseAiTranscriber` to generate timestamped chapter markers in `androidApp/src/main/kotlin/group/two/knockledge/ai/FirebaseAiTranscriber.kt`
- [x] T017 [US3] Update `SnapshotNoteSink` to forward generated chapter markers to `NoteRepository.attachSummary()` in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/SnapshotNoteSink.kt`

**Checkpoint**: All three user stories work together end-to-end

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Audio focus handling, validation, and test suite execution

- [x] T018 [P] Implement `AudioManager.OnAudioFocusChangeListener` to handle phone calls and interruptions in `androidApp/src/main/kotlin/group/two/knockledge/audio/AndroidAudioPlayer.kt`
- [x] T019 [P] Execute all JVM unit tests via `./gradlew testDebugUnitTest`
- [x] T020 Run end-to-end verification scenarios per `specs/006-audio-playback-chapters/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion
- **User Story 2 (Phase 4)**: Depends on Foundational phase and US1 player engine
- **User Story 3 (Phase 5)**: Depends on Foundational phase (independent of UI playback, can run in parallel)
- **Polish (Phase 6)**: Depends on US1 and US2 completion

### User Story Dependencies

- **User Story 1 (P1)**: Independent MVP slice.
- **User Story 2 (P2)**: Extends US1 with chapter markers on progress bar and chapter list.
- **User Story 3 (P3)**: Generates chapter markers in the AI pipeline; independent of UI playback controls.

### Parallel Opportunities

- T001 and T002 can run in parallel during Setup.
- T003 and T004 can run in parallel during Foundational.
- T006 and T010 tests can run in parallel.
- User Story 3 (AI chapter generation) can proceed in parallel with User Story 2 (UI chapter controls).

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001 - T002)
2. Complete Phase 2: Foundational (T003 - T005)
3. Complete Phase 3: User Story 1 (T006 - T009)
4. **STOP and VALIDATE**: Test User Story 1 independently with local WAV playback.

### Incremental Delivery

1. Deliver US1: Basic audio playback with play/pause and continuous slider.
2. Deliver US2: YouTube-style chapter divisions and chapter list tap-to-seek.
3. Deliver US3: AI cloud generation of structured chapter markers.
