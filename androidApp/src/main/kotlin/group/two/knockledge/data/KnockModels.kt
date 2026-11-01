package group.two.knockledge.data

import group.two.knockledge.core.RewindPolicy
import java.io.File
import kotlin.math.sqrt

/** What started a recording: the knock gesture or the user. */
enum class TriggerType { GESTURE, MANUAL }

/** Settings for the circular audio buffer; the constructor rejects a window outside RewindPolicy's limits and any sample rate but the required one. */
data class BufferConfiguration(
    val durationSeconds: Int,
    val sampleRate: Int = RewindPolicy.SAMPLE_RATE,
    val chunkDurationMs: Int = RewindPolicy.CHUNK_SECONDS * 1000,
    val storageDirectory: File,
) {
    init {
        require(durationSeconds in RewindPolicy.MIN_WINDOW_SECONDS..RewindPolicy.MAX_WINDOW_SECONDS)
        require(sampleRate == RewindPolicy.SAMPLE_RATE)
    }
}

/** One accelerometer reading and the moment it was captured, in nanoseconds. */
data class KnockSensorSample(val x: Float, val y: Float, val z: Float, val timestampNs: Long) {
    /** Total acceleration, sqrt(x² + y² + z²). */
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

/** A detected double knock; the constructor rejects a peak gap outside 100-400 ms and a peak below 12. */
data class KnockGestureEvent(
    val firstPeakTimeMs: Long,
    val secondPeakTimeMs: Long,
    val peakMagnitude: Float,
    val triggerSource: TriggerType = TriggerType.GESTURE,
) {
    init {
        require(secondPeakTimeMs - firstPeakTimeMs in 100L..400L)
        require(peakMagnitude >= 12.0f)
    }
}

/** A finished recording on disk and the span of wall-clock time it covers. */
data class AudioSnapshot(
    val file: File,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val sampleRate: Int,
    val triggerType: TriggerType,
) {
    /** Recorded length in milliseconds. */
    val durationMs: Long get() = endEpochMs - startEpochMs
}
