# Implementation Plan: Audio Playback with Interactive Chapters

**Branch**: `006-audio-playback-chapters` | **Date**: 2026-10-31 | **Spec**: [specs/006-audio-playback-chapters/spec.md](spec.md)

**Input**: Feature specification from `/specs/006-audio-playback-chapters/spec.md`

## Summary

Implement native audio playback for saved note WAV files using Android `MediaPlayer`, with interactive YouTube-style chapter scrubbing and real-time active chapter tracking. Connect chapter generation from the AI summarization pipeline to persist structured chapter markers in Room and Firestore.

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (JVM target 17)

**Primary Dependencies**: Android SDK 35 (API 29+), Jetpack Compose (Material3), Kotlinx Coroutines (`StateFlow`), Room 2.6.1, Dagger 2.51.1

**Storage**: Local app private files ([`AudioFileStorage`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/local/AudioFileStorage.kt#L7-L10)), Room Database ([`NoteEntity`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/local/NoteEntity.kt#L10-L30)), Cloud Firestore

**Testing**: JUnit 4, Kotlinx Coroutines Test, MockK, JVM Unit Tests (`./gradlew test`)

**Target Platform**: Android 10+ (API 29 through API 35)

**Project Type**: Native Android mobile application

**Performance Goals**: Playback start < 200 ms, seek response < 100 ms, progress UI updates at 10 Hz (every 100 ms)

**Constraints**: Zero new external dependencies (Ponytail simplicity rule), offline playback capability, bounded buffer duration (up to 300 seconds)

**Scale/Scope**: 1-to-5 minute WAV audio files, 1 to 10 chapter markers per note

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Hackathon MVP Scope**: PASS. Uses standard platform `MediaPlayer` to build the simplest working playback solution.
- **Principle II: ADHD Execution**: PASS. Clear, bounded, testable steps with tangible outcomes.
- **Principle III: Karpathy Discipline**: PASS. Extends existing `ChapterMarker` domain models without breaking existing audio capture or database layers.
- **Principle IV: Ponytail Simplicity**: PASS. Rejects `ExoPlayer` dependency; uses native Android platform APIs exclusively.
- **Principle V: Android Best Practices**: PASS. Follows MVVM and Unidirectional Data Flow with `StateFlow` and stateless `@Preview` composables.
- **Principle VI: No Workarounds or Magic Values**: PASS. All durations, colors, and layout tokens reside in theme and resource files.

---

## Project Structure

### Documentation (this feature)

```text
specs/006-audio-playback-chapters/
├── plan.md              # This file
├── research.md          # Architectural decisions and tradeoffs
├── data-model.md        # Entities, state machine, and validations
├── quickstart.md        # Verification scenarios and test execution
├── contracts/           # Kotlin interface contracts
│   ├── audio-player-contract.kt
│   └── playback-ui-contract.kt
└── checklists/
    └── requirements.md  # Spec quality validation checklist
```

### Source Code Layout

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── audio/
│   ├── AudioPlayer.kt                  # Contract & AudioPlaybackState
│   └── AndroidAudioPlayer.kt           # MediaPlayer implementation with coroutine ticker
├── data/
│   └── repository/
│       └── SnapshotNoteSink.kt         # Forwards generated chapters to NoteRepository
├── di/
│   └── AudioModule.kt                  # Dagger module binding AudioPlayer
└── ui/
    └── playback/
        ├── AudioPlaybackControls.kt    # Stateless Compose player & chapter seekbar
        └── NotePlaybackViewModel.kt    # ViewModel managing playback session

androidApp/src/test/kotlin/two/knockledge/
└── audio/
    └── AndroidAudioPlayerTest.kt       # Fast JVM unit tests for playback state & chapters
```

**Structure Decision**: Place core player logic under `audio/`, UI components under `ui/playback/`, and tests under `src/test/kotlin/two/knockledge/audio/`.

---

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| None | None | Architecture complies fully with all constitutional principles |
