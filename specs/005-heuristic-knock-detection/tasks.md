# Tasks: Heuristic Knock Detection

**Input**: Design documents from `specs/005-heuristic-knock-detection/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`  
**Tests**: Fast JVM unit tests required by Karpathy discipline and constitution testing directives.  
**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish base contracts and diagnostic interfaces.

- [X] T001 Verify sensor component packaging and structure in `androidApp/src/main/kotlin/group/two/knockledge/sensor/`
- [X] T002 [P] Implement `HeuristicEvaluationListener` and `RejectionReason` in `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationListener.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Ensure core mathematical filter primitives operate cleanly without TensorFlow Lite or gyroscope dependencies.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Verify `Slope3C.kt`, `Lowpass1C.kt`, `Highpass1C.kt`, and `PeakDetector.kt` math in `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/`
- [X] T004 Decouple heuristic processing in `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt` so `isHeuristic = true` executes without initializing `TfClassifier`

**Checkpoint**: Foundation ready - user story implementation can begin.

---

## Phase 3: User Story 1 - Physical Double Knock Audio Capture (Priority: P1) 🎯 MVP

**Goal**: Detect physical double knocks via accelerometer jerk and rebound peak tracking to trigger audio capture.  
**Independent Test**: Execute `.\gradlew test --tests "two.knockledge.HeuristicKnockGestureDetectorTest"` with synthetic double knocks separated by 250ms to verify trigger emission.

### Tests for User Story 1

- [X] T005 [P] [US1] Create unit test suite `HeuristicKnockGestureDetectorTest` in `androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt`

### Implementation for User Story 1

- [X] T006 [US1] Refactor `AndroidKnockGestureDetector` in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt` to run heuristic mode (`isHeuristic = true`) using only `Sensor.TYPE_ACCELEROMETER`
- [X] T007 [US1] Wire double-knock cadence timing ($100\text{ ms} \le \Delta t \le 500\text{ ms}$) and haptic confirmation in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`

**Checkpoint**: User Story 1 functional and verifiable independently.

---

## Phase 4: User Story 2 - Motion Noise and False Trigger Suppression (Priority: P1)

**Goal**: Suppress low-frequency handling motion, walking vibration, and rapid chatter (< 100ms).  
**Independent Test**: Feed continuous 2 Hz sinusoidal motion samples to detector in unit test; verify zero double-knock detections occur.

### Tests for User Story 2

- [X] T008 [P] [US2] Add noise suppression and debounce unit tests in `androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt`

### Implementation for User Story 2

- [X] T009 [US2] Tune peak noise tolerance (`setMinNoiseTolerate(0.02f)`) and window size (8) in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- [X] T010 [US2] Enforce debounce threshold for rapid chatter (< 100ms) in `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt`

**Checkpoint**: User Stories 1 and 2 work reliably together.

---

## Phase 5: User Story 3 - Accuracy Evaluation and Model Escalation Gate (Priority: P2)

**Goal**: Expose evaluation telemetry to measure the 85% true positive rate before deciding on machine learning fallback.  
**Independent Test**: Run 20 synthetic knock trials through evaluation listener; verify true positive and false positive counters match expectations.

### Tests & Implementation for User Story 3

- [X] T011 [P] [US3] Create evaluation metrics collector in `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationMetrics.kt`
- [X] T012 [US3] Connect evaluation listener hooks in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- [X] T013 [P] [US3] Add 20-trial evaluation benchmark test in `androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt`

**Checkpoint**: Telemetry enables clear decision on heuristic satisfaction vs ML escalation.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, cleanup, and battery lifecycle safety.

- [X] T014 Run full JVM test suite via `.\gradlew test` to ensure 0 regressions
- [X] T015 Verify clean sensor unregistration in `stopListening()` to prevent battery drain in `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- [X] T016 Validate end-to-end integration against `specs/005-heuristic-knock-detection/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 - BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2 - Delivers MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 3 completion.
- **User Story 3 (Phase 5)**: Depends on Phase 4 completion.
- **Polish (Phase 6)**: Depends on all user stories completing.

### Parallel Opportunities

- T001 and T002 can execute in parallel.
- T003 and T004 can execute in parallel.
- T005, T008, and T011 can be authored in parallel.

---

## Parallel Example: User Story 1

```bash
# Launch test task:
Task: "T005 [P] [US1] Create unit test suite HeuristicKnockGestureDetectorTest in androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt"

# Launch implementation:
Task: "T006 [US1] Refactor AndroidKnockGestureDetector in androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (decouple TFLite & gyro requirements)
3. Complete Phase 3: User Story 1 (jerk + dual peak detection + double-knock trigger)
4. **STOP and VALIDATE**: Run JVM test `HeuristicKnockGestureDetectorTest`
5. Verify physical double-knock trigger on device

### Incremental Delivery

1. Deliver MVP (US1) -> Basic double knock working.
2. Deliver US2 -> Motion noise and debounce protection active.
3. Deliver US3 -> Telemetry verification to decide if ML model is required.
