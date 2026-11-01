# Quickstart: Heuristic Knock Detection Validation

This guide outlines runnable verification steps to validate the heuristic knock detection pipeline.

---

## 1. Prerequisites

- Android SDK installed with `adb` available.
- Connected physical Android device or emulator.
- Gradle build tooling.

---

## 2. Fast JVM Unit Test Verification

Verify the signal processing mathematics, peak detector, slope/jerk, and double-tap timing logic in JVM isolation without requiring hardware:

```powershell
.\gradlew test --tests "two.knockledge.*Knock*"
```

**Expected Result**:
- Tests pass in under 5 seconds.
- Validates slope/jerk output on synthetic acceleration steps.
- Validates that rebound peak intervals outside $[1, 2]$ are rejected.
- Validates double tap detection window ($100\text{ ms} \le \Delta t \le 500\text{ ms}$).

---

## 3. On-Device Synthetic Gesture Simulation

Simulate accelerometer events via adb or unit harness to verify end-to-end trigger integration:

```powershell
# Run instrumented or local integration checks
.\gradlew testDebugUnitTest
```

---

## 4. Manual Physical Evaluation (2-Minute Test)

1. Launch Knockledge on a physical Android test device.
2. Ensure the audio recording service is running.
3. Tap twice firmly on the back of the phone with index finger:
   - Expect immediate haptic vibration confirmation.
   - Expect rolling buffer capture log entry (`KnockTest: double knock detected`).
4. Walk with the phone in pocket for 1 minute:
   - Expect 0 false triggers.
5. If true positive rate exceeds 85% and false positives remain under 1 per hour:
   - Mark heuristic implementation accepted.
6. If accuracy is unsatisfactory:
   - Transition to machine learning classifier pipeline.
