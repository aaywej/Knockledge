# Implementation Plan: Audio Pre-Processing Pipeline Unit

**Branch**: `008-audio-preprocessing` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-audio-preprocessing/spec.md`

## Summary

Completely remove the text-filtering unit (`SensitiveFilter`) from the rewind pipeline. Introduce an upfront audio pre-processing unit (`AudioPreprocessor`) that applies frame-based Voice Activity Detection (VAD) to eradicate extended dead air (>1.5s silence truncated to 300ms ambient silence) and normalizes volume to optimize audio for downstream AI models (Whisper/Gemini). If remaining active speech is under 1.0 second, skip AI processing and note persistence, and inform the user via the existing error reporting channel. Crucially, preserve the original recorded audio file (`wav`) completely intact for playback in saved notes.

## Technical Context

**Language/Version**: Kotlin 2.0+ / Android SDK 35 (minSdk 26)

**Primary Dependencies**: Kotlin Standard Library, Kotlinx Coroutines, Dagger DI, Android SDK Audio APIs

**Storage**: Local filesystem for original audio snapshots (WAV) and Room SQLite for note persistence

**Testing**: JUnit 4, Kotlinx Coroutines Test

**Target Platform**: Android 8.0+ (API level 26+)

**Project Type**: Native Android Application (Audio & AI Pipeline)

**Performance Goals**: Audio pre-processing executes in under 20 ms for a 5-minute buffer; reduces audio payload by >= 20% on conversational speech; improves overall AI pipeline latency by >= 15%

**Constraints**: Pure Kotlin implementation (no native C++ or external audio libraries); original audio snapshot file remains completely unedited; zero Jetpack Compose UI modifications

**Scale/Scope**: Circular audio buffers from 5 seconds to 300 seconds (16 kHz 16-bit mono PCM)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Minimum Viable Hackathon Build)**: PASS. Implements surgical dead air eradication and peak normalization in pure Kotlin. Removes sensitive filtering unit without speculative abstractions.
- **Principle II (ADHD-Driven Execution)**: PASS. Eradicates dead air to speed up AI recap delivery; immediately notifies user when an audio snapshot contains insufficient speech.
- **Principle III (Karpathy Discipline)**: PASS. Touches only the pipeline, controller, and DI binding layers. Preserves surrounding recording and playback architecture.
- **Principle IV (Ponytail Simplicity)**: PASS. Uses standard Kotlin math and array operations for RMS VAD and peak normalization. Avoids adding heavyweight third-party audio signal processing libraries.
- **Principle V (Android Best Practices)**: PASS. Executes pre-processing on background coroutine dispatchers off the main thread; respects memory lifecycle by clearing intermediate arrays.
- **Principle VI (Workaround and Hard-Coding Prohibition)**: PASS. Constants (frame size, sample rate, speech threshold, target peak) declared in contracts and companion objects.
- **Principle VII (Domain Layer and Interactor Governance)**: PASS. Rewind pipeline interfaces directly with controller and repository without unrequested domain interactors.

## Project Structure

### Documentation (this feature)

```text
specs/008-audio-preprocessing/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── audio-preprocessor-contract.kt
│   └── rewind-pipeline-contract.kt
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── pipeline/
│   ├── AudioPreprocessor.kt          # New: AudioPreprocessor interface & DefaultAudioPreprocessor implementation
│   ├── Pipeline.kt                   # Update: RewindPipeline uses AudioPreprocessor; remove SensitiveFilter; return PipelineResult
│   └── SensitiveFilter.kt            # Decouple/retain if needed, but remove from pipeline flow
├── KnockledgeController.kt           # Update: Handle PipelineResult (skip persistence & reportError on InsufficientSpeech; keep original WAV)
├── KnockledgeGraph.kt                # Update: Replace filter parameter with preprocessor in test/preview constructor
└── di/
    └── KnockledgeComponent.kt        # Update: Provide AudioPreprocessor and inject into RewindPipeline; remove SensitiveFilter injection

androidApp/src/test/kotlin/two/knockledge/
├── pipeline/
│   ├── AudioPreprocessorTest.kt      # New: Unit tests for VAD dead air removal, normalization, and <1.0s threshold
│   └── RewindPipelineTest.kt         # Update: Unit tests for RewindPipeline with preprocessor and no filter
└── RewinderSaveThresholdTest.kt      # Update: RewindController pipeline invocation tests
```

**Structure Decision**: Create `AudioPreprocessor.kt` in `group.two.knockledge.pipeline`, refactor `RewindPipeline` to replace `SensitiveFilter` with `AudioPreprocessor`, update `KnockledgeController` to handle `PipelineResult.InsufficientSpeech` by skipping note persistence, and update Dagger DI in `KnockledgeComponent.kt`.

## Complexity Tracking

*No constitution violations. Pure Kotlin platform implementation with direct ViewModel/Controller-to-Pipeline wiring.*
