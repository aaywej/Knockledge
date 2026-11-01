# Implementation Plan: Unit 2 — Firebase, Auth & Storage

**Branch**: `002-unit2-firebase-auth-storage` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-unit2-firebase-auth-storage/spec.md`

## Summary

Unit 2 establishes the core persistence, identity, and cloud synchronization backbone for Knockledge. It introduces the unified `Note` Aggregate Root that binds captured audio from Unit 4, diarized transcripts from Unit 3, and structured AI summaries from Unit 1. The architecture leverages Firebase Authentication (supporting Google Sign-In and Email/Password via Android Credential Manager) with a mandatory authentication gate on launch. Local storage uses Android Jetpack Room for sub-200ms offline-first writes, while Cloud Firestore and Firebase Storage provide automatic background sync upon network connectivity recovery, strictly isolated under `/users/{userId}/`.

## Technical Context

**Language/Version**: Kotlin 2.1.0, Java 11/17, Android SDK 35 (minSdk 26)

**Primary Dependencies**: AndroidX Core, Jetpack Compose, Dagger DI, Firebase Auth (`firebase-auth-ktx`), Cloud Firestore (`firebase-firestore-ktx`), Firebase Storage (`firebase-storage-ktx`), Android Credential Manager (`androidx.credentials`), Jetpack Room (`androidx.room:room-ktx`)

**Storage**: Local Android Jetpack Room SQLite database (`knockledge.db`), App-private internal storage (`context.filesDir/audio/`), Remote Cloud Firestore, Remote Firebase Cloud Storage

**Testing**: JUnit 4, Kotlin Coroutines Test, Room In-Memory Testing (`./gradlew test`)

**Target Platform**: Android 8.0+ (API 26+)

**Project Type**: Native Android Mobile Application Subsystem

**Performance Goals**: Local note persistence $< 200\text{ ms}$; session restoration on launch $< 1\text{ s}$; background cloud sync initiation $< 3\text{ s}$ following network reconnection

**Constraints**: Mandatory login gate; zero unauthenticated recording access; strict user ID query partitioning; 16 kHz WAV audio persistence; automatic upload retry queue

**Scale/Scope**: Single module (`androidApp`), Unit 2 data and authentication subsystem unifying Units 1, 3, and 4

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Minimum Viable Hackathon Build (3-Day Scope)**: PASS. Reuses Firebase Auth, Firestore, and Room without speculative custom backends. Directs email collision to original provider rather than building complex account linking dialogs.
- **Principle II: ADHD-Driven Execution**: PASS. Discrete bounded repository interfaces; concrete quickstart validation scenarios; sequentially ordered tasks.
- **Principle III: Karpathy Discipline**: PASS. Extends existing `data/` package surgically; introduces explicit data contracts; validates via JVM tests before and after edits.
- **Principle IV: Ponytail Simplicity**: PASS. Reuses standard Android platform storage (`filesDir`) and standard AndroidX Room APIs; avoids external storage wrappers.
- **Technology Stack & Architecture Constraints**: PASS. Adheres strictly to MVVM/UDF with StateFlow, Room for caching, Firestore for remote sync, Firebase Auth for identity, and Dagger DI.

## Project Structure

### Documentation (this feature)

```text
specs/002-unit2-firebase-auth-storage/
├── plan.md              # This implementation plan
├── research.md          # Phase 0 technology choices and decisions
├── data-model.md        # Phase 1 unified domain model and storage schemas
├── quickstart.md        # Phase 1 verification scenarios
├── contracts/           # Phase 1 contract schemas and interfaces
│   ├── note-repository-contract.kt
│   ├── auth-repository-contract.kt
│   └── firestore-note-schema.json
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── data/
│   ├── model/
│   │   ├── Note.kt                 # Unified Note aggregate root, AudioMetadata, SummaryMetadata
│   │   ├── UserProfile.kt          # UserProfile domain model
│   │   └── SyncQueueItem.kt        # SyncQueueItem entity
│   ├── local/
│   │   ├── KnockledgeDatabase.kt   # Room database definition
│   │   ├── NoteDao.kt              # Room DAO for note persistence and user filtering
│   │   ├── SyncQueueDao.kt         # Room DAO for offline sync queue
│   │   ├── NoteEntity.kt           # SQLite table representation of Note
│   │   └── NoteTypeConverters.kt   # JSON converters for sentences and chapter markers
│   ├── remote/
│   │   ├── FirebaseAuthSource.kt   # Firebase Auth integration with Credential Manager
│   │   ├── FirestoreDataSource.kt  # Cloud Firestore note document persistence
│   │   └── FirebaseStorageSource.kt# Firebase Storage audio upload client
│   ├── repository/
│   │   ├── NoteRepositoryImpl.kt   # Implementation of NoteRepository contract
│   │   ├── AuthRepositoryImpl.kt   # Implementation of AuthRepository contract
│   │   └── SyncEngine.kt           # Background sync orchestrator with network listener
│   └── Recents.kt                  # Existing legacy wrapper adapted to NoteRepository
├── di/
│   ├── DataModule.kt               # Dagger module providing Room, Firebase, Repositories
│   └── KnockledgeComponent.kt      # Component bindings
└── ui/
    ├── auth/
    │   ├── AuthScreen.kt           # Login and Registration Compose UI (Google & Email)
    │   ├── AuthViewModel.kt        # Auth UDF ViewModel exposing StateFlow<AuthState>
    │   └── AuthState.kt            # Immutable UI state
    └── notes/
        └── NoteListViewModel.kt    # Notes list ViewModel observing NoteRepository

androidApp/src/test/kotlin/two/knockledge/data/
├── NoteRepositoryTest.kt           # Unit tests for cross-unit note creation and updates
├── AuthRepositoryTest.kt           # Unit tests for Google/Email auth and collisions
└── SyncQueueTest.kt                # Unit tests for offline queue and retry transitions
```

**Structure Decision**: Placed in `androidApp/src/main/kotlin/group/two/knockledge/data/` organized into `local`, `remote`, `model`, and `repository` packages, cleanly separating Room storage from Firebase network clients while exposing unified contracts.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations. Architecture strictly follows constitution directives.*
