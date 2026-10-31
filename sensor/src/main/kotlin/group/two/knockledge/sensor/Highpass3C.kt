package group.two.knockledge.sensor

/** One-pole high-pass filter applied per axis, one [Highpass1C] per axis. */
class Highpass3C {

    private val xHighpass = Highpass1C()
    private val yHighpass = Highpass1C()
    private val zHighpass = Highpass1C()

    /** Seeds every axis with the sample used for both input and output history. */
    fun init(point: Point3f) {
        xHighpass.init(point.x)
        yHighpass.init(point.y)
        zHighpass.init(point.z)
    }

    /** Applies the same feed weight to every axis. */
    fun setPara(para: Float) {
        xHighpass.setPara(para)
        yHighpass.setPara(para)
        zHighpass.setPara(para)
    }

    /** Returns the high-passed point with the same units as the input. */
    fun update(point: Point3f): Point3f {
        return Point3f(xHighpass.update(point.x), yHighpass.update(point.y), zHighpass.update(point.z))
    }

}
