package group.two.knockledge.sensor

/** Source of knock gestures, decoupled from the sensor API so callers can substitute a fake. */
interface KnockGestureDetector {
    /** Starts listening and calls [onKnockDetected] for each confirmed knock; repeated calls while listening do nothing. */
    fun startListening(onKnockDetected: () -> Unit)

    /** Stops listening and releases sensor resources; safe to call when not listening. */
    fun stopListening()

    /** Whether a sensor listener is currently registered. */
    val isListening: Boolean
}
