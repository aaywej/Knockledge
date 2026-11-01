package group.two.knockledge.sensor

/** One 3-axis point of a resampled signal, with its timestamp in nanoseconds. */
class Sample3C(x: Float, y: Float, z: Float, var t: Long) {
    var point: Point3f = Point3f(x, y, z)
}
