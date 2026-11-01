package group.two.knockledge.pipeline

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Configuration options for Silero VAD neural voice classification.
 *
 * @property sampleRate Audio sampling rate in Hz (defaults to 16,000).
 * @property frameSize Chunk size in samples passed into the neural network (defaults to 512, ~32ms).
 * @property speechThreshold Confidence threshold for speech detection (defaults to 0.5f).
 * @property minSpeechDurationMillis Minimum speech duration required to pass to AI (1,000 ms).
 * @property maxDeadAirGapMillis Maximum silent gap before dead air truncation (1,500 ms).
 * @property silencePaddingMillis Ambient silence margin around speech segments (300 ms).
 * @property targetPeakAmplitude Peak amplitude for headroom normalization (29,490).
 */
data class SileroVadConfig(
    val sampleRate: Int = 16_000,
    val frameSize: Int = 512,
    val speechThreshold: Float = 0.5f,
    val minSpeechDurationMillis: Long = AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS,
    val maxDeadAirGapMillis: Long = AudioPreprocessor.MAX_DEAD_AIR_GAP_MILLIS,
    val silencePaddingMillis: Long = AudioPreprocessor.SILENCE_PADDING_MILLIS,
    val targetPeakAmplitude: Short = AudioPreprocessor.TARGET_PEAK_AMPLITUDE,
)

/**
 * Contract for voice activity classification over audio frames.
 */
interface VadDetector : AutoCloseable {
    /**
     * Evaluates [pcm] audio and returns a boolean array indicating whether each
     * sequential chunk contains active human speech.
     *
     * @param pcm 16-bit mono PCM sample array.
     * @param sampleRate Sampling frequency in Hz (typically 16,000).
     * @return Boolean array with one flag per frame.
     */
    fun detect(pcm: ShortArray, sampleRate: Int = 16_000): BooleanArray

    override fun close() {}
}

/**
 * Neural [AudioPreprocessor] backed by [VadDetector] for speech-isolated conditioning.
 */
interface SileroAudioPreprocessorContract : AudioPreprocessor {
    val vadDetector: VadDetector
    val config: SileroVadConfig
}

/**
 * Pure-Kotlin [VadDetector] that classifies frames by RMS energy. Used on JVM unit
 * tests and as the graceful fallback when the neural Silero model is unavailable.
 */
class FallbackEnergyVadDetector(
    private val frameSize: Int = SileroVadConfig().frameSize,
    private val rmsThreshold: Double = SPEECH_RMS_THRESHOLD,
) : VadDetector {

    override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray {
        val frames = (pcm.size + frameSize - 1) / frameSize
        return BooleanArray(frames) { i ->
            val from = i * frameSize
            rms(pcm, from, min(pcm.size, from + frameSize)) >= rmsThreshold
        }
    }

    /** Root-mean-square amplitude of [pcm] over [from, to). */
    private fun rms(pcm: ShortArray, from: Int, to: Int): Double {
        if (to <= from) return 0.0
        var sum = 0.0
        for (i in from until to) sum += pcm[i].toDouble() * pcm[i]
        return sqrt(sum / (to - from))
    }

    private companion object {
        /** Matches the energy gate used by [DefaultAudioPreprocessor]. */
        const val SPEECH_RMS_THRESHOLD = 330.0
    }
}
