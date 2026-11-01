# Implementation Plan: Configurable AI Processing and Manual Trigger

**Branch**: `007-configurable-ai-processing` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-configurable-ai-processing/spec.md`

## Summary

Make automatic speech transcription and AI summarization configurable, defaulting to disabled so that audio captures save immediately without executing AI processing. Introduce on-demand user-triggered processing within the note UI and a settings toggle to re-enable automated post-save processing.

## Technical Context

**Language/Version**: Kotlin 2.0+ / Android SDK 35 (minSdk 26)

**Primary Dependencies**: Jetpack Compose, Kotlin Coroutines & Flow, Room, Dagger DI, Firebase Auth & Cloud Firestore, LiteRT-LM & Whisper

**Storage**: Android `SharedPreferences` / `KeyValueStore` for settings, Room SQLite and local audio WAV files for note storage

**Testing**: JUnit 4, Kotlinx Coroutines Test, Roborazzi / Android Compose Tests

**Target Platform**: Android 8.0+ (API level 26+)

**Project Type**: Mobile Android Application

**Performance Goals**: Audio buffer capture completes and persists in under 1 second; UI processing state updates within 500 milliseconds

**Constraints**: Audio persistence must succeed even when offline or when AI models fail; no concurrent duplicate processing jobs on the same note

**Scale/Scope**: Circular audio buffers up to 5 minutes; notes collection per authenticated user

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Minimum Viable Hackathon Build)**: PASS. Reuses existing `AppSettings`, `NoteRepository`, and `RewindPipeline`. Implements only required configuration and manual trigger.
- **Principle II (ADHD-Driven Execution)**: PASS. Provides immediate manual control over expensive AI processing and clear UI feedback.
- **Principle III (Karpathy Discipline)**: PASS. Surgical additions to settings and note processing coordinator without refactoring unchanged audio recording pipelines.
- **Principle IV (Ponytail Simplicity)**: PASS. Uses standard Android SharedPreferences and StateFlow without adding third-party workflow orchestrators.
- **Principle V (Android Best Practices)**: PASS. Follows Jetpack Compose UDF architecture and coroutine lifecycles.
- **Principle VI (Workaround and Hard-Coding Prohibition)**: PASS. Resource keys and UI strings declared cleanly.
- **Principle VII (Domain Layer and Interactor Governance)**: PASS. Directly connects ViewModel to `NoteRepository` and `RewindPipeline` without introducing unrequested domain interactors.

## Project Structure

### Documentation (this feature)

```text
specs/007-configurable-ai-processing/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── settings-contract.kt
│   └── note-processing-contract.kt
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── settings/
│   └── AppSettings.kt                     # Add autoAiProcessingEnabled StateFlow & setter
├── KnockledgeController.kt                # Decouple raw audio save from automatic pipeline run
├── data/
│   └── repository/
│       └── SnapshotNoteSink.kt            # Provide audio-only note creation method
├── ui/
│   ├── processing/
│   │   ├── NoteProcessingController.kt   # Interface & UI state for on-demand trigger
│   │   └── NoteProcessingViewModel.kt    # ViewModel executing RewindPipeline on demand
│   └── notes/
│       └── NoteProcessingControls.kt      # Compose UI button and progress indicator
```

**Structure Decision**: Add settings property to `AppSettings.kt`, update `KnockledgeController.kt` to check the setting on rewind, and add `NoteProcessingViewModel` and Compose controls under `ui/processing/` for on-demand triggering.

## Complexity Tracking

*No constitution violations. Direct ViewModel-to-Repository and platform APIs applied throughout.*
