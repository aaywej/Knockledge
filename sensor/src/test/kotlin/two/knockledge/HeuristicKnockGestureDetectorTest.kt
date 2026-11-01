package two.knockledge

import group.two.knockledge.sensor.HeuristicEvaluationMetrics
import group.two.knockledge.sensor.TapRT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeuristicKnockGestureDetectorTest {

    private fun createConfiguredTapRT(): TapRT {
        val tap = TapRT(160_000_000L)
        tap.getLowpassKey().setPara(1f)
        tap.getHighpassKey().setPara(0.3f)
        tap.getPositivePeakDetector().setMinNoiseTolerate(0.15f)
        tap.getPositivePeakDetector().setWindowSize(8)
        tap.getNegativePeakDetection().setMinNoiseTolerate(0.15f)
        tap.getNegativePeakDetection().setWindowSize(8)
        tap.maxTapPeakThreshold = 6.0f
        tap.maxTransverseThreshold = 2.5f
        tap.reset(true)
        return tap
    }

    private fun feedKnockImpulse(tap: TapRT, startTimestampNs: Long, isWarm: Boolean = false): Long {
        var t = startTimestampNs
        val stepNs = 2_500_000L // 2.5ms sampling interval

        // Pre-fill window baseline
        repeat(if (isWarm) 5 else 70) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        // Realistic physical knock waveform (peaks around 1.5 - 2.5f jerk)
        val zSignal = floatArrayOf(
            9.81f, 15f, 22f, 15f, 5f, 0f, 5f, 9.81f
        )
        for (z in zSignal) {
            tap.updateData(1, 0f, 0f, z, t, stepNs, true)
            t += stepNs
        }

        // Post-fill baseline (window size 8 requires 4 samples to reach peak alignment)
        repeat(12) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        return t
    }

    @Test
    fun syntheticDoubleKnock250msTriggersDoubleTap() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L

        // First knock (cold)
        t = feedKnockImpulse(tap, t, isWarm = false)
        assertEquals(1, tap.checkDoubleTapTiming(t), "Should have 1 pending knock")

        // Wait ~150ms
        t += 150_000_000L

        // Second knock (warm)
        t = feedKnockImpulse(tap, t, isWarm = true)
        val timingResult = tap.checkDoubleTapTiming(t)

        assertEquals(2, timingResult, "Double knock within interval should trigger gesture")
        assertEquals(1, metrics.doubleKnocksTriggered)
    }

    @Test
    fun singleKnockTimesOutWithoutSecondKnock() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        t = feedKnockImpulse(tap, t, isWarm = false)

        assertEquals(1, tap.checkDoubleTapTiming(t))

        // Advance timestamp beyond max gap (> 500ms)
        t += 600_000_000L
        val result = tap.checkDoubleTapTiming(t)

        assertEquals(0, result, "Pending knock should expire after 500ms timeout")
        assertEquals(1, metrics.timedOutKnocksCount)
    }

    @Test
    fun continuousLowFrequencySinusoidalMotionProducesNoTriggers() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        val stepNs = 5_000_000L // 200Hz
        val frequencyHz = 2.0 // 2 Hz walking/handling motion
        val totalSeconds = 5.0

        val totalSamples = (totalSeconds * 1000 / 5).toInt()
        for (i in 0 until totalSamples) {
            val timeSec = i * 0.005
            val zAcc = 9.81f + (3.0f * Math.sin(2.0 * Math.PI * frequencyHz * timeSec)).toFloat()
            tap.updateData(1, 0.5f, 0.5f, zAcc, t, stepNs, true)
            tap.checkDoubleTapTiming(t)
            t += stepNs
        }

        assertEquals(0, metrics.doubleKnocksTriggered, "2 Hz motion should produce 0 double knocks")
    }

    @Test
    fun rapidChatterUnder100msIsDebounced() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L

        // First knock (cold)
        t = feedKnockImpulse(tap, t, isWarm = false)

        // Rapid second knock only 20ms later (< 100ms debounce)
        t += 20_000_000L
        t = feedKnockImpulse(tap, t, isWarm = true)

        val timingResult = tap.checkDoubleTapTiming(t)
        assertTrue(timingResult != 2, "Knocks separated by < 100ms must be debounced")
        assertTrue(metrics.suppressedChatterCount >= 1, "Chatter event count should be at least 1")
    }

    @Test
    fun twentyTrialEvaluationBenchmarkReaches85PercentAccuracy() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        var successfulTriggers = 0
        val totalTrials = 20

        repeat(totalTrials) {
            // First knock
            t = feedKnockImpulse(tap, t, isWarm = false)
            tap.checkDoubleTapTiming(t)

            // 150ms gap
            t += 150_000_000L

            // Second knock
            t = feedKnockImpulse(tap, t, isWarm = true)
            if (tap.checkDoubleTapTiming(t) == 2) {
                successfulTriggers++
            }

            // Gap between trials (1 second)
            t += 1_000_000_000L
        }

        val accuracy = successfulTriggers.toDouble() / totalTrials
        assertTrue(accuracy >= 0.85, "True positive rate ($accuracy) must be >= 85%")
        assertEquals(totalTrials, metrics.doubleKnocksTriggered)
    }

    @Test
    fun rapidShakingMotionIsSuppressedAndTriggersZeroDoubleKnocks() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        val stepNs = 2_500_000L // 400Hz
        val shakeFreqHz = 6.0 // 6 Hz rapid hand shake
        val durationSec = 3.0
        val totalSamples = (durationSec * 400).toInt()

        for (i in 0 until totalSamples) {
            val timeSec = i * 0.0025
            val zAcc = 9.81f + (15.0f * Math.sin(2.0 * Math.PI * shakeFreqHz * timeSec)).toFloat()
            val xAcc = (10.0f * Math.cos(2.0 * Math.PI * shakeFreqHz * timeSec)).toFloat()
            val yAcc = (10.0f * Math.sin(2.0 * Math.PI * shakeFreqHz * timeSec)).toFloat()

            tap.updateData(1, xAcc, yAcc, zAcc, t, stepNs, true)
            tap.checkDoubleTapTiming(t)
            t += stepNs
        }

        assertEquals(0, metrics.doubleKnocksTriggered, "Rapid shaking must trigger 0 false double knocks")
    }

    private fun feedHighAmplitudeImpact(tap: TapRT, startTimestampNs: Long): Long {
        var t = startTimestampNs
        val stepNs = 2_500_000L

        repeat(70) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        // Heavy table collision shock (jerk amplitude > 10.0f)
        val zSignal = floatArrayOf(
            9.81f, 35f, 65f, 25f, -15f, -30f, 0f, 9.81f
        )
        for (z in zSignal) {
            tap.updateData(1, 0f, 0f, z, t, stepNs, true)
            t += stepNs
        }

        repeat(12) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        return t
    }

    @Test
    fun highAmplitudeTableImpactRejectedByShockCeiling() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        t = feedHighAmplitudeImpact(tap, t)

        val timingResult = tap.checkDoubleTapTiming(t)
        assertEquals(0, timingResult, "High amplitude table impact should be rejected")
        assertTrue(metrics.shockCeilingExceededCount >= 1, "Shock ceiling exceeded count should be at least 1")
        assertEquals(0, metrics.reboundMatches, "No single knock should be validated")
        assertEquals(0, metrics.doubleKnocksTriggered, "No double knock should be triggered")
    }

    @Test
    fun flickAndRaiseMotionRejectedByTransverseThreshold() {
        val tap = createConfiguredTapRT()
        val metrics = HeuristicEvaluationMetrics()
        tap.evaluationListener = metrics

        var t = 1_000_000_000L
        val stepNs = 2_500_000L

        repeat(70) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        // Arm flick / phone raise generates high transverse motion along Y and X axes
        for (i in 0 until 15) {
            val yAcc = 15.0f * (i / 15.0f)
            val zAcc = 9.81f + 8.0f * (i / 15.0f)
            tap.updateData(1, 8.0f, yAcc, zAcc, t, stepNs, true)
            t += stepNs
        }

        repeat(12) {
            tap.updateData(1, 0f, 0f, 9.81f, t, stepNs, true)
            t += stepNs
        }

        assertEquals(0, metrics.doubleKnocksTriggered, "Arm flick / raise should trigger 0 double knocks")
        assertTrue(
            metrics.transverseMotionExceededCount >= 1,
            "Transverse motion should be caught by transverse motion threshold"
        )
    }
}
