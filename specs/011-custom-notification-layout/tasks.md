# Tasks: Custom Notification Layout for Audio Recording

**Input**: Design documents from `/specs/011-custom-notification-layout/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (`[US1]`, `[US2]`)
- Includes exact file paths for all tasks

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify feature specs and initialize resource layout environment

- [x] T001 Verify project structure and layout resource path availability in `androidApp/src/main/res/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core drawables, strings, and dimension XML resources required for notification layouts

- [x] T002 [P] Create rounded badge background drawable in `androidApp/src/main/res/drawable/bg_notification_badge.xml`
- [x] T003 [P] Add string resources (`notification_save_count_format`, `notification_action_rewind`, `notification_action_stop`) to `androidApp/src/main/res/values/strings.xml`
- [x] T004 [P] Add notification dimension constants to `androidApp/src/main/res/values/dimens.xml`

---

## Phase 3: User Story 1 - Recording Notification with Live Duration & Cache Save Count (Priority: P1) — MVP

**Goal**: Render a custom foreground service notification displaying live recording duration, save count badge, and Rewind/Stop buttons.

**Independent Test**: Start a recording session and verify that the ongoing notification displays the live ticking timer, save count, and interactive Rewind and Stop buttons.

### Implementation for User Story 1

- [x] T005 [P] [US1] Create compact notification layout in `androidApp/src/main/res/layout/notification_recording_compact.xml` featuring title, `<Chronometer>`, save count badge `TextView`, and `ImageButton`s for Rewind and Stop
- [x] T006 [P] [US1] Create expanded notification layout in `androidApp/src/main/res/layout/notification_recording_expanded.xml` with detailed status layout
- [x] T007 [US1] Refactor `build(model: NotificationModel)` in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt` to instantiate `RemoteViews` for compact/expanded views and bind `<Chronometer>` base time
- [x] T008 [US1] Bind PendingIntents for `ACTION_REWIND`, `ACTION_STOP`, and `MainActivity` launch intent on the `RemoteViews` elements in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt`

---

## Phase 4: User Story 2 - Real-Time Save Count Synchronization (Priority: P2)

**Goal**: Ensure notification save count badge updates immediately whenever a new audio clip is saved.

**Independent Test**: Trigger a knock gesture or Rewind button while recording and confirm the notification's save count increments in real time.

### Implementation for User Story 2

- [x] T009 [US2] Update `NotificationModel` and state collection in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeService.kt` to pass `savedCount` into `RemoteViews` text binding and re-post `notify()` when `savedCount` changes

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validation and verification

- [x] T010 [P] Add JVM unit test for notification state binding in `androidApp/src/test/kotlin/group/two/knockledge/KnockledgeServiceTest.kt`
- [ ] T011 Run manual quickstart verification workflow on Android emulator/device per `quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) - BLOCKS User Story 1.
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2).
- **User Story 2 (Phase 4)**: Depends on User Story 1 (Phase 3).
- **Polish (Phase 5)**: Depends on User Stories 1 and 2.

### Parallel Opportunities
- T002, T003, T004 (Drawables, strings, dimens) can run in parallel.
- T005 and T006 (Compact and expanded layout XMLs) can run in parallel once Foundational phase is complete.

---

## Implementation Strategy (MVP First)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational resources).
2. Complete Phase 3 (User Story 1 - Custom notification layout & service integration).
3. **Validate MVP**: Test live duration timer and Rewind/Stop actions on notification shade.
4. Complete Phase 4 (User Story 2 - Real-time save count synchronization).
5. Complete Phase 5 (Polish & Quickstart manual verification).
