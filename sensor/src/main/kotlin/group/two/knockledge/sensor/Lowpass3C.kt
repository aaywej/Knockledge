package group.two.knockledge.sensor

/** One-pole low-pass filter applied per axis, one [Lowpass1C] per axis. */
class Lowpass3C {

    private val xLowpass = Lowpass1C()
    private val yLowpass = Lowpass1C()
    private val zLowpass = Lowpass1C()

    /** Seeds every axis with the sample used as the previous output. */
    fun init(point: Point3f) {
        xLowpass.init(point.x)
        yLowpass.init(point.y)
        zLowpass.init(point.z)
    }

    /** Applies the same smoothing weight to every axis. */
    fun setPara(para: Float) {
        xLowpass.setPara(para)
        yLowpass.setPara(para)
        zLowpass.setPara(para)
    }

    /** Returns the filtered point with the same units as the input. */
    fun update(point: Point3f): Point3f {
        return Point3f(xLowpass.update(point.x), yLowpass.update(point.y), zLowpass.update(point.z))
    }

}
