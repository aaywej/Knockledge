package two.knockledge.pipeline

import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AudioPreprocessorTest {

    private val sampleRate = 16_000
    private val preprocessor = DefaultAudioPreprocessor()

    private fun speech(seconds: Double, amplitude: Int = 2_000): ShortArray =
        ShortArray((seconds * sampleRate).toInt()) { amplitude.toShort() }

    private fun silence(seconds: Double): ShortArray = ShortArray((seconds * sampleRate).toInt())

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) {
            p.copyInto(out, pos)
            pos += p.size
        }
        return out
    }

    private fun peak(pcm: ShortArray): Int = pcm.maxOf { abs(it.toInt()) }

    @Test
    fun truncatesDeadAirGapsToSilencePadding() {
        val input = concat(speech(1.0), silence(3.0), speech(1.0))
        val result = preprocessor.process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        val expected = 2.0 * sampleRate +
            AudioPreprocessor.SILENCE_PADDING_MILLIS * sampleRate / 1_000
        assertEquals(expected.toInt(), result.pcm.size)
    }

    @Test
    fun keepsNaturalPausesUnderTheGapLimit() {
        val input = concat(speech(1.0), silence(0.5), speech(1.0))
        val result = preprocessor.process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(input.size, result.pcm.size)
    }

    @Test
    fun normalizesQuietSpeechToTargetPeak() {
        val input = speech(1.5, amplitude = 5_000)
        val result = preprocessor.process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(
            AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt(),
            peak(result.pcm),
        )
    }

    @Test
    fun clampsNormalizationWithoutClippingLoudSpeech() {
        val input = speech(1.5, amplitude = 30_000)
        val result = preprocessor.process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertTrue(peak(result.pcm) <= Short.MAX_VALUE.toInt())
    }

    @Test
    fun returnsInsufficientSpeechUnderOneSecond() {
        val input = concat(speech(0.5), silence(1.0))
        val result = preprocessor.process(input, sampleRate)

        val insufficient = assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
        assertTrue(insufficient.detectedSpeechMillis < AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS)
    }

    @Test
    fun returnsInsufficientSpeechForPureSilence() {
        val result = preprocessor.process(silence(5.0), sampleRate)
        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun returnsSuccessForAtLeastOneSecondOfSpeech() {
        val result = preprocessor.process(speech(1.2), sampleRate)
        assertIs<AudioPreprocessResult.Success>(result)
    }

    @Test
    fun doesNotMutateInputArray() {
        val input = concat(speech(1.0), silence(3.0), speech(1.0))
        val original = input.copyOf()
        preprocessor.process(input, sampleRate)
        assertTrue(input.contentEquals(original))
    }
}
