# Tasks: Unit 4 — Knock Gesture & Recording

**Input**: Design documents from `/specs/004-unit4-knock-recording/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)

## Format: `[ID] [P?] [Story] Description with exact file path`

- **[P]**: Can run in parallel (different files, no mutual dependencies)
- **[Story]**: User story identifier ([US1], [US2], [US3])
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize directory structure and configuration constants for local storage caching.

- [x] T001 Setup cache directory creation helper for rolling audio buffer in `androidApp/src/main/kotlin/group/two/knockledge/core/AudioStorageUtils.kt`
- [x] T002 [P] Update `RewindPolicy.kt` with configurable window limits (`60 <= durationSeconds <= 300`, default `300`) in `androidApp/src/main/kotlin/group/two/knockledge/core/RewindPolicy.kt`
- [x] T003 [P] Add buffer duration preferences key and flow (`buffer_duration_seconds`) to `AppSettings.kt` in `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core interfaces, models, and dependency injection modules required before story implementation.

- [x] T004 Create domain entities (`BufferConfiguration`, `KnockSensorSample`, `KnockGestureEvent`, `AudioSnapshot`) in `androidApp/src/main/kotlin/group/two/knockledge/data/KnockModels.kt`
- [x] T005 [P] Define `LocalAudioRingBuffer` interface per contract in `androidApp/src/main/kotlin/group/two/knockledge/core/LocalAudioRingBuffer.kt`
- [x] T006 [P] Define `KnockGestureDetector` interface per contract in `androidApp/src/main/kotlin/group/two/knockledge/sensor/KnockGestureDetector.kt`
- [x] T007 [P] Define `KnockRecordingUiState` and `KnockRecordingController` interfaces in `androidApp/src/main/kotlin/group/two/knockledge/ui/KnockRecordingController.kt`
- [x] T008 [P] Create Dagger DI module `KnockRecordingModule` with `@Binds` bindings in `androidApp/src/main/kotlin/group/two/knockledge/di/KnockRecordingModule.kt`

---

## Phase 3: User Story 2 - User-Configurable Rolling Audio Cache Buffer in Local Storage (Priority: P1)

**Goal**: Record audio continuously into local disk chunks (5-second PCM segments), evict chunks exceeding the configured duration (up to 5 minutes), and export snapshots on demand.
**Independent Test**: Configure a 60-second window, stream 180 seconds of audio samples, and verify that disk usage remains bounded by 12 chunks (60 seconds) with older chunks removed automatically.

### Tests for User Story 2
- [x] T009 [P] [US2] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/LocalAudioRingBufferTest.kt` verifying 5-second chunk segmentation, eviction when exceeding `durationSeconds`, and snapshot audio file concatenation.

### Implementation for User Story 2
- [x] T010 [US2] Implement `FileLocalAudioRingBuffer` managing 5-second chunk files in `cache/audio_buffer/`, pruning files beyond `durationSeconds`, and exporting concatenated audio in `androidApp/src/main/kotlin/group/two/knockledge/core/FileLocalAudioRingBuffer.kt`
- [x] T011 [US2] Update `KnockledgeController.kt` to replace in-memory `AudioRingBuffer` with `FileLocalAudioRingBuffer` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`
- [x] T012 [US2] Connect `AppSettings.bufferDuration` updates to dynamic `setWindowSeconds()` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`

**Checkpoint**: User Story 2 complete. Continuous audio records directly to disk cache and evicts old data without RAM leakage.

---

## Phase 4: User Story 1 - Physical Knock Gesture Capture (Priority: P1)

**Goal**: Detect double-knock gestures using `Sensor.TYPE_LINEAR_ACCELERATION` (with fallback), trigger haptic feedback, and snapshot the preceding audio buffer within 150 ms.
**Independent Test**: Feed simulated linear acceleration sensor samples to verify that peaks exceeding `12.0 m/s^2` spaced between `100 ms` and `400 ms` trigger a save event, while single peaks or 2000 ms debounce periods do not re-trigger.

### Tests for User Story 1
- [x] T013 [P] [US1] Write unit tests in `androidApp/src/test/kotlin/two/knockledge/KnockGestureDetectorTest.kt` simulating acceleration time-series for peak threshold (`12.0 m/s^2`), timing window (`100 ms <= delta <= 400 ms`), and 2000 ms debounce rejection.

### Implementation for User Story 1
- [x] T014 [US1] Implement `AndroidKnockGestureDetector` using `Sensor.TYPE_LINEAR_ACCELERATION` (with accelerometer gravity filtering fallback) and 50 ms vibration pulse in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- [x] T015 [US1] Wire `KnockGestureDetector` into `KnockledgeService` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt` to start listening when recording begins and trigger `rewind()` snapshot on knock detection

