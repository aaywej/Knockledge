# Phase 1 Data Model: Configurable AI Processing and Manual Trigger

## Entity & State Definitions

### 1. AI Processing Settings

Configuration properties added to [`AppSettings`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt):

| Property | Type | Default | Description |
|---|---|---|---|
| `autoAiProcessingEnabled` | `StateFlow<Boolean>` | `false` | When `true`, buffer save runs transcription and summarization immediately. When `false`, saving only stores raw audio. |
| `setAutoAiProcessingEnabled(enabled: Boolean)` | Method | - | Persists value to `KeyValueStore` and updates `_autoAiProcessingEnabled`. |

Storage key:
- `KEY_AUTO_AI_PROCESSING = "auto_ai_processing_enabled"`

### 2. Note AI Processing State

Represents the UI lifecycle state when generating a transcript and summary for an existing note:

```kotlin
enum class NoteAiProcessingState {
    /** Note has audio but no AI processing is in flight. */
    IDLE,

    /** Audio is currently being processed by speech recognition. */
    TRANSCRIBING,

    /** Transcript is being processed by the language model. */
    SUMMARIZING,

    /** Processing succeeded; transcript and summary are attached. */
    COMPLETED,

    /** Processing encountered an error; retry is permitted. */
    FAILED
}
```

### 3. Note Entity Relationship

The existing `Note` model in [`NoteModels.kt`](file:///D:/General%20Project%20Bins/Academics/CIT306/Knockledge/androidApp/src/main/kotlin/group/two/knockledge/data/model/NoteModels.kt) already supports nullable `transcript` and `summary`:

```text
+-------------------------------------------------------+
|                         Note                          |
+-------------------------------------------------------+
| id: String                                            |
| userId: String                                        |
| title: String                                         |
| createdAtEpochMs: Long                                |
| audio: AudioMetadata (file, duration, sample rate)    |
| transcript: Transcript? (null when unprocessed)       |
| summary: Summary? (null when unprocessed)             |
| syncStatus: SyncStatus                                |
+-------------------------------------------------------+
```

### State Transitions

```text
[Audio Captured & Saved]
         │
         ▼
       IDLE (transcript = null, summary = null)
         │
         │  User taps "Process AI Notes"
         ▼
    TRANSCRIBING
         │
         ├─── (Error / Timeout) ───► FAILED ─── (User taps Retry) ───┐
         │                                                           │
         ▼                                                           │
    SUMMARIZING ◄────────────────────────────────────────────────────┘
         │
         ├─── (Error / Timeout) ───► FAILED
         │
         ▼
     COMPLETED (transcript != null, summary != null)
```
