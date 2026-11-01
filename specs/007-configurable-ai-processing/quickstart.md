# Quickstart Validation: Configurable AI Processing and Manual Trigger

This guide validates the configurable automatic AI processing setting and the on-demand note processing workflow.

## Prerequisites

- Android SDK installed with `android` CLI or Gradle wrapper available.
- Existing unit test suite passes: `./gradlew testDebugUnitTest`.

## Verification Scenarios

### Scenario 1: Default Settings Verification (Automated Unit Test)

Prove that automatic AI processing is disabled by default upon new configuration initialization.

```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.AppSettingsTest"
```

**Expected Outcome**:
- `settings.autoAiProcessingEnabled.value` evaluates to `false`.
- Calling `settings.setAutoAiProcessingEnabled(true)` updates the value to `true` and persists across reloads.

---

### Scenario 2: Save Without Processing (Integration / Unit Test)

Prove that capturing an audio buffer when `autoAiProcessingEnabled` is `false` writes the note entity and audio file without invoking the transcriber or summarizer.

```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.ConfigurableAiSaveTest"
```

**Expected Outcome**:
- An audio snapshot is written to storage.
- The note is persisted in Room with `syncStatus = LOCAL_ONLY`.
- Note `transcript` and `summary` are `null`.
- Neither `transcriber.transcribe()` nor `summarizer.summarize()` is called.

---

### Scenario 3: On-Demand Note Processing Execution

Prove that invoking on-demand processing on an existing unprocessed audio note executes transcription and summarization and attaches them to the note.

```powershell
./gradlew testDebugUnitTest --tests "two.knockledge.NoteProcessingViewModelTest"
```

**Expected Outcome**:
- `NoteProcessingController.processNote(noteId)` transitions through `RewindStage.Transcribing` then `RewindStage.Summarizing`.
- The note record in Room is updated with non-null `transcript` and `summary`.
- `processingState.value.isSuccess` becomes `true`.

---

### Scenario 4: UI Processing Controls & States (Compose Preview & Inspection)

Prove that the note view provides a "Process Note" action when unprocessed, displays progress during execution, and reveals transcript/summary when completed.

```powershell
./gradlew compileDebugKotlin
```

**Expected Outcome**:
- Stateless Compose previews compile cleanly and demonstrate IDLE, PROCESSING, COMPLETED, and FAILED states.
