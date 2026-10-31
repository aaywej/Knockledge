package group.two.knockledge.sensor

/** One-pole high-pass filter; keeps changes and decays a constant input toward zero. */
class Highpass1C {

    private var para = 1.0f
    private var xLast = 0.0f
    private var yLast = 0.0f

    /** Seeds both the input and the output history. */
    fun init(value: Float) {
        xLast = value
        yLast = value
    }

    /** Feed weight in 0..1: larger values pass more of the fast-changing component through. */
    fun setPara(para: Float) {
        this.para = para
    }

    /** Returns the high-passed value and stores the input and output as history. */
    fun update(value: Float): Float {
        val newYLast = (value - xLast) * para + yLast * para
        yLast = newYLast
        xLast = value
        return newYLast
    }
}
