package group.two.knockledge.sensor

/** Places 3-axis samples on a fixed time grid by linear interpolation, one [Resample1C] per axis. */
class Resample3C : Resample1C() {

    private var yRawLast = 0f
    private var yResampledThis = 0f
    private var zRawLast = 0f
    private var zResampledThis = 0f

    /** Last interpolated sample; unchanged until the next grid point is emitted. */
    val results: Sample3C
        get() = Sample3C(xResampledThis, yResampledThis, zResampledThis, tResampledLast)

    /** Seeds the grid interval and all three axes with the first sample. */
    fun init(x: Float, y: Float, z: Float, t: Long, interval: Long) {
        this.init(x, t, interval)
        yRawLast = y
        zRawLast = z
        yResampledThis = y
        zResampledThis = z
    }

    /**
     * Feeds one raw sample and interpolates it onto the grid.
     *
     * @return true when a grid point was produced, in which case [results] holds it; false when the
     * sample only became the new baseline or repeated an identical timestamp.
     */
    fun update(x: Float, y: Float, z: Float, t: Long): Boolean {
        val tLast = tRawLast
        if (t.compareTo(tLast) != 0) {
            val interval = (if (this.tInterval > 0L) this.tInterval else t - tLast) + tResampledLast
            if (t < interval) {
                tRawLast = t
                xRawLast = x
                yRawLast = y
                zRawLast = z
                return false
            }
            val tLastSecond = tRawLast
            val scaledInterval = (interval - tLastSecond).toFloat() / (t - tLastSecond).toFloat()
            xResampledThis = xRawLast + (x - xRawLast) * scaledInterval
            yResampledThis = yRawLast + (y - yRawLast) * scaledInterval
            zResampledThis = scaledInterval * (z - zRawLast) + zRawLast
            tResampledLast = interval
            if (tLastSecond < interval) {
                tRawLast = t
                xRawLast = x
                yRawLast = y
                zRawLast = z
            }
            return true
        }
        return false
    }
}
