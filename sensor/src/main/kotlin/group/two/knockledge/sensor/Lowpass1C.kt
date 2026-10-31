package group.two.knockledge.sensor

/** One-pole low-pass filter; each output blends the new sample with the previous output. */
class Lowpass1C {

    private var para = 1.0f
    private var xLast = 0.0f

    /** Seeds the previous output so the first update starts from a known value. */
    fun init(arg1: Float) {
        xLast = arg1
    }

    /** Smoothing weight in 0..1: 1 passes the input unchanged, values near 0 smooth heavily. */
    fun setPara(arg1: Float) {
        para = arg1
    }

    /** Returns the filtered value and keeps it as the previous output for the next call. */
    fun update(value: Float): Float {
        val newXLast = value * para + (1.0f - para) * xLast
        xLast = newXLast
        return newXLast
    }
}
