# Settings UI Hook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the UI state, controller interface (`SettingsController`), and ViewModel (`SettingsViewModel`) in `ui/settings/` for the settings page without writing Jetpack Compose UI.

**Architecture:** MVVM with Unidirectional Data Flow (UDF). `SettingsViewModel` observes `AppSettings` flows, combines them into an immutable `StateFlow<SettingsUiState>`, and handles user actions. Compose UI widgets remain out of scope for this task.

**Tech Stack:** Kotlin, Coroutines, StateFlow, AndroidX Lifecycle ViewModel, Dagger DI, JUnit4.

## Global Constraints
- **Do not write Jetpack Compose UI**: Restrict scope to state holders, contracts, and ViewModel logic.
- **Local Persistence Only**: Settings persist strictly in `AppSettings` via `SharedPreferences`.
- **ASD-STE100 Rules**: Use active voice, simple sentences, and plain English.
- **Karpathy Discipline**: Surgical additions only. Write reproducing unit tests first.
- **Ponytail Simplicity**: Rely on Kotlin standard library and existing `AppSettings`. Avoid unnecessary abstractions.

---

### Task 1: Create Settings UI Contract and State Holder

**Files:**
- Create: `androidApp/src/main/kotlin/group/two/knockledge/ui/settings/SettingsController.kt`

**Interfaces:**
- Produces:
  - `SettingsUiState`: Immutable data class capturing all settings fields.
  - `SettingsController`: Contract exposing `val uiState: StateFlow<SettingsUiState>` and mutator methods.

- [ ] **Step 1: Define `SettingsUiState` and `SettingsController` interface**
  Create `androidApp/src/main/kotlin/group/two/knockledge/ui/settings/SettingsController.kt` with state fields and action signatures.

---

### Task 2: Implement SettingsViewModel and Unit Tests

**Files:**
- Create: `androidApp/src/test/kotlin/two/knockledge/ui/settings/SettingsViewModelTest.kt`
- Create: `androidApp/src/main/kotlin/group/two/knockledge/ui/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `AppSettings`, `SettingsController`, `SettingsUiState`
- Produces: `SettingsViewModel` implementing `SettingsController` and extending `ViewModel`

- [ ] **Step 1: Write unit tests in `SettingsViewModelTest.kt`**
  Cover initial state emission, audio setting updates, and AI setting updates.
- [ ] **Step 2: Run test to verify failure**
  Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.ui.settings.SettingsViewModelTest"`
- [ ] **Step 3: Implement `SettingsViewModel.kt`**
  Combine `AppSettings` flows and forward mutations.
- [ ] **Step 4: Verify tests pass**
  Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.ui.settings.SettingsViewModelTest"`
