package group.two.knockledge.sensor

/**
 * Diagnostic evaluation listener for assessing heuristic algorithm accuracy.
 */
interface HeuristicEvaluationListener {
    /**
     * Emitted when a positive impact peak is registered.
     */
    fun onPositivePeak(amplitude: Float, windowIndex: Int)

    /**
     * Emitted when a single knock impulse is validated via rebound pairing.
     */
    fun onSingleKnockValidated(timestampNs: Long, reboundDelta: Int)

    /**
     * Emitted when a candidate knock is rejected.
     */
    fun onKnockRejected(reason: RejectionReason)

    /**
     * Emitted when a full double-knock gesture is triggered.
     */
    fun onDoubleKnockTriggered(intervalMs: Long)
}

enum class RejectionReason {
    PEAK_NOT_ALIGNED,
    REBOUND_OUT_OF_BOUNDS,
    DEBOUNCE_TOO_FAST,
    TIMED_OUT,
    SHOCK_EXCEEDS_CEILING,
    TRANSVERSE_MOTION_EXCESSIVE
}
