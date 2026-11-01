package group.two.knockledge.sensor

/** First difference of a scalar signal, with an optional per-call scale factor. */
class Slope1C {

    private var xDelta = 0.0f
    private var xRawLast = 0f

    /** Records the value treated as the previous sample. */
    fun init(x: Float) {
        xRawLast = x
    }

    /**
     * Returns `value * d` minus the previous scaled value and stores it as the new baseline.
     *
     * @param d scale used to compensate for a changed sample interval.
     */
    fun update(value: Float, d: Float): Float {
        val x = value * d
        val delta = x - xRawLast
        xDelta = delta
        xRawLast = x
        return delta
    }
}
