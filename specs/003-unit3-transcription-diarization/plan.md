# Implementation Plan: Unit 3 — Transcription & Diarization

**Branch**: `003-unit3-transcription-diarization` | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-unit3-transcription-diarization/spec.md`

## Summary

Unit 3 provides speech-to-text transcription and speaker diarization for cached PCM audio buffers. The implementation adopts a dual-engine architecture behind a unified `Transcriber` interface. Online requests route to Firebase AI (Gemini 1.5 Flash) to deliver automatic language detection (English, Tagalog, Taglish) and native speaker turn separation. When offline or upon cloud timeout/error, the system fails fast and switches immediately to the on-device Whisper ONNX engine (`AndroidSpeechEngine`) to generate plain text transcripts without user-facing delay. Audio conditioning reuses the existing `AudioGain` module.

## Technical Context

**Language/Version**: Kotlin 2.1.0, Java 17, Android SDK 35 (minSdk 26)

**Primary Dependencies**: AndroidX Core, Jetpack Compose, Dagger DI, Firebase AI Logic (Gemini 1.5 Flash), ONNX Runtime (`ai.onnxruntime:onnxruntime-android`)

**Storage**: In-memory PCM circular buffers, Room local database for transcript storage

**Testing**: JUnit 4, Kotlin Coroutines Test (`./gradlew test`)

**Target Platform**: Android 8.0+ (API 26+)

**Project Type**: Native Android Mobile Application

**Performance Goals**: Cloud transcription finishes in `< 0.5x` recording duration (30s audio in `< 15s`); cloud timeout switch to on-device completes in `< 10s`

**Constraints**: 16 kHz 16-bit mono PCM audio input; rolling circular buffer up to 5 minutes; zero UI frame drops or memory leaks

**Scale/Scope**: Single module (`androidApp`), Unit 3 speech recognition subsystem

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Minimum Viable Hackathon Build (3-Day Scope)**: PASS. Uses single unified `Transcriber` interface; reuses existing Firebase AI and ONNX Whisper engines; avoids speculative on-device diarization models.
- **Principle II: ADHD-Driven Execution**: PASS. Clear component boundaries; actionable verification steps in quickstart; sequential tasks capped at 5.
- **Principle III: Karpathy Discipline**: PASS. Preserves existing codebase style; touches only required pipeline files; supported by JVM unit tests.
- **Principle IV: Ponytail Simplicity**: PASS. Reuses existing `AudioGain` and platform APIs; introduces zero unapproved third-party dependencies.
- **Technology Stack & Architecture Constraints**: PASS. Adheres to MVVM/UDF, Dagger DI, Firebase AI, Room, and 16 kHz PCM buffer requirements.

## Project Structure

### Documentation (this feature)

```text
specs/003-unit3-transcription-diarization/
├── plan.md              # This implementation plan
├── research.md          # Phase 0 technology choices and decisions
├── data-model.md        # Phase 1 data entities and lifecycle
├── quickstart.md        # Phase 1 validation scenarios
├── contracts/           # Phase 1 contract schemas and interfaces
│   ├── transcriber-interface.kt
│   └── gemini-transcription-schema.json
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── ai/
│   ├── AndroidEngines.kt        # Existing on-device Whisper & LiteRT-LM engines
│   ├── FirebaseAiTranscriber.kt # Online Firebase AI (Gemini 1.5 Flash) transcriber & diarizer
│   └── OnDeviceAi.kt            # SpeechEngine & model lifecycle abstractions
├── core/
│   ├── AudioGain.kt             # Audio gain normalization and noise suppression
│   ├── AudioRingBuffer.kt       # In-memory circular audio buffer
│   └── RewindPolicy.kt          # Buffer timing and 16 kHz sample rate constants
├── pipeline/
│   ├── Pipeline.kt              # Core Transcriber contract, Transcript data class, RewindPipeline
│   ├── SensitiveFilter.kt       # PII filtering
│   └── TextFormats.kt           # Text normalization and Whisper initial prompts
└── di/
    └── KnockledgeComponent.kt   # Dagger DI bindings for Transcriber and engines

androidApp/src/test/kotlin/two/knockledge/
├── AudioGainTest.kt             # Existing audio gain tests
├── SensitiveFilterTest.kt       # PII filtering tests
└── TranscriberTest.kt           # Unit tests for online/offline Transcriber routing & fallback
```

**Structure Decision**: Integrated directly into `androidApp` module within existing `pipeline/` and `ai/` packages, matching existing project architecture.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations. All components adhere strictly to project constitution principles.*
