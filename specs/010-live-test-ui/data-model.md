# Data Model: Utilitarian Material 3 Live Test UI

**Feature**: `010-live-test-ui`  
**Date**: 2026-11-01  
**Status**: Completed

## 1. UI Navigation & Session Entities

### LiveTestTab (Enum)
Represents the primary utilitarian navigation destinations.

| Name | Display Label | Description |
|---|---|---|
| `RECORD` | "Record & Knock" | Audio buffer progress, recording controls, manual capture, knock sensor pulse |
| `NOTES` | "Notes & Review" | Note list, audio playback controls, chapter navigation, transcript/summary details |
| `SETTINGS` | "Settings & Auth" | User authentication, auto-AI toggle, buffer window slider, sensor threshold |

---

### LiveTestUiState (Data Class)
State model for the top-level test screen shell.

| Field | Type | Description |
|---|---|---|
| `activeTab` | `LiveTestTab` | Currently displayed utilitarian test tab |
| `selectedNoteId` | `String?` | ID of the note currently selected for playback and inspection |
| `lastKnockEpochMs` | `Long?` | Timestamp of the most recent physical or simulated knock event |
| `knockCount` | `Int` | Total number of knock triggers detected in current session |
| `hasRecordAudioPermission` | `Boolean` | Whether Android `RECORD_AUDIO` runtime permission is granted |

---

### KnockSensorStatus (Data Class)
Real-time sensor event indicator for on-device knock verification.

| Field | Type | Description |
|---|---|---|
| `isListening` | `Boolean` | Whether the hardware accelerometer listener is actively polling |
| `lastTriggerTimeFormatted` | `String?` | Human-readable time of the last knock (e.g. "12:34:56.789") |
| `isRecentPulse` | `Boolean` | True if a knock occurred within the last 1500 ms (triggers visual pulse) |

---

## 2. Note Presentation Model

### NoteItemUiModel (Data Class)
Lightweight presentation projection of a `Note` entity for the utilitarian list view.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique note identifier |
| `title` | `String` | Note title (or fallback title if empty) |
| `durationFormatted` | `String` | Formatted audio duration (e.g., "0:30") |
| `createdAtFormatted` | `String` | Formatted date/time of recording |
| `hasAudioFile` | `Boolean` | True if the local audio file exists on disk |
| `hasTranscript` | `Boolean` | True if speech-to-text transcript is attached |
| `hasSummary` | `Boolean` | True if AI executive summary is attached |
| `chapterCount` | `Int` | Number of chapter markers identified in the audio |

---

## 3. State Transitions

```
[Screen Launch]
      │
      ▼
Check RECORD_AUDIO Permission ──(Denied)──► Display Permission Request Banner
      │
   (Granted)
      │
      ▼
Load Active User ──(Signed Out)──► Default Tab = RECORD (Local buffer operational)
      │                            Prompt optional test sign-in for cloud sync
   (Signed In)
      │
      ▼
Observe Notes Flow from NoteRepository(userId)
      │
[User Actions]:
  ├── Select Tab ───────────────► Update `activeTab`
  ├── Tap "Start Recording" ────► Call `controller.start()`
  ├── Knock Device ─────────────► Pulse `lastKnockEpochMs` + Call `controller.rewind()`
  ├── Tap Note Item ────────────► Update `selectedNoteId` + Call `playbackViewModel.loadNote()`
  ├── Tap "Run AI Processing" ──► Call `processingViewModel.processNote(noteId)`
  └── Tap "Sign Out" ───────────► Call `authViewModel.signOut()`
```
