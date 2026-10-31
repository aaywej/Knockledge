<!--
SYNC IMPACT REPORT
- Version change: 1.1.0 -> 1.2.0
- List of modified principles:
  - None
- Added sections:
  - Kotlin documentation directives under Code Quality and Architecture Directives
- Removed sections:
  - None
- Follow-up TODOs:
  - None
-->

# Knockledge Constitution

Knockledge is a native Android application for caching-based audio recording. The application records audio continuously within a predefined circular window buffer. It automatically deletes older unmarked audio to minimize memory consumption. Users record and recall key segments from lectures, conferences, and presentations. Through the device accelerometer, a user triggers a gesture to capture recently recorded audio segments, including the moments immediately preceding the gesture.

## Core Principles

### I. Minimum Viable Hackathon Build (3-Day Scope)
Every feature MUST serve the working end-to-end hackathon demo.
- Prioritize working user flows over optional features.
- Reject speculative abstractions, gold-plating, and premature optimization.
- Stop immediately when acceptance criteria pass.
- Rationale: The team has a strict 3-day hackathon timeline. Shipping an operable end-to-end product takes precedence over perfection.

### II. ADHD-Driven Execution
All project communications and agent interactions MUST facilitate immediate action.
- Lead every communication with the next direct action.
- Format sequential steps as bounded numbered lists capped at 5 items.
- Conclude tasks with one concrete action doable in under 2 minutes.
- Eliminate pleasantries, filler phrases, and unprompted conversational recaps.
- Rationale: Clear, action-first steps reduce cognitive load and prevent task stalling.

### III. Karpathy Discipline
Code implementation MUST stay minimal, surgical, and verifiable.
- State all assumptions explicitly before coding.
- Edit only the lines required for the specific task.
- Preserve existing formatting, unrelated comments, and surrounding code structure.
- Write a reproducing test before fixing any reported defect.
- Rationale: Restricting scope avoids unintended regressions and keeps the codebase understandable.

### IV. Ponytail Simplicity
The codebase MUST maintain the shortest functional solution.
- Use Android platform APIs and standard library functions before adding dependencies.
- Require explicit justification and approval for every new external library.
- Prefer code deletion over code addition.
- Avoid building single-use abstractions or speculative configuration layers.
- Rationale: Minimal dependencies and clean platform code prevent dependency conflicts and maintenance debt.

### V. Android Best Practices
Developers and agents MUST follow official Android development best practices.
- Apply official Android architecture guidelines and modern Jetpack libraries.
- Respect Android component lifecycles, coroutine dispatchers, and state management patterns.
- Implement responsive, power-efficient, and accessible Android platform interfaces.
- Rationale: Official Android practices prevent memory leaks, crashes, and maintenance debt.

### VI. Workaround and Hard-Coding Prohibition
Developers and agents MUST NOT hard-code values or introduce temporary workarounds.
- Store strings, dimensions, colors, and configurations in standard Android resource files.
- Prohibit undocumented bypasses, temporary patches, and shortcut workarounds across all layers.
- If an agent or developer insists on a workaround, they MUST ask the user first.
- Obtain explicit user approval before you insert any workaround or hard-coded value into code.
- Rationale: Hard-coded values and workarounds create hidden defects, degrade code quality, and increase technical debt.

## Technology Stack & Architecture Constraints

The project MUST adhere to the following architecture and component boundaries:

1. **Architecture Pattern**: MVVM with Unidirectional Data Flow (UDF). ViewModels expose immutable `StateFlow` state to Jetpack Compose UI.
2. **Core Platform & UI**: Native Android SDK, Kotlin, and Jetpack Compose.
3. **Dependency Injection**: Dagger DI.
4. **Data Persistence**: Room for local device caching and Cloud Firestore for remote synchronization.
5. **Identity & Auth**: Firebase Auth.
6. **Audio & Sensor Pipeline**: Circular window buffer for audio caching, device accelerometer for gesture capture, Google SpeechRecognizer, and Whisper.
7. **AI Inference**: Firebase AI for cloud-assisted processing.

## Development Workflow & Quality Gates

Development activities MUST pass these quality gates before integration:

### Code Quality and Architecture Directives
- Adhere strictly to recommended Android architecture and Jetpack best practices.
- Define UI strings, dimensions, and theme attributes in Android XML resources or Compose theme tokens.
- Never use magic literals, hard-coded URLs, or inline credentials.
- Prompt the user and secure written consent before writing any necessary workaround.
- Write KDoc comments for all classes and public methods.
- Add explanatory comments to complex private helper methods.

### Compose Preview Directives
- Write stateless `@Preview` functions for every screen and reusable UI component.
- Never instantiate ViewModels, Room databases, or network clients inside preview composables.
- Provide distinct preview states for loading, active content, and error views.
- Wrap all previews inside the application theme container.
- Re-render previews through the sidecar only after saving all file changes.

### Testing Directives
- **Unit Tests First**: Write fast JVM unit tests for business logic, ViewModel state transformations, and buffer operations.
- **No Bulk Integration Tests**: Run integration tests in isolation. Never run integration tests in bulk.
- **User Approval Required**: Always prompt and obtain user permission before executing full integration test runs.
- **Bug Reproducing Tests**: Write a failing test before resolving any defect.
- **Routine Verification**: Run and pass `./gradlew test` before marking any development task complete.

## Governance

This constitution supersedes all other documentation, practices, and informal conventions in this repository.

- **Amendment Process**: Amendments require documented rationale, impact analysis, and explicit user approval.
- **Versioning Policy**: Semantic versioning (MAJOR.MINOR.PATCH) applies to this document:
  - MAJOR: Incompatible principle removals or architecture changes.
  - MINOR: Added principles, new stack components, or expanded workflow gates.
  - PATCH: Wording refinements, typo corrections, and non-semantic adjustments.
- **Compliance Review**: All specifications, implementation plans, and code reviews MUST verify compliance with these rules.

**Version**: 1.2.0 | **Ratified**: 2026-10-29 | **Last Amended**: 2026-10-31
