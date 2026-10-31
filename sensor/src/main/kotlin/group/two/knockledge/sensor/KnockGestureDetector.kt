package group.two.knockledge.sensor

interface KnockGestureDetector {
    fun startListening(onKnockDetected: () -> Unit)

    fun stopListening()

    val isListening: Boolean
}
