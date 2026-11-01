# Implementation Plan: Heuristic Knock Detection

**Branch**: `005-heuristic-knock-detection` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-heuristic-knock-detection/spec.md`

## Summary

Port and isolate the non-machine-learning heuristic knock detection pipeline from TapTap into Knockledge. The system calculates acceleration jerk ($\Delta a / \Delta t$) on the Z-axis, applies low-pass and high-pass bandpass filtering, and identifies physical knocks using dual-peak rebound matching. If accuracy during evaluation satisfies the 85% threshold, the lightweight heuristic remains the permanent solution; otherwise, the architecture cleanly transitions to the TensorFlow Lite classifier.

## Technical Context

**Language/Version**: Kotlin 2.0 / Java 17  
**Primary Dependencies**: Android SDK Platform Sensors (`SensorManager`, `Sensor.TYPE_ACCELEROMETER`), Dagger DI  
**Storage**: N/A (in-memory circular signal processing window)  
**Testing**: JUnit 4 JVM unit tests (`./gradlew test`)  
**Target Platform**: Android 8.0+ (API 26+)  
**Project Type**: Mobile Application Component (Signal Processing / Sensor Service)  
**Performance Goals**: Detection latency under 150ms, processing overhead under 1% CPU, 0 frame drops  
**Constraints**: Continuous operation during audio recording, power-efficient sensor sampling, zero additional third-party dependencies  
**Scale/Scope**: Single sensor service pipeline; 4 mathematical filter classes; ~200 lines of Kotlin  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Justification |
|---|---|---|
| **I. Hackathon MVP Scope (3-Day)** | **PASS** | Ships the simplest non-ML gesture detection first; avoids heavy model tuning and asset loading. |
| **II. ADHD-Driven Execution** | **PASS** | Plan follows bounded numbered steps with explicit verification criteria. |
| **III. Karpathy Discipline** | **PASS** | Reuses existing mathematical primitives (`Slope`, `PeakDetector`, `Resample`) without touching unrelated audio or UI code. Reproducing tests verify logic before integration. |
| **IV. Ponytail Simplicity** | **PASS** | Eliminates TensorFlow Lite dependency for this mode. Relies solely on native platform accelerometer and standard library math. |

## Project Structure

### Documentation (this feature)

```text
specs/005-heuristic-knock-detection/
├── plan.md              # Implementation plan (this document)
├── research.md          # Architectural decisions & math pipeline analysis
├── data-model.md        # Entities, signals, and state transitions
├── quickstart.md        # Runnable verification guide
├── contracts/           # Kotlin interface contracts
│   └── heuristic-detector-contract.kt
└── checklists/
    └── requirements.md  # Spec validation checklist
```

### Source Code Layout

```text
androidApp/src/main/kotlin/group/two/knockledge/sensor/
├── KnockGestureDetector.kt                  # Base interface contract
├── AndroidKnockGestureDetector.kt           # Production sensor listener
└── columbus/                                # Core math primitives
    ├── PeakDetector.kt                      # Positive/negative extrema tracking
    ├── Slope1C.kt / Slope3C.kt              # Jerk derivative calculation
    ├── Lowpass1C.kt / Highpass1C.kt         # Frequency band filtering
    ├── Resample1C.kt / Resample3C.kt        # Timestamp normalization
    └── TapRT.kt / ColumbusTapRT.kt          # Heuristic gesture recognition logic

androidApp/src/test/kotlin/two/knockledge/
└── HeuristicKnockGestureDetectorTest.kt     # JVM unit tests for signal math & timing
```

## Structure Decision

Keep all sensor signal processing contained within `androidApp/src/main/kotlin/group/two/knockledge/sensor/`. The heuristic detector implements `KnockGestureDetector`, making it a plug-and-play drop-in replacement that decouples signal mathematics from Android hardware lifecycles.

## Complexity Tracking

*No constitutional violations identified. No complexity exceptions required.*
