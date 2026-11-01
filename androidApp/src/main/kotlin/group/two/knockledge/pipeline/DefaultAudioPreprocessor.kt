package group.two.knockledge.pipeline

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure-Kotlin [AudioPreprocessor]: energy-based Voice Activity Detection over 20 ms
 * frames, dead air truncation, then peak normalization.
 *
 * Silent runs longer than [AudioPreprocessor.MAX_DEAD_AIR_GAP_MILLIS] are cut down to
 * [AudioPreprocessor.SILENCE_PADDING_MILLIS] of ambient silence; retained audio whose
 * active speech is under [AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS] is rejected.
 */
class DefaultAudioPreprocessor : AudioPreprocessor {

    override fun process(pcm: ShortArray, sampleRate: Int): AudioPreprocessResult {
        if (pcm.isEmpty()) return AudioPreprocessResult.InsufficientSpeech(0L)
        val frameSamples = sampleRate * FRAME_MILLIS / 1_000
        val maxGapFrames = (AudioPreprocessor.MAX_DEAD_AIR_GAP_MILLIS / FRAME_MILLIS).toInt()
        val paddingFrames = (AudioPreprocessor.SILENCE_PADDING_MILLIS / FRAME_MILLIS).toInt()

        val loud = BooleanArray((pcm.size + frameSamples - 1) / frameSamples) { i ->
            rms(pcm, i * frameSamples, min(pcm.size, (i + 1) * frameSamples)) >= SPEECH_RMS_THRESHOLD
        }

        val out = ShortArray(pcm.size)
        var outSize = 0
        var speechFrames = 0
        var i = 0
        while (i < loud.size) {
            val runEnd = runEnd(loud, i)
            if (loud[i]) {
                outSize += copyFrames(pcm, out, outSize, i, runEnd, frameSamples)
                speechFrames += runEnd - i
            } else {
                val keep = min(runEnd - i, if (runEnd - i > maxGapFrames) paddingFrames else runEnd - i)
                outSize += copyFrames(pcm, out, outSize, i, i + keep, frameSamples)
            }
            i = runEnd
        }

        val speechMillis = speechFrames.toLong() * FRAME_MILLIS
        if (speechMillis < AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS) {
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

    /** Index one past the end of the same-loudness frame run starting at [from]. */
    private fun runEnd(loud: BooleanArray, from: Int): Int {
        var end = from
        while (end < loud.size && loud[end] == loud[from]) end++
        return end
    }

    /** Copies frames [fromFrame, toFrame) of [src] into [dst] at [dstOffset]; returns samples copied. */
    private fun copyFrames(
        src: ShortArray,
        dst: ShortArray,
        dstOffset: Int,
        fromFrame: Int,
        toFrame: Int,
        frameSamples: Int,
    ): Int {
        val start = fromFrame * frameSamples
        val end = min(src.size, toFrame * frameSamples)
        src.copyInto(dst, dstOffset, start, end)
        return end - start
    }

    private fun rms(pcm: ShortArray, from: Int, to: Int): Double {
        if (to <= from) return 0.0
        var sum = 0.0
        for (i in from until to) sum += pcm[i].toDouble() * pcm[i]
        return sqrt(sum / (to - from))
    }

    /** Scales [pcm] in place so its peak lands on [AudioPreprocessor.TARGET_PEAK_AMPLITUDE]. */
    private fun normalizePeak(pcm: ShortArray) {
        var peak = 0
        for (s in pcm) peak = maxOf(peak, abs(s.toInt()))
        if (peak == 0 || peak == AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt()) return
        val gain = AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toDouble() / peak
        for (i in pcm.indices) {
            pcm[i] = (pcm[i] * gain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private companion object {
        const val FRAME_MILLIS = 20
        const val SPEECH_RMS_THRESHOLD = 330.0
    }
}
