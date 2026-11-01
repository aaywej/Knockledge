# Implementation Plan: Custom Notification Layout for Audio Recording

**Branch**: `011-custom-notification-layout` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/011-custom-notification-layout/spec.md`

## Summary

Build a custom Android notification layout (`RemoteViews`) for `KnockledgeService` to render a polished, high-contrast lock screen and notification shade card. Displays live recording duration (`Chronometer`), total session cache save count badge (`3 saved`), and circular action buttons for Rewind and Stop. Designed following **Impeccable** Operate mode principles for glanceability and touch accuracy.

## Technical Context

**Language/Version**: Kotlin 2.1 / Java 17 / Android SDK API 35 (Min SDK 26)
**Primary Dependencies**: AndroidX Core, Material Components, Android Framework Notification APIs (`RemoteViews`, `Notification.Builder`)
**Storage**: N/A (State derived from `KnockledgeController` in-memory state)
**Testing**: JVM Unit Tests (`./gradlew testDebugUnitTest`) & Manual Device/Emulator Verification
**Target Platform**: Android Phone / Tablet (Lock screen and Notification Shade)
**Project Type**: Native Android App (`:androidApp`)
**Performance Goals**: Instant UI response (<100ms) for button taps; 0% notification re-post overhead for ticking timer via native `Chronometer`.
**Constraints**: Must adhere to Android notification system constraints and OEM dark/light theme adaptivity.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Minimum Viable Hackathon Build**: Direct, focused implementation without speculative abstractions.
- [x] **II. ADHD-Driven Execution**: Action-first structure and clear verification targets.
- [x] **III. Karpathy Discipline**: Surgical changes restricted to `KnockledgeService`, layout resources, and controller state binding.
- [x] **IV. Ponytail Simplicity**: Uses standard Android platform `RemoteViews` APIs without adding 3rd-party notification libraries.
- [x] **V. Android Best Practices**: Respects foreground service lifecycle, pending intent security flags (`FLAG_IMMUTABLE`), and system notification guidelines.
- [x] **VI. Workaround & Hard-Coding Prohibition**: All strings, dimensions, and drawable resources defined in Android XML resources (`res/values/strings.xml`, `res/layout/`, `res/drawable/`).
- [x] **VII. Domain Layer Governance**: Direct ViewModel/Service-to-Controller state observation; no unnecessary use cases created.

## Project Structure

### Documentation (this feature)

```text
specs/011-custom-notification-layout/
├── spec.md              # Feature Specification
├── plan.md              # Implementation Plan (this file)
├── research.md          # Research & Impeccable Design Decisions
├── data-model.md        # State Schema & RemoteViews Mapping
├── quickstart.md        # Manual & Automated Verification Guide
└── contracts/
    └── notification-intents.md  # Intent & Action Contract Definitions
```

### Source Code (repository root)

```text
androidApp/src/main/
├── res/
│   ├── layout/
│   │   ├── notification_recording_compact.xml   # Custom collapsed RemoteViews layout
│   │   └── notification_recording_expanded.xml  # Custom expanded RemoteViews layout
│   ├── drawable/
│   │   ├── ic_notification_rewind.xml          # Rewind button vector icon
│   │   ├── ic_notification_stop.xml            # Stop button vector icon
│   │   └── bg_notification_badge.xml           # Save count badge pill background
│   └── values/
│       ├── strings.xml                          # Notification text strings & formats
│       └── dimens.xml                           # Notification layout dimensions
└── kotlin/group/two/knockledge/
    ├── KnockledgeService.kt                     # Foreground service inflating custom RemoteViews
    └── KnockledgeController.kt                  # State publisher exposing savedClips count
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | Fully compliant with project architecture and constitution |
