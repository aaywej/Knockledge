# Feature Specification: Custom Notification Layout for Audio Recording

**Feature Branch**: `011-custom-notification-layout`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "let's create a custom notification layout with information about the recording duration, two buttons: rewind and stop, and the cache save count"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recording Notification with Live Duration & Cache Save Count (Priority: P1)

As a user recording continuous audio, I want to see a custom notification card showing live elapsed recording duration, the current cache save count, and explicit Rewind and Stop action buttons so that I can easily monitor status and control recording directly from my lock screen or notification shade.

**Why this priority**: Core user interaction for continuous background recording. Provides status visibility and immediate action triggers without requiring the user to unlock or switch back to the application UI.

**Independent Test**: Can be tested by starting a background recording session and verifying that the ongoing notification displays the live timer, accurate save count, and that tapping Rewind saves a clip while Stop terminates recording.

**Acceptance Scenarios**:

1. **Given** continuous recording is active, **When** I pull down the notification shade or view the lock screen, **Then** I see the custom notification layout displaying live recording duration and current cache save count.
2. **Given** the ongoing recording notification is displayed, **When** I tap the "Rewind" button, **Then** the recent audio buffer is saved as a clip and the displayed cache save count increments by 1.
3. **Given** the ongoing recording notification is displayed, **When** I tap the "Stop" button, **Then** the active recording session ends and the notification is dismissed.

---

### User Story 2 - Real-Time Save Count Synchronization (Priority: P2)

As a user saving audio via physical knock gestures or app controls, I want the ongoing notification's cache save count to update immediately so that the status on the lock screen always reflects my total saved clips.

**Why this priority**: Ensures data consistency between physical gesture interactions and system notification UI.

**Independent Test**: Can be tested by triggering a knock gesture while recording and verifying the notification's save count updates automatically.

**Acceptance Scenarios**:

1. **Given** recording is active with an initial save count of 0, **When** a knock gesture saves a clip, **Then** the notification UI updates the save count to 1 without flickering or restarting the duration timer.

---

### Edge Cases

- What happens when save count is 0? The notification clearly displays "0 saved" or "0 clips" without layout shifting.
- What happens if the Rewind button is tapped before minimum audio buffer is accumulated? The system safely ignores or handles the action without crashing or corrupting recording state.
- How does the system handle high save count values (e.g., > 99 saved clips)? The save count layout badge gracefully contains multi-digit numbers without overlapping adjacent control buttons.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display an ongoing foreground notification with a custom layout while audio recording is in progress.
- **FR-002**: System MUST display live elapsed recording duration on the custom notification layout.
- **FR-003**: System MUST display the current cache save count (total clips saved in the active session) on the custom notification layout.
- **FR-004**: System MUST provide an explicit "Rewind" action button on the notification layout that triggers saving the recent audio buffer when tapped.
- **FR-005**: System MUST provide an explicit "Stop" action button on the notification layout that stops the active recording session when tapped.
- **FR-006**: System MUST update the notification's displayed cache save count in real time whenever a clip is saved via gesture, notification button, or app interaction.
- **FR-007**: Tapping the notification card background (outside action buttons) MUST bring the main application activity to the foreground.

### Key Entities *(include if feature involves data)*

- **Notification Model**: Represents the UI state rendered by the custom notification view (running state, duration start timestamp, cache save count, rewind capability).
- **Session Clip Count**: The integer count tracking total audio segments saved during the current active recording session.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can observe live recording duration and current save count directly from the lock screen or notification shade without opening the app.
- **SC-002**: Tapping Rewind or Stop notification buttons executes the corresponding action within 200ms.
- **SC-003**: Cache save count on the notification updates within 100ms of any clip save event.

## Assumptions

- Custom notification layout uses standard Android `RemoteViews` compatible with system notification builders across supported Android versions.
- Live duration display utilizes standard system timer capabilities (`Chronometer` or formatted elapsed time) to minimize CPU and battery overhead.
- Cache save count tracks the number of saved audio clips in the active session.
