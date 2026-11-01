# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Anyone who needs on-demand audio capture. Users include students in lectures, attendees in conferences, and participants in meetings where critical spoken moments occur without notice.

## Product Purpose

Knockledge records rolling audio continuously in a local circular buffer. The application removes old unmarked audio automatically to save memory and storage. Users capture recent audio retroactively through a physical double-knock gesture or a manual button. The application saves key moments without permanent recording.

## Positioning

Retroactive audio capture through physical accelerometer knock detection and circular buffer storage. Standard voice recorders require the user to start recording before speech begins. Knockledge captures audio that just occurred when the user knocks on the desk or phone.

## Operating Context

- Users operate the device in lectures, conference halls, and workplace meetings.
- The phone rests flat on a table, desk, or podium, or remains in the user hand.
- Audio recording runs as a persistent foreground service with a rolling local cache.
- Accelerometer algorithms detect double-knock acceleration peaks while filtering common phone movement and vibrations.

## Capabilities and Constraints

- Continuous circular audio buffer in local storage with configurable length up to 5 minutes.
- Automatic removal of audio frames exceeding the configured buffer limit.
- Physical double-knock gesture detection using linear acceleration peak intervals between 100 ms and 400 ms.
- Haptic feedback confirmation when a knock gesture triggers a capture.
- Manual save button on the user interface as a gesture fallback.
- Audio pre-processing pipeline that removes silent pauses and normalizes volume for AI processing.
- Original audio preservation so playback preserves the authentic, unedited recording.
- Audio playback interface with seekbar control and interactive timestamped chapter navigation.
- Speech transcription and structured summarization powered by cloud inference and on-device offline models.
- Default manual AI processing mode that saves audio immediately and runs AI analysis only on user request.
- Configurable settings toggle to enable automatic AI processing.
- Mandatory user authentication using Google Sign-In or email and password; anonymous and guest login are prohibited.
- Implementation constraints: Android SDK, Kotlin, Jetpack Compose, Material 3, Room, Firestore, and Firebase Auth.

## Brand Commitments

- Product name: Knockledge.
- Tone: Focused, dependable, and unobtrusive.
- Visual standards: Android platform conventions and system insets; visual styling and theme are currently under definition.

## Evidence on Hand

- Native Android source code in the `androidApp` and `sensor` project modules.
- Architectural directives in [.specify/memory/constitution.md](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/.specify/memory/constitution.md).
- Formal technical specifications in `specs/001-unit1-ai-engine` through `specs/008-audio-preprocessing`.
- Test suites in `androidApp/src/test` and `sensor/src/test`.

## Product Principles

1. Retroactive First: Capture speech that already occurred to eliminate recording anxiety.
2. Frictionless Capture: The physical double-knock gesture enables capture without unlocking the phone.
3. Authentic Preservation: Pre-processing prepares audio for AI models while user notes preserve raw original audio.
4. User-Governed Resources: The circular buffer limits storage, and AI executes on demand to conserve power.
5. Native Android Fidelity: Apply standard Jetpack Compose architecture and Material 3 design patterns.

## Accessibility & Inclusion

- Manual on-screen save button provides a reliable fallback for users who cannot perform physical knocks.
- All touch targets meet or exceed 48x48 dp dimensions.
- Text sizes use sp units to respect user font scale settings.
- Haptic vibrations confirm gesture triggers for tactile feedback.
