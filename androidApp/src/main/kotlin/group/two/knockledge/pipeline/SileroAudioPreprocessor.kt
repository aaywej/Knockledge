package group.two.knockledge.pipeline

import kotlin.math.abs
import kotlin.math.min

/**
 * Neural [AudioPreprocessor]: a [VadDetector] classifies each [SileroVadConfig.frameSize]
 * chunk as speech or not, non-speech runs longer than
 * [SileroVadConfig.maxDeadAirGapMillis] are cut down to
 * [SileroVadConfig.silencePaddingMillis] of ambient silence, and retained audio is peak
 * normalized to [SileroVadConfig.targetPeakAmplitude]. Audio holding under
 * [SileroVadConfig.minSpeechDurationMillis] of speech is rejected.
 *
 * @property vadDetector Classifier producing the per-frame speech mask.
 * @property config Thresholds and frame sizing for conditioning.
 */
class SileroAudioPreprocessor(
    override val vadDetector: VadDetector = FallbackEnergyVadDetector(),
    override val config: SileroVadConfig = SileroVadConfig(),
) : SileroAudioPreprocessorContract {

    override fun process(pcm: ShortArray, sampleRate: Int): AudioPreprocessResult {
        if (pcm.isEmpty()) return AudioPreprocessResult.InsufficientSpeech(0L)
        val speech = vadDetector.detect(pcm, sampleRate)
        val maxGapSamples = (config.maxDeadAirGapMillis * sampleRate / 1_000).toInt()
        val paddingSamples = (config.silencePaddingMillis * sampleRate / 1_000).toInt()

        val out = ShortArray(pcm.size)
        var outSize = 0
        var speechSamples = 0
        var frame = 0
        while (frame < speech.size) {
            var end = frame
            while (end < speech.size && speech[end] == speech[frame]) end++
            val from = frame * config.frameSize
            if (from >= pcm.size) break
            val to = min(pcm.size, end * config.frameSize)
            var run = to - from
            if (speech[frame]) {
                speechSamples += run
            } else if (run > maxGapSamples) {
                run = paddingSamples
            }
            pcm.copyInto(out, outSize, from, from + run)
            outSize += run
            frame = end
        }

        val speechMillis = speechSamples * 1_000L / sampleRate
        if (speechMillis < config.minSpeechDurationMillis) {
            return AudioPreprocessResult.InsufficientSpeech(speechMillis)
        }

        val trimmed = out.copyOf(outSize)
        normalizePeak(trimmed)
        return AudioPreprocessResult.Success(
            pcm = trimmed,
            durationMillis = trimmed.size * 1_000L / sampleRate,
            sampleRate = sampleRate,
        )
    }

    /** Scales [pcm] in place so its peak lands on [SileroVadConfig.targetPeakAmplitude]. */
    private fun normalizePeak(pcm: ShortArray) {
        var peak = 0
        for (s in pcm) peak = maxOf(peak, abs(s.toInt()))
        if (peak == 0 || peak == config.targetPeakAmplitude.toInt()) return
        val gain = config.targetPeakAmplitude.toDouble() / peak
        for (i in pcm.indices) {
            pcm[i] = (pcm[i] * gain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
