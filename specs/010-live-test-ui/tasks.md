# Tasks: Utilitarian Material 3 Live Test UI

**Input**: Design documents from `specs/010-live-test-ui/` (`plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/live-ui-contracts.kt`, `quickstart.md`)

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/live-ui-contracts.kt`

**Organization**: Tasks are grouped by phase and user story to enable independent implementation and testing of each slice.

## Format: `- [X] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no blocking dependencies)
- **[Story]**: Which user story this task belongs to (e.g. `[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- Every task includes an explicit target file path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: UI string resources and dependency injection wiring.

- [X] T001 Define Android UI string resources for live testing, tabs, knock feedback, audio playback, and settings in `androidApp/src/main/res/values/strings.xml`
- [X] T002 [P] Expose `audioPlayer(): AudioPlayer`, `pipeline(): RewindPipeline`, and `appSettings(): AppSettings` on `KnockledgeComponent` in `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core UI coordinator, state management, and shell navigation scaffolding that all tabs depend on.

- [X] T003 [P] Implement `LiveTestTab` enum (`RECORD`, `NOTES`, `SETTINGS`), `LiveTestUiState`, and `LiveTestController` state coordinator in `androidApp/src/main/kotlin/group/two/knockledge/ui/LiveTestController.kt`
- [X] T004 [P] Write unit tests for `LiveTestCoordinator` verifying tab switching, knock pulse expiration, and note selection in `androidApp/src/test/kotlin/two/knockledge/ui/LiveTestCoordinatorTest.kt`
- [X] T005 Create the utilitarian top-level `LiveTestScreen` shell with Material 3 `Scaffold` and `TabRow` navigation in `androidApp/src/main/kotlin/group/two/knockledge/ui/LiveTestScreen.kt`

---

## Phase 3: User Story 1 - Live Audio Recording and Knock Capture (Priority: P1) 🎯 MVP

**Goal**: Continuous circular audio buffer monitoring, start/stop recording, manual snapshot save, and live accelerometer knock detection feedback with visual pulse.

**Independent Test**: Start session, speak into microphone to increment buffer, tap knock or simulate knock, and verify immediate "KNOCK DETECTED!" visual feedback and snapshot save.

- [X] T006 [US1] Build `RecordTab` composable integrating `RecordingControls`, buffer window slider, and real-time knock event badge in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/RecordTab.kt`
- [X] T007 [US1] Add hardware knock listener wiring and manual knock simulation trigger button in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/RecordTab.kt`
- [X] T008 [US1] Integrate `RecordTab` into `LiveTestScreen` with `KnockRecordingViewModel` in `androidApp/src/main/kotlin/group/two/knockledge/ui/LiveTestScreen.kt`

**Checkpoint**: User Story 1 complete and independently testable on physical hardware and emulator.

---

## Phase 4: User Story 2 - Note History and Audio Playback (Priority: P2)

**Goal**: Reactive list of captured notes from `NoteRepository`, note selection, and utilitarian audio playback with seekbar and chapter selection.

**Independent Test**: Select a captured note from the list, press play, scrub time slider, tap chapter marker, and verify audible playback and timestamp seeking.

- [X] T009 [P] [US2] Create `NoteListItem` composable and notes list column in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/NotesTab.kt` observing `NoteRepository.observeNotes(userId)`
- [X] T010 [US2] Integrate `AudioPlaybackControls` with `NotePlaybackViewModel` for the selected note in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/NotesTab.kt`
- [X] T011 [US2] Add empty state message and note deletion handler in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/NotesTab.kt`

**Checkpoint**: User Stories 1 and 2 work independently. Captured notes can be reviewed and played immediately.

---

## Phase 5: User Story 3 - AI Note Details and Processing Inspection (Priority: P3)

**Goal**: Display transcript text, executive summary, bullet points, and provide on-demand AI processing controls for unprocessed notes.

**Independent Test**: Select an unprocessed note, tap "Run AI Processing", verify progress stage indicators, and observe resulting transcript and summary.

- [X] T012 [P] [US3] Build note AI intelligence details view in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/NotesTab.kt` rendering transcript sentences, summary gist, and key bullet points
- [X] T013 [US3] Integrate `NoteProcessingControls` and wire `NoteProcessingViewModel.processNote(noteId)` with error retry in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/NotesTab.kt`

**Checkpoint**: User Stories 1, 2, and 3 work independently. Note AI pipeline outputs are fully verifiable.

---

## Phase 6: User Story 4 - User Authentication and Profile Status (Priority: P4)

**Goal**: View active user authentication state, switch between authenticated user and guest mode, and allow sign-in/registration/sign-out for cloud sync testing.

**Independent Test**: View user card on settings tab, perform sign-in or sign-out, and verify reactive profile status update without data loss.

- [X] T014 [P] [US4] Build utilitarian account status card with sign-in, registration, and sign-out actions in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/SettingsTab.kt` wired to `AuthViewModel`
- [X] T015 [US4] Connect authenticated user flow to `NoteRepository` note observation across user switching in `androidApp/src/main/kotlin/group/two/knockledge/ui/LiveTestScreen.kt`

**Checkpoint**: User Stories 1 through 4 work independently. Authentication and note isolation verified.

---

## Phase 7: User Story 5 - Unified Navigation and Settings (Priority: P5)

**Goal**: Connect all runtime settings (auto-AI toggle, buffer window, sensor threshold) and wire `MainActivity` as the single executable test entrypoint.

**Independent Test**: Toggle auto-AI processing, navigate between tabs with single taps, and verify settings persist and govern recording behavior.

- [X] T016 [P] [US5] Implement settings controls in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/SettingsTab.kt` integrating `AiProcessingSettingsControl`, buffer duration slider, and knock sensor threshold slider with `SettingsViewModel`
- [X] T017 [US5] Update `MainActivity` in `androidApp/src/main/kotlin/group/two/knockledge/MainActivity.kt` to initialize `KnockledgeComponent` dependencies, request `RECORD_AUDIO` permissions, and host `LiveTestScreen`

**Checkpoint**: All 5 user stories are completely functional and unified in `MainActivity`.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Previews, compiler checks, and end-to-end test verification.

- [X] T018 [P] Add stateless `@Preview` composables wrapped in `KnockledgeTheme` for `RecordTab`, `NotesTab`, and `SettingsTab` in `androidApp/src/main/kotlin/group/two/knockledge/ui/tabs/`
- [X] T019 Run JVM test suite (`./gradlew test`) and verify zero regressions across all unit and viewmodel tests
- [X] T020 Execute end-to-end verification against `specs/010-live-test-ui/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user story tabs.
- **User Story 1 (Phase 3)**: Depends on Phase 2 — Deliver MVP recording & knock detection.
- **User Story 2 (Phase 4)**: Depends on Phase 2 — Deliver note history & audio playback.
- **User Story 3 (Phase 5)**: Depends on Phase 4 — Deliver transcript & summary details on selected notes.
- **User Story 4 (Phase 6)**: Depends on Phase 2 — Deliver auth card on settings tab.
- **User Story 5 (Phase 7)**: Depends on Phases 3, 4, 5, 6 — Connect MainActivity host & settings controls.
- **Polish (Phase 8)**: Depends on all user story phases complete.

---

## Parallel Opportunities

- **Phase 1**: T001 (`strings.xml`) and T002 (`KnockledgeComponent.kt`) can run in parallel.
- **Phase 2**: T003 (`LiveTestController.kt`) and T004 (`LiveTestCoordinatorTest.kt`) can run in parallel.
- **Phase 4 & Phase 6**: Notes list development (T009) and Account status card (T014) can run in parallel once Phase 2 is complete.
- **Phase 8**: Preview composables (T018) can be authored in parallel with test verification runs.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001, T002)
2. Complete Phase 2: Foundational (T003, T004, T005)
3. Complete Phase 3: User Story 1 (T006, T007, T008)
4. **STOP and VALIDATE**: Verify live circular recording and physical knock detection on device.

### Incremental Delivery

1. Setup + Foundational → Navigation shell ready.
2. User Story 1 → Live recording and knock capture working (MVP).
3. User Story 2 → Note history and audio playback working.
4. User Story 3 → AI transcription and summarization inspection working.
5. User Story 4 → User authentication and cloud sync switching working.
6. User Story 5 → Utilitarian settings controls and MainActivity host fully wired.
7. Polish → Previews and test suite clean.
