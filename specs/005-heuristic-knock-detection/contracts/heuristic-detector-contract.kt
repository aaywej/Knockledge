package group.two.knockledge.sensor

/**
 * Common contract for knock gesture detection implementations.
 */
interface KnockGestureDetector {
    /**
     * Commences active sensor monitoring.
     * @param onKnockDetected Invoked when a valid double-knock gesture is confirmed.
     */
    fun startListening(onKnockDetected: () -> Unit)

    /**
     * Halts sensor sampling and unregisters listeners.
     */
    fun stopListening()

    /**
     * Indicates whether the detector is currently registered to hardware sensors.
     */
    val isListening: Boolean
}

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
    TIMED_OUT
}
