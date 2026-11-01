# Bug Fix: Heuristic False Triggers on Surface Impacts and Arm Flicks

- **Slug**: heuristic-shock-ceiling
- **Fixed**: 2026-10-31T13:07:00Z
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Implemented transverse motion suppression and calibrated shock ceilings in `TapRT.kt` and `AndroidKnockGestureDetector.kt`. Table impacts are rejected by the 6.0f shock ceiling, while arm flicks and raising the phone are rejected by the 2.5f transverse jerk threshold.

## Changes

| File | Change | Notes |
|---|---|---|
| `androidApp/src/main/kotlin/group/two/knockledge/sensor/AndroidKnockGestureDetector.kt` | modified | Configured `maxTapPeakThreshold = 6.0f` and `maxTransverseThreshold = 2.5f`. |
| `androidApp/src/main/kotlin/group/two/knockledge/sensor/columbus/TapRT.kt` | modified | Added transverse jerk tracking along X/Y axes (`_xsAcc`) and integrated shock ceiling and transverse suppression into `recognizeTapHeuristic()`. |
| `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationListener.kt` | modified | Added `TRANSVERSE_MOTION_EXCESSIVE` to `RejectionReason`. |
| `androidApp/src/main/kotlin/group/two/knockledge/sensor/HeuristicEvaluationMetrics.kt` | modified | Added counter tracking for `transverseMotionExceededCount`. |
| `androidApp/src/test/kotlin/two/knockledge/HeuristicKnockGestureDetectorTest.kt` | modified | Added regression tests for table collisions and arm flicks; verified all 8 unit tests pass. |

## Diff Highlights

```kotlin
// TapRT.kt
val transverseJerk = kotlin.math.sqrt(slopeAcc.x * slopeAcc.x + slopeAcc.y * slopeAcc.y)
_xsAcc.add(transverseJerk)
...
if (maxPeakAmplitude > maxTapPeakThreshold) {
    _result = TapClass.Others.ordinal
    evaluationListener?.onKnockRejected(RejectionReason.SHOCK_EXCEEDS_CEILING)
} else if (maxTransverse > maxTransverseThreshold) {
    _result = TapClass.Others.ordinal
    evaluationListener?.onKnockRejected(RejectionReason.TRANSVERSE_MOTION_EXCESSIVE)
} else if (maxPeakAmplitude >= 0.5f && negativeIdMajorPeak in 1..2) {
    _result = TapClass.Back.ordinal
    evaluationListener?.onSingleKnockValidated(_zsAcc.size.toLong(), negativeIdMajorPeak)
}
```

## Tests Added or Updated

- `HeuristicKnockGestureDetectorTest::highAmplitudeTableImpactRejectedByShockCeiling` — Verifies rejection of solid surface impacts.
- `HeuristicKnockGestureDetectorTest::flickAndRaiseMotionRejectedByTransverseThreshold` — Verifies rejection of arm flicks and phone raises.
- `HeuristicKnockGestureDetectorTest::syntheticDoubleKnock250msTriggersDoubleTap` — Verifies valid finger knocks detect properly.

## Local Verification

- Commands run: `.\gradlew test` → BUILD SUCCESSFUL in 7s (94 tests passed, 0 failed).