**Checkpoint**: User Story 1 complete. Double knocks trigger haptic vibration and save preceding audio from the local cache.

---

## Phase 5: User Story 3 - Manual Save Fallback Trigger (Priority: P2)

**Goal**: Provide an on-screen manual save button and buffer duration selector in Jetpack Compose UI with unidirectional StateFlow state.
**Independent Test**: Render Compose UI with preview, simulate tapping the manual save button, and verify that `KnockRecordingUiState` updates to saving snapshot and invokes controller snapshot export.

### Tests for User Story 3
- [x] T016 [P] [US3] Write unit test in `androidApp/src/test/kotlin/two/knockledge/KnockRecordingViewModelTest.kt` verifying manual trigger action and `StateFlow<KnockRecordingUiState>` state transitions.

### Implementation for User Story 3
- [x] T017 [US3] Implement `KnockRecordingViewModel` exposing immutable `StateFlow<KnockRecordingUiState>` and `triggerManualSave()` in `androidApp/src/main/kotlin/group/two/knockledge/ui/KnockRecordingViewModel.kt`
- [x] T018 [US3] Build Compose manual trigger button and buffer duration slider with `@Preview` functions in `androidApp/src/main/kotlin/group/two/knockledge/ui/RecordingControls.kt`

**Checkpoint**: All user stories complete and testable independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end verification and quality gate compliance.

- [ ] T019 [P] Execute `./gradlew :androidApp:testDebugUnitTest` to verify all unit tests pass cleanly
- [ ] T020 [P] Render and inspect Compose `@Preview` in `androidApp/src/main/kotlin/group/two/knockledge/ui/RecordingControls.kt` per constitution guidelines
- [ ] T021 Execute full quickstart validation scenarios defined in `specs/004-unit4-knock-recording/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: No dependencies — starts immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — blocks all user stories.
- **User Story 2 (Phase 3)**: Depends on Foundational completion. Implements local storage cache.
- **User Story 1 (Phase 4)**: Depends on Phase 3 completion (requires working local storage buffer to snapshot).
- **User Story 3 (Phase 5)**: Depends on Phase 2 completion. Can run in parallel with Phase 4.
- **Polish (Phase 6)**: Depends on all user stories being complete.

### Parallel Opportunities
- In Phase 1: `T002` and `T003` can run in parallel.
- In Phase 2: `T005`, `T006`, `T007`, and `T008` can run in parallel.
- In Phase 3: `T009` test task can run in parallel with other test writing.
- In Phase 4: `T013` test task can run in parallel.
- In Phase 5: `T016` test task can run in parallel.
- In Phase 6: `T019` and `T020` can run in parallel.

---

## Implementation Strategy

### MVP Scope (User Story 1 & User Story 2)
1. Complete Phase 1: Setup (`T001`-`T003`).
2. Complete Phase 2: Foundational (`T004`-`T008`).
3. Complete Phase 3: User Story 2 local disk buffer (`T009`-`T012`).
4. Complete Phase 4: User Story 1 knock gesture capture (`T013`-`T015`).
5. **STOP and VALIDATE**: Verify that double knocking saves preceding disk-buffered audio and vibrates the phone.
