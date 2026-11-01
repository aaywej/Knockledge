# Bug Verification: Heuristic False Triggers on Surface Impacts

- **Slug**: heuristic-shock-ceiling
- **Tested**: 2026-10-31T12:51:00Z
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The high-amplitude surface impact false trigger issue was tested and verified. High-energy whole-device impacts exceeding the upper shock ceiling (`MAX_TAP_PEAK_THRESHOLD = 35.0f` m/s²) are successfully suppressed and logged with `RejectionReason.SHOCK_EXCEEDS_CEILING`. All 92 unit tests pass without regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `two.knockledge.HeuristicKnockGestureDetectorTest::highAmplitudeTableImpactRejectedByShockCeiling` | pass | High amplitude surface impacts (> 35.0f m/s²) are rejected and produce 0 triggers |
| New / updated tests | `./gradlew test --tests "two.knockledge.HeuristicKnockGestureDetectorTest"` | pass | Verified `highAmplitudeTableImpactRejectedByShockCeiling` passes |
| Regression suite | `./gradlew test` | pass | 92 passed, 0 failed across all module unit tests |

## Output Excerpts

```text
BUILD SUCCESSFUL
:androidApp:testDebugUnitTest -> 92 passed, 0 skipped, 0 failed
```

## Residual Risks

- Physical test on real hardware recommended across various phone case materials to confirm 35.0f m/s² threshold calibration under physical dampening.

## Recommendation

Close the bug — verified with automated test suite and regression suite passing 100%.
