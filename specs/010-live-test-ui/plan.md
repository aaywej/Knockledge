# Implementation Plan: Utilitarian Material 3 Live Test UI

**Branch**: `010-live-test-ui` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-live-test-ui/spec.md`

## Summary

Build a utilitarian, single-activity Material 3 live test UI in `MainActivity` that unifies all core subsystems (continuous circular audio recording, hardware accelerometer knock detection, note persistence, audio playback with chapter seeking, on-demand AI transcription and summarization, and runtime configuration/authentication) into a clean 3-tab layout (`RECORD`, `NOTES`, `SETTINGS`). The UI maintains a strictly lean build without decorative styling or extra dependencies, directly reusing existing controllers and ViewModels (`KnockRecordingViewModel`, `NotePlaybackViewModel`, `NoteProcessingViewModel`, `SettingsViewModel`, `AuthViewModel`) and official Material 3 Compose components.

## Technical Context

**Language/Version**: Kotlin 2.4.10 / Android SDK 37 (minSdk 26)

**Primary Dependencies**: Android Jetpack Compose, Material 3 (`androidx.compose.material3:material3`), Dagger DI, Kotlinx Coroutines

**Storage**: Room SQLite database for note entity storage (`NoteRepository`), local filesystem for audio snapshots (`AudioFileStorage`), and Android `SharedPreferences` for user settings (`AppSettings`)

**Testing**: JUnit 4, Kotlinx Coroutines Test, Compose Previews

**Target Platform**: Android 8.0+ (API level 26+)

**Project Type**: Native Android Application (Compose UI Shell)

**Performance Goals**: Tab transitions under 300 ms; initial UI rendering under 2 seconds; knock event visual feedback latency under 500 ms.

**Constraints**: Utilitarian styling only (no custom decorative draw modifiers or extra styling libraries); zero new third-party dependencies; all strings stored in `strings.xml`.

**Scale/Scope**: 1 unified activity screen with 3 test tabs, serving full end-to-end live testing of all backend, recording, and AI units.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Minimum Viable Hackathon Build)**: PASS. Delivers the minimal working UI needed to test all built components live on device/emulator within the 3-day hackathon limit.
- **Principle II (ADHD-Driven Execution)**: PASS. Directly exposes recording, playback, and AI test controls on the primary interface with instant feedback; caps actions to single-tap operations.
- **Principle III (Karpathy Discipline)**: PASS. Reuses existing tested ViewModels and UI controls without introducing speculative abstractions or modifying unrelated code.
- **Principle IV (Ponytail Simplicity)**: PASS. Uses standard Material 3 Compose components; zero new external dependencies; shortest working solution.
- **Principle V (Android Best Practices)**: PASS. Follows Android UDF architecture, ViewModel lifecycle handling, Compose state hoisting, and provides stateless `@Preview` composables.
- **Principle VI (Workaround and Hard-Coding Prohibition)**: PASS. Eliminates magic literals; defines all UI labels and messages in Android resource files (`strings.xml`).
- **Principle VII (Domain Layer and Interactor Governance)**: PASS. ViewModels connect directly to Repositories and Controllers; no use cases or interactors introduced.

## Project Structure

### Documentation (this feature)

```text
specs/010-live-test-ui/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── live-ui-contracts.kt
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── MainActivity.kt                       # Update: Utilitarian Material 3 live test UI shell
├── ui/
│   ├── LiveTestController.kt             # New: LiveTestTab, LiveTestUiState, and LiveTestCoordinator
│   ├── LiveTestScreen.kt                 # New: Utilitarian Material 3 screen composable with tabs
│   ├── tabs/
│   │   ├── RecordTab.kt                  # New: RecordingControls, buffer progress, and live knock pulse
│   │   ├── NotesTab.kt                   # New: Notes list, selection, playback controls, and AI details
│   │   └── SettingsTab.kt                # New: Auth card, auto-AI switch, and buffer/sensor settings
│   ├── theme/
│   │   ├── Theme.kt                      # Existing: Material 3 KnockledgeTheme
│   │   └── Color.kt                      # Existing: Material 3 color scheme
│   ├── KnockRecordingViewModel.kt        # Existing: recording session and buffer controller
│   ├── playback/
│   │   ├── NotePlaybackViewModel.kt      # Existing: audio playback controller
│   │   └── AudioPlaybackControls.kt      # Existing: play/pause, seek, chapter controls
│   ├── processing/
│   │   ├── NoteProcessingViewModel.kt    # Existing: on-demand AI processing controller
│   │   └── NoteProcessingControls.kt    # Existing: AI progress and retry controls
│   ├── settings/
│   │   ├── SettingsViewModel.kt          # Existing: runtime preferences controller
│   │   └── AiProcessingSettingsControl.kt # Existing: auto-AI toggle
│   └── auth/
│       ├── AuthViewModel.kt              # Existing: Firebase authentication controller
│       └── AuthScreen.kt                 # Existing: sign-in and registration UI
└── di/
    └── KnockledgeComponent.kt            # Update: Expose audioPlayer and pipeline if needed for ViewModels

androidApp/src/main/res/values/
└── strings.xml                           # Update: UI string resources for tabs and live test actions

androidApp/src/test/kotlin/two/knockledge/
└── ui/
    └── LiveTestCoordinatorTest.kt        # New: Unit tests for tab navigation and knock pulse state
```

**Structure Decision**: Host the utilitarian live test UI in `MainActivity.kt` and `LiveTestScreen.kt`, organizing the 3 tabs into `RecordTab.kt`, `NotesTab.kt`, and `SettingsTab.kt` within `group.two.knockledge.ui`. Bind existing ViewModels directly to their respective controllers without intermediate domain interactors.

## Complexity Tracking

*No constitution violations. Reuses existing ViewModels and Jetpack Material 3 components directly.*
