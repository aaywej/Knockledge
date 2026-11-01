package group.two.knockledge.sensor

/** Contract for the tap detectors built on the [EventIMURT] pipeline. */
interface BaseTapRT {

    /** Feeds one raw sample of [type] with timestamp [lastT]; [interval] is the expected resampling grid in nanoseconds. */
    fun updateData(type: Int, lastX: Float, lastY: Float, lastZ: Float, lastT: Long, interval: Long, isHeuristic: Boolean)
    /** Returns how many taps are pending at [timestamp]: 2 after a confirmed pair, 1 for a single tap, 0 otherwise. */
    fun checkDoubleTapTiming(timestamp: Long): Int
    /** Discards detection state so a new stream can start; [justClearFv] keeps the retained sample buffer instance. */
    fun reset(justClearFv: Boolean)

}
