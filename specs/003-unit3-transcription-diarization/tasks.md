---
description: "Task list for Unit 3 — Transcription & Diarization implementation"
---

# Tasks: Unit 3 — Transcription & Diarization

**Input**: Design documents from `specs/003-unit3-transcription-diarization/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)
**Tests**: Unit tests included per Constitution testing directives (`./gradlew test`).
**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story identifier (US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Validate dependencies and define shared data models.

- [x] T001 Verify Firebase AI Logic and ONNX Runtime dependencies in `androidApp/build.gradle.kts`
- [x] T002 [P] Define `AudioSegment`, `TranscriptSentence`, `TranscriptResult`, and `TranscriptionEngineType` models in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/TranscriptModels.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core interfaces and test scaffolding blocking all user stories.

**⚠️ CRITICAL**: Must complete before user story implementation begins.

- [x] T003 Define `Transcriber` and `TranscriptionEngine` interface contracts in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/TranscriberContracts.kt`
- [x] T004 [P] Create unit test fixture for transcription routing and timeout fallback in `androidApp/src/test/kotlin/two/knockledge/TranscriberTest.kt`

**Checkpoint**: Core contracts and test fixtures ready — user story implementation can begin.

---

## Phase 3: User Story 1 - Online Speech Transcription with Speaker Diarization (Priority: P1) 🎯 MVP

**Goal**: Ingest buffered PCM audio, auto-detect language (Taglish/English), and output speaker-separated transcripts via Firebase AI (Gemini 1.5 Flash).

**Independent Test**: Ingest a 30-second multi-speaker audio clip; verify `TranscriptResult` contains distinct speaker labels, language detection, and completes in under 15 seconds.

### Tests for User Story 1 ⚠️

- [x] T005 [P] [US1] Write failing unit test for Gemini structured transcription JSON response parsing in `androidApp/src/test/kotlin/two/knockledge/FirebaseAiTranscriberTest.kt`

### Implementation for User Story 1

- [x] T006 [US1] Implement `FirebaseAiTranscriber` using Firebase AI (Gemini 1.5 Flash) with audio blob payload and structured JSON output in `androidApp/src/main/kotlin/group/two/knockledge/ai/FirebaseAiTranscriber.kt`
- [x] T007 [US1] Implement `CompositeTranscriber` with 10-second timeout and immediate fallback routing in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/CompositeTranscriber.kt`
- [x] T008 [US1] Register `Transcriber` binding in Dagger dependency injection graph in `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`

**Checkpoint**: User Story 1 fully functional and independently testable as MVP.

---

## Phase 4: User Story 2 - Offline On-Device Speech Transcription (Priority: P2)

**Goal**: Ingest buffered PCM audio and produce a plain text transcript with generic `"Speaker"` label using on-device Whisper ONNX when offline.

**Independent Test**: Disable network connectivity; process 15-second voice clip; verify `TranscriptResult` contains plain text with `engineType = LOCAL_WHISPER` and `isDiarized = false`.

### Tests for User Story 2 ⚠️

- [x] T009 [P] [US2] Write failing unit test for `WhisperTranscriberAdapter` output mapping in `androidApp/src/test/kotlin/two/knockledge/WhisperTranscriberTest.kt`

### Implementation for User Story 2

- [x] T010 [US2] Implement `WhisperTranscriberAdapter` wrapping `AndroidSpeechEngine.transcribe` into `TranscriptResult` with single `"Speaker"` stream in `androidApp/src/main/kotlin/group/two/knockledge/ai/WhisperTranscriberAdapter.kt`
- [x] T011 [US2] Integrate offline network connectivity check into `CompositeTranscriber.kt` to route offline calls directly to `WhisperTranscriberAdapter`

**Checkpoint**: User Stories 1 and 2 operate independently with seamless online/offline switching.

---

## Phase 5: User Story 3 - Audio Conditioning and Voice Enhancement (Priority: P3)

**Goal**: Normalize low-amplitude microphone samples and filter non-speech silence before transcription.

**Independent Test**: Feed low-amplitude audio with background noise; verify gain normalizes without clipping and non-speech silence trims before dispatching to speech engine.

### Tests for User Story 3 ⚠️

- [x] T012 [P] [US3] Write unit test for `AudioConditioner` gain scaling and silence trimming in `androidApp/src/test/kotlin/two/knockledge/AudioConditionerTest.kt`

### Implementation for User Story 3

- [x] T013 [US3] Implement `AudioConditioner` wrapping `AudioGain.normalize` and energy-based silence trimming in `androidApp/src/main/kotlin/group/two/knockledge/audio/AudioConditioner.kt`
- [x] T014 [US3] Connect `AudioConditioner` into `CompositeTranscriber.transcribe` prior to engine execution in `androidApp/src/main/kotlin/group/two/knockledge/pipeline/CompositeTranscriber.kt`

**Checkpoint**: Audio conditioning pipeline active across all transcription routes.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification, quality gates, and end-to-end checks.

- [x] T015 [P] Run full unit test suite via `./gradlew test` to verify zero regressions
- [ ] T016 Execute quickstart scenarios in `specs/003-unit3-transcription-diarization/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2 — deliverable as MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and integrates with `CompositeTranscriber` from US1.
- **User Story 3 (Phase 5)**: Depends on Phase 2; enhances input to `CompositeTranscriber`.
- **Polish (Phase 6)**: Depends on completing desired user stories.

### Parallel Opportunities

- T001 and T002 can run in parallel.
- T004 (test scaffold) and T003 (interfaces) can run in parallel.
- T005, T009, and T012 (story test definitions) can run in parallel once Phase 2 completes.
- T006 (`FirebaseAiTranscriber`) and T010 (`WhisperTranscriberAdapter`) can run in parallel.

---

## Parallel Example: User Story 1

```bash
# Launch test and model creation in parallel:
Task: "Write failing unit test for Gemini structured transcription in androidApp/src/test/kotlin/two/knockledge/FirebaseAiTranscriberTest.kt"
Task: "Define models in androidApp/src/main/kotlin/group/two/knockledge/pipeline/TranscriptModels.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001, T002)
2. Complete Phase 2: Foundational (T003, T004)
3. Complete Phase 3: User Story 1 (T005, T006, T007, T008)
4. **STOP and VALIDATE**: Verify online transcription with speaker diarization on sample audio.
5. Deliver MVP.

### Incremental Delivery

1. Setup + Foundational → Contracts and models ready.
2. User Story 1 → Online Firebase AI transcription with diarization (MVP).
3. User Story 2 → Offline on-device Whisper fallback (resilience).
4. User Story 3 → Audio conditioning with `AudioGain` (quality).
5. Polish → Verification through `./gradlew test`.
