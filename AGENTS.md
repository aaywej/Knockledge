# Knockledge Agent Instructions

Knockledge is a native Android caching audio recording application. The application records audio continuously in a circular window buffer. It automatically removes older unmarked audio to save memory. The device accelerometer detects gestures to save recently recorded audio.

Agents must follow the constitution in [.specify/memory/constitution.md](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/.specify/memory/constitution.md).

## Core Directives

1. **Hackathon MVP Scope (3-Day Limit)**: Build the minimum working solution. Stop work immediately when acceptance criteria pass. Reject speculative features and premature optimization.
2. **ADHD Communication Rules**: Lead responses with the next direct action on line 1. Format sequential tasks as numbered lists capped at 5 items. Conclude with one concrete action doable in under 2 minutes. Eliminate pleasantries and conversational recaps.
3. **Karpathy Discipline**: State assumptions explicitly before coding. Touch only required code lines. Preserve existing code style and formatting. Write a reproducing test before fixing any defect.
4. **Ponytail Simplicity**: Use Android platform APIs and Kotlin standard library first. Favor code deletion over code addition. Require approval for every new dependency.
5. **Android Best Practices**: Follow official Android architecture guidelines, Jetpack standards, and lifecycle rules.
6. **No Hard-Coding or Workarounds**: Prohibit hard-coded values, raw strings, and temporary hacks. Ask the user first and obtain explicit approval before using any workaround.
7. **Domain Layer Governance**: Maintain direct ViewModel-to-Repository communication by default. Consider domain use cases or interactors only when multiple ViewModels share complex multi-repository logic. Always prompt the user for feedback and secure explicit approval before creating any domain use case or interactor.

8. **Shell**: Always run commands with `shell_flavor: "powershell"` and PowerShell syntax. Never use bash.
9. **Android CLI & Studio**: Use the official `android` CLI for Android tasks. Use `android studio ...` commands during development when an Android Studio instance is running.

## Architecture & Technology Stack

- **Architecture**: MVVM with Unidirectional Data Flow (UDF). ViewModels expose immutable `StateFlow` state to Jetpack Compose UI.
- **Core Platform**: Android SDK, Kotlin, Jetpack Compose, and Dagger DI.
- **Persistence**: Room for local caching and Cloud Firestore for remote synchronization.
- **Authentication**: Firebase Auth.
- **Audio & Sensors**: Circular window buffer, accelerometer gesture detection, Google SpeechRecognizer, and Whisper.
- **AI Processing**: Firebase AI for cloud inference.

## Quality Gates & Verification

- **Code Quality**: Define UI strings, dimensions, and theme values in Android resources. Prohibit magic literals and unapproved workarounds.
- **Kotlin Documentation**: Write KDoc comments for all classes and public methods. Add explanatory comments to complex private helper methods.
- **Compose Previews**: Provide stateless `@Preview` composables for all UI components. Never couple previews to ViewModels or databases. Wrap previews in the project theme.
- **Unit Testing**: Run and pass `./gradlew test` for routine verification. Write fast JVM unit tests for all business logic.
- **Integration Tests**: Never run integration tests in bulk. Always ask user permission before running full integration tests.
