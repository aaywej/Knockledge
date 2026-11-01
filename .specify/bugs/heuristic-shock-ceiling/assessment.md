# Bug Assessment: Heuristic False Triggers on Surface Impacts

- **Slug**: heuristic-shock-ceiling
- **Created**: 2026-10-31T12:43:00Z
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> "the knock also triggers when i hit it against a table or any solid surface"
> "we stick with heuristic method"

The user reports that striking the device against a table or any solid surface triggers the knock capture action. The user selected the heuristic approach to remediate this issue rather than migrating to a machine learning classifier.

## Symptom

Striking the phone against a solid surface (such as a table) triggers the double-knock gesture detection. The system should distinguish localized finger knocks on the phone casing from whole-device impacts against external surfaces and suppress the latter.

## Reproduction

1. Start Knockledge on a physical Android test device with active background sensor listening.
2. Strike the device body firmly against a solid desk or table surface twice within 500 milliseconds.
3. Observe that the device emits haptic vibration and registers an audio capture event.

## Suspected Code Paths

- `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt:159-174` — `recognizeTapHeuristic()` enforces only a minimum amplitude threshold (`maxPeakAmplitude >= 0.5f`) and has no upper bound ceiling.
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt:135-157` — `processKeySignalHeursitic()` only processes the Z-axis jerk signal, ignoring transverse energy on the X and Y axes.
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt:42-48` — Configures filter parameters and noise tolerances but lacks an upper shock boundary.
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationListener.kt:28-33` — `RejectionReason` lacks an enumeration for excessive shock or ceiling violations.

## Root Cause Hypothesis

The heuristic tap classifier relies exclusively on a lower-bound threshold (`maxPeakAmplitude >= 0.5f`) and rebound index delta (`negativeIdMajorPeak in 1..2`) on the filtered Z-axis jerk signal. A whole-device impact against a table produces an identical impulse shape (compression followed by rebound) but at a drastically higher amplitude (> 3.5f up to 15f+) and with high cross-axis energy across X and Y. Because no ceiling or 3D kinetic energy constraint exists, whole-device impacts satisfy all conditions and trigger false double-knock gestures.

**Confidence**: High

## Proposed Remediation

**Preferred**:
Introduce a calibrated upper shock ceiling (`MAX_TAP_PEAK_THRESHOLD`, e.g. 3.5f) in `TapRT.recognizeTapHeuristic()`. Finger knocks produce local casing vibration peaks between 0.5f and 3.0f, whereas whole-device surface strikes generate peaks well above 3.5f. Add `SHOCK_EXCEEDS_CEILING` to `RejectionReason` to reject high-energy collisions. Additionally, check that cross-axis motion on X and Y does not exhibit massive simultaneous jerk.

**Alternatives**:
- Calculate total 3D vector jerk magnitude $\sqrt{j_x^2 + j_y^2 + j_z^2}$ and require Z-axis dominance ratio ($j_z / \text{total} \ge 0.7$).
- Pre-impact stillness window: verify the device was stationary (near 0 velocity change) in the 50ms immediately preceding the strike.

**Files likely to change**:
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt`
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationListener.kt`
- `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt`
- `androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt`

**Tests to add or update**:
- Add unit test `highAmplitudeTableImpactRejectedByShockCeiling` verifying that peak amplitudes $> 3.5f$ are rejected with `RejectionReason.SHOCK_EXCEEDS_CEILING` and do not increment the tap counter.
- Verify existing double-knock unit tests (`syntheticDoubleKnock250msTriggersDoubleTap`) remain passing.

## Risks & Considerations

- Setting the ceiling too low could reject aggressive finger knocks; calibration around 3.5f–4.0f preserves normal finger knocks while filtering table collisions.
- Thick shock-absorbing phone cases may slightly lower table impact peaks, but the multi-axis check provides secondary protection.

## Open Questions

- None. The upper amplitude boundary and cross-axis validation provide direct heuristic suppression.
