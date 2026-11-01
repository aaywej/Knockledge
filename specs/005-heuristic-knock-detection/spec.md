# Feature Specification: Heuristic Knock Detection

**Feature Branch**: `005-heuristic-knock-detection`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "we'll port this non machine learning detection first and if does not work well or if im not satisfied, we will do machine learning detection."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Physical Double Knock Audio Capture (Priority: P1)

A user listens to a presentation and wants to save the preceding conversation segment. The user knocks twice firmly on the device or supporting desk. The system detects the impact shocks through acceleration jerk and rebound peak tracking. The system then issues an audio capture trigger immediately.

**Why this priority**: Deliberate physical knock detection provides the core hands-free recording trigger required for the primary user workflow.

**Independent Test**: Perform a physical double knock on the phone body while the background audio service is active. Verify that the system registers the gesture and issues a capture trigger within 150 milliseconds.

**Acceptance Scenarios**:

1. **Given** an active audio recording session, **When** the user performs two firm knocks within 500 milliseconds, **Then** the system detects a valid double-knock gesture.
2. **Given** a detected double knock, **When** gesture validation completes, **Then** the system triggers audio buffer saving within 150 milliseconds.
3. **Given** a valid knock capture, **When** the trigger fires, **Then** the system provides immediate feedback to confirm detection.

---

### User Story 2 - Motion Noise and False Trigger Suppression (Priority: P1)

A user carries the phone in a bag or pocket while walking, climbing stairs, or setting the phone on a table. The system analyzes motion rates of change and ignores continuous vibrations or single bumps without triggering an unwanted audio capture.

**Why this priority**: Preventing false triggers preserves storage space and stops unnecessary audio clip creation during routine daily activities.

**Independent Test**: Place the device in a pocket and walk for 15 minutes. Check the audio library to confirm zero unintended recording captures occurred.

**Acceptance Scenarios**:

1. **Given** an active monitoring session, **When** the device experiences continuous low-frequency motion such as walking, **Then** the system filters out the motion and does not trigger audio capture.
2. **Given** a single bump or surface contact, **When** no second knock occurs within 500 milliseconds, **Then** the system discards the initial strike without issuing a capture event.
3. **Given** rapid device shaking, **When** peak rebound signatures do not match physical knock profiles, **Then** the system rejects the sensor event.

---

### User Story 3 - Accuracy Evaluation and Model Escalation Gate (Priority: P2)

A developer or tester assesses detection reliability during hands-on evaluation. If the mathematical heuristic detection achieves acceptable accuracy, the team retains the lightweight implementation. If detection accuracy is insufficient or produces excessive false positives, the team flags the module for machine-learning classifier integration.

**Why this priority**: Establishing a quantifiable evaluation gate ensures the team delivers a working MVP without premature complexity while defining a clear escalation path.

**Independent Test**: Execute 20 intentional double knocks and 20 normal movement handling actions. Record true positive and false positive rates against acceptance thresholds.

**Acceptance Scenarios**:

1. **Given** 20 intentional double-knock attempts on a desk surface, **When** the heuristic algorithm evaluates the motion data, **Then** the system successfully detects at least 17 attempts (85% accuracy).
2. **Given** 60 minutes of everyday phone handling and transit, **When** the heuristic algorithm runs continuously, **Then** the system records no more than 1 false positive activation.
3. **Given** evaluation results that fail the accuracy threshold, **When** the user reviews satisfaction metrics, **Then** the system specification guides transition to a machine-learning sensor pipeline.

---

### Edge Cases

- What happens when a user knocks rapidly more than twice in under 100 milliseconds? The system enforces a minimum 100 millisecond debounce interval and treats chatter as a single event.
- What happens when a user knocks on a very soft surface such as a mattress? The impact impulse dampens below the minimum noise threshold and the system ignores the motion.
- What happens when the device changes orientation while knocking? The algorithm normalizes three-dimensional motion signals so physical orientation does not prevent detection.
- What happens when the battery enters power saver mode? The system maintains continuous sensor sampling within system battery governance constraints.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST compute the time derivative of acceleration (jerk) from consecutive motion sensor samples.
- **FR-002**: System MUST apply low-pass and high-pass filtering to isolate impact frequencies and eliminate gravity baseline offsets.
- **FR-003**: System MUST monitor motion signals for both positive acceleration peaks and corresponding negative rebound peaks.
- **FR-004**: System MUST confirm a single knock impulse only when a negative rebound peak follows a positive peak within 1 to 2 sample cycles.
- **FR-005**: System MUST require exactly two confirmed knock impulses separated by at least 100 milliseconds and at most 500 milliseconds to trigger a double knock.
- **FR-006**: System MUST issue an audio buffer capture trigger within 150 milliseconds of confirming the second knock impulse.
- **FR-007**: System MUST provide an evaluation mode to log detection events and false positives for accuracy assessment.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: System achieves at least 85% true positive detection rate across 20 consecutive deliberate double knocks on hard surfaces.
- **SC-002**: System produces fewer than 2 false trigger activations per hour of standard active device handling.
- **SC-003**: System completes gesture recognition and issues the capture trigger in under 150 milliseconds following the second knock.
- **SC-004**: Gesture monitoring operates with zero noticeable user interface lag or frame rate drops.

## Assumptions

- The target mobile device provides an onboard accelerometer with a minimum sampling frequency of 50 Hz.
- The initial lightweight heuristic implementation precedes machine learning to minimize computational overhead and dependency size.
- If the heuristic approach fails to meet the 85% accuracy target during validation, the project transitions to a machine learning classifier.
- The existing audio caching module provides an event receiver that consumes the gesture trigger signal.
