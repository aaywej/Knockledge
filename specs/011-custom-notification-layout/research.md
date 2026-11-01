# Research & Design Decisions: Custom Notification Layout

**Feature**: Custom Notification Layout for Audio Recording
**Spec**: [spec.md](./spec.md)
**Design System/Framework**: Impeccable (Operate Mode - Native Android Notification UI)

## 1. Notification Layout Architecture

### Decision
Use Android `RemoteViews` with custom XML layouts (`R.layout.notification_recording_compact` and `R.layout.notification_recording_expanded`) set via `Notification.Builder.setCustomContentView()` and `Notification.Builder.setCustomBigContentView()`.

### Rationale
- `RemoteViews` is the standard Android API for rendering custom layouts inside the system notification shade and lock screen.
- Allows precise control over typography, spacing, icon placement, and button styling while respecting OS-level notification card containers across vendor skins (OneUI, HyperOS, Stock Android).

### Alternatives Considered
- Standard `Notification.Builder.addAction()`: Rejected because standard action buttons render as small text-icon rows at the bottom of the card, lacking the side-by-side circular button layout and integrated save count badge requested.
- `Notification.MediaStyle`: Rejected because MediaStyle forces media playback controls (play/pause/next) and album art metadata formatting rather than custom recording metrics and save count badges.

---

## 2. Real-Time Elapsed Duration Display

### Decision
Use `<Chronometer>` inside the `RemoteViews` XML layout configured with `SystemClock.elapsedRealtime()` base time.

### Rationale
- `<Chronometer>` updates the seconds counter on-device directly within the System UI process.
- Prevents posting IPC `NotificationManager.notify()` updates every second, eliminating CPU wake-ups, power drain, and notification update lag.
- State updates from `KnockledgeService` only re-post `notify()` when state attributes change (e.g., `savedCount` increments, `canRewind` state toggles, or session stops).

---

## 3. Impeccable Design & Visual Hierarchy (Operate Mode)

### Design Direction
- **Mode**: Operate (Glanceable, high contrast, task-focused, min 48dp touch targets).
- **Primary Focus**: Live elapsed time (`00:14`) rendered in prominent monospace/numeric font.
- **Secondary Indicator**: Compact pill badge showing current session cache save count (e.g., `3 saved` or `3 clips`).
- **Action Buttons**:
  - **Rewind**: Accent-colored action icon button (e.g., rewind icon with filled/semi-transparent background container) for immediate clip saving.
  - **Stop**: High-contrast stop icon button (e.g., square or checkmark icon in circular container) for ending the session.
- **Background & Margins**: Transparent layout background inside `RemoteViews` to allow native OS notification card styling, dark/light theme adaptivity, and glassmorphism.

---

## 4. State Management & Data Flow

### Decision
`KnockledgeController` exposes `StateFlow<KnockledgeState>` containing `savedCount: Int` (or derived from saved items list). `KnockledgeService` collects state updates and updates `RemoteViews` text/button pending intents.

### Actions & Pending Intents
- **Rewind Button**: Fires `PendingIntent.getService` with action `ACTION_REWIND`.
- **Stop Button**: Fires `PendingIntent.getService` with action `ACTION_STOP`.
- **Card Tap**: Fires `PendingIntent.getActivity` targeting `MainActivity` with `FLAG_ACTIVITY_SINGLE_TOP`.
