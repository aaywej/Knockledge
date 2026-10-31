package group.two.knockledge.sensor

/** Mutable counters of heuristic outcomes, pluggable as a [HeuristicEvaluationListener] to measure detection accuracy. */
data class HeuristicEvaluationMetrics(
    var totalSensorEvents: Long = 0L,
    var positivePeaksDetected: Int = 0,
    var reboundMatches: Int = 0,
    var doubleKnocksTriggered: Int = 0,
    var suppressedChatterCount: Int = 0,
    var timedOutKnocksCount: Int = 0,
    var shockCeilingExceededCount: Int = 0,
    var transverseMotionExceededCount: Int = 0
) : HeuristicEvaluationListener {
    /** Counts the peak; amplitude and window index are not recorded. */
    override fun onPositivePeak(amplitude: Float, windowIndex: Int) {
        positivePeaksDetected++
    }

    /** Counts the validation; timestamp and rebound offset are not recorded. */
    override fun onSingleKnockValidated(timestampNs: Long, reboundDelta: Int) {
        reboundMatches++
    }

    /** Counts the rejection under its reason; reasons without a counter are ignored. */
    override fun onKnockRejected(reason: RejectionReason) {
        when (reason) {
            RejectionReason.DEBOUNCE_TOO_FAST -> suppressedChatterCount++
            RejectionReason.TIMED_OUT -> timedOutKnocksCount++
            RejectionReason.SHOCK_EXCEEDS_CEILING -> shockCeilingExceededCount++
            RejectionReason.TRANSVERSE_MOTION_EXCESSIVE -> transverseMotionExceededCount++
            else -> {}
        }
    }

    /** Counts the trigger; the interval is not recorded. */
    override fun onDoubleKnockTriggered(intervalMs: Long) {
        doubleKnocksTriggered++
    }

    /** Zeroes every counter. */
    fun reset() {
        totalSensorEvents = 0L
        positivePeaksDetected = 0
        reboundMatches = 0
        doubleKnocksTriggered = 0
        suppressedChatterCount = 0
        timedOutKnocksCount = 0
        shockCeilingExceededCount = 0
        transverseMotionExceededCount = 0
    }
}
