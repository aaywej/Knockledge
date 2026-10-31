# Implementation Plan: Silero VAD Audio Pre-Processing

**Branch**: `009-silero-vad-preprocessing` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-silero-vad-preprocessing/spec.md`

## Summary

Implement neural voice activity detection (VAD) using Silero VAD v5 to isolate spoken voice segments and eliminate non-speech audio (such as typing, table knocks, and room hum) along with dead air in the rewind pre-processing pipeline. Introduce `SileroAudioPreprocessor` implementing `AudioPreprocessor`, backed by a decoupled `VadDetector` interface with `OnnxSileroVadDetector` (for production using the existing `onnxruntime-android` dependency) and `FallbackEnergyVadDetector` (for fast JVM testing without native `.so` dependencies). Normalize volume of conditioned speech to target headroom (`TARGET_PEAK_AMPLITUDE = 29,490`), reject audio with under 1.0 second of speech, and preserve the original recorded WAV snapshot completely intact for user note playback.

## Technical Context

**Language/Version**: Kotlin 2.4.10 / Android SDK 37 (minSdk 26)

**Primary Dependencies**: Kotlin Standard Library, Kotlinx Coroutines, Dagger DI, ONNX Runtime (`com.microsoft.onnxruntime:onnxruntime-android:1.20.0`), Android Audio APIs

**Storage**: Local filesystem for original audio snapshots (WAV) and Room SQLite for note persistence

**Testing**: JUnit 4, Kotlinx Coroutines Test

**Target Platform**: Android 8.0+ (API level 26+)

**Project Type**: Native Android Application (Audio & AI Pipeline)

**Performance Goals**: Neural VAD inference processes 30-second audio buffers in under 500 ms on device; eliminates >= 90% of non-speech ambient noises; reduces rewind payload duration by >= 20% on conversational speech.

**Constraints**: Original audio snapshot remains completely unedited; zero changes to existing `RewindPipeline` contract; JVM unit tests pass cleanly without native ONNX library dependencies.

**Scale/Scope**: Circular audio buffers from 5 seconds to 300 seconds (16 kHz 16-bit mono PCM).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Minimum Viable Hackathon Build)**: PASS. Implements surgical VAD neural classification behind existing `AudioPreprocessor` contract. Reuses already-integrated `onnxruntime-android`.
- **Principle II (ADHD-Driven Execution)**: PASS. Immediately stops pipeline and notifies user when speech content is insufficient (< 1.0s).
- **Principle III (Karpathy Discipline)**: PASS. Surgical edits to pipeline preprocessor and DI bindings. Preserves existing codebase style.
- **Principle IV (Ponytail Simplicity)**: PASS. Reuses existing ONNX Runtime dependency already present in `build.gradle.kts` and approved by user. Decouples behind lightweight interface to avoid test friction.
- **Principle V (Android Best Practices)**: PASS. Executes neural inference on background coroutines (`Dispatchers.Default`); closes ONNX tensors and sessions deterministically.
- **Principle VI (Workaround and Hard-Coding Prohibition)**: PASS. Constants (thresholds, frame size, sample rate) encapsulated in `SileroVadConfig` and companion constants.
- **Principle VII (Domain Layer and Interactor Governance)**: PASS. Direct ViewModel/Controller-to-Pipeline wiring; no unapproved domain interactors.

## Project Structure

### Documentation (this feature)

```text
specs/009-silero-vad-preprocessing/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── vad-detector-contract.kt
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── pipeline/
│   ├── AudioPreprocessor.kt          # Existing: interface and constants
│   ├── VadDetector.kt                # New: VadDetector interface, SileroVadConfig, and FallbackEnergyVadDetector
│   ├── OnnxSileroVadDetector.kt      # New: Production Silero VAD implementation using ONNX Runtime
│   └── SileroAudioPreprocessor.kt    # New: AudioPreprocessor implementation backed by VadDetector
└── di/
    └── KnockledgeComponent.kt        # Update: Bind SileroAudioPreprocessor as AudioPreprocessor

androidApp/src/test/kotlin/two/knockledge/
└── pipeline/
    ├── AudioPreprocessorTest.kt      # Existing: regression tests for DefaultAudioPreprocessor
    └── SileroAudioPreprocessorTest.kt # New: unit tests for SileroAudioPreprocessor and VadDetector
```

**Structure Decision**: Create `VadDetector.kt`, `OnnxSileroVadDetector.kt`, and `SileroAudioPreprocessor.kt` in `group.two.knockledge.pipeline`. Bind `SileroAudioPreprocessor` into `KnockledgeComponent.kt` without changing `RewindPipeline` or `KnockledgeController`. Add unit tests in `SileroAudioPreprocessorTest.kt`.

## Complexity Tracking

*No constitution violations. Reuses existing ONNX Runtime dependency and existing AudioPreprocessor contract.*
