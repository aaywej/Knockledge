package group.two.knockledge.sensor

/** First difference of a 3-axis signal, one [Slope1C] per axis. */
class Slope3C {

    private val _slopeX = Slope1C()
    private val _slopeY = Slope1C()
    private val _slopeZ = Slope1C()

    /** Seeds every axis with the sample used as the previous value. */
    fun init(point: Point3f) {
        _slopeX.init(point.x)
        _slopeY.init(point.y)
        _slopeZ.init(point.z)
    }

    /**
     * Per-axis change since the previous call.
     *
     * @param d scale applied to the new value before differencing, used to compensate for a changed
     * sample interval.
     */
    fun update(point: Point3f, d: Float): Point3f {
        return Point3f(
            _slopeX.update(point.x, d),
            _slopeY.update(point.y, d),
            _slopeZ.update(point.z, d)
        )
    }

}
