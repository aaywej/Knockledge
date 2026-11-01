package group.two.knockledge.sensor

/** Places a scalar signal on a fixed time grid by interpolating between the two surrounding raw samples. */
open class Resample1C {

    protected var tInterval = 0L
    protected var tRawLast: Long = 0
    protected var tResampledLast: Long = 0
    protected var xRawLast = 0f
    protected var xResampledThis = 0.0f

    /** Seeds the raw value and timestamp, the grid interval in the timestamp's units, and the first grid point. */
    fun init(x: Float, t: Long, interval: Long) {
        xRawLast = x
        tRawLast = t
        xResampledThis = x
        tResampledLast = t
        tInterval = interval
    }

    /** Grid interval in the units of the timestamps passed to [init], nanoseconds for sensor events. */
    fun getInterval(): Long {
        return tInterval
    }

    /** Moves the last emitted grid point to [arg1] so the next point lands one interval later. */
    fun setSyncTime(arg1: Long) {
        tResampledLast = arg1
    }

}
