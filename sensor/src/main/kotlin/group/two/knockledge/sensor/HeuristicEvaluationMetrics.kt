package group.two.knockledge.sensor

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
    override fun onPositivePeak(amplitude: Float, windowIndex: Int) {
        positivePeaksDetected++
    }

    override fun onSingleKnockValidated(timestampNs: Long, reboundDelta: Int) {
        reboundMatches++
    }

    override fun onKnockRejected(reason: RejectionReason) {
        when (reason) {
            RejectionReason.DEBOUNCE_TOO_FAST -> suppressedChatterCount++
            RejectionReason.TIMED_OUT -> timedOutKnocksCount++
            RejectionReason.SHOCK_EXCEEDS_CEILING -> shockCeilingExceededCount++
            RejectionReason.TRANSVERSE_MOTION_EXCESSIVE -> transverseMotionExceededCount++
            else -> {}
        }
    }

    override fun onDoubleKnockTriggered(intervalMs: Long) {
        doubleKnocksTriggered++
    }

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
