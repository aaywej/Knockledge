package two.knockledge.pipeline

import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.FallbackEnergyVadDetector
import group.two.knockledge.pipeline.SileroAudioPreprocessor
import group.two.knockledge.pipeline.SileroVadConfig
import group.two.knockledge.pipeline.VadDetector
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SileroAudioPreprocessorTest {

    private val sampleRate = 16_000
    private val config = SileroVadConfig()

    /** Chunk-length fake: evaluates the supplied predicate per 512-sample chunk. */
    private class ChunkVadDetector(
        private val frameSize: Int,
        private val classify: (chunk: ShortArray) -> Boolean,
    ) : VadDetector {
        var calls = 0
        override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray {
            calls++
            val frames = (pcm.size + frameSize - 1) / frameSize
            return BooleanArray(frames) { i ->
                val from = i * frameSize
                classify(pcm.copyOfRange(from, minOf(pcm.size, from + frameSize)))
            }
        }
    }

    /** Loud chunks read as speech; near-silent chunks read as non-speech. */
    private fun energyFake(): ChunkVadDetector = ChunkVadDetector(config.frameSize) { chunk ->
        chunk.any { abs(it.toInt()) >= 330 }
    }

    /** Fake returning a caller-scripted mask, one flag per 512-sample frame. */
    private class ScriptedVadDetector(private val mask: BooleanArray) : VadDetector {
        override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray = mask
    }

    private fun preprocessor(detector: VadDetector) = SileroAudioPreprocessor(detector, config)

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

    /** [n] detector-frame-sized chunks of constant-amplitude samples. */
    private fun chunks(n: Int, amplitude: Int = 2_000): ShortArray =
        ShortArray(n * config.frameSize) { amplitude.toShort() }

    private fun silentChunks(n: Int): ShortArray = ShortArray(n * config.frameSize)

    @Test
    fun retainsSpeechAndTruncatesDeadAirToSilencePadding() {
        // 32 speech frames (~1s), 94 non-speech frames (~3s), 32 speech frames.
        val mask = BooleanArray(32) { true } + BooleanArray(94) { false } + BooleanArray(32) { true }
        val input = concat(chunks(32), silentChunks(94), chunks(32))

        val result = preprocessor(ScriptedVadDetector(mask)).process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        val expected = 2 * 32 * config.frameSize +
            AudioPreprocessor.SILENCE_PADDING_MILLIS * sampleRate / 1_000
        assertEquals(expected.toInt(), result.pcm.size)
    }

    @Test
    fun rejectsNonSpeechNoiseClassifiedByNeuralDetector() {
        // A short noise run under the gap limit is kept whole as a natural pause.
        val mask = BooleanArray(24) { true } + BooleanArray(16) { false } + BooleanArray(24) { true }
        val input = concat(chunks(24), silentChunks(16), chunks(24))

        val result = preprocessor(ScriptedVadDetector(mask)).process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(input.size, result.pcm.size)
    }

    @Test
    fun keepsShortPausesUnderTheGapLimitIntact() {
        val input = concat(speech(1.0), silence(0.5), speech(1.0))

        val result = preprocessor(energyFake()).process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(input.size, result.pcm.size)
    }

    @Test
    fun normalizesQuietSpeechToTargetPeakHeadroom() {
        val input = speech(1.5, amplitude = 5_000)

        val result = preprocessor(energyFake()).process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt(), peak(result.pcm))
    }

    @Test
    fun clampsNormalizationWithoutClippingLoudSpeech() {
        val input = speech(1.5, amplitude = 30_000)

        val result = preprocessor(energyFake()).process(input, sampleRate)

        assertIs<AudioPreprocessResult.Success>(result)
        assertTrue(peak(result.pcm) <= Short.MAX_VALUE.toInt())
    }

    @Test
    fun returnsInsufficientSpeechWhenVoiceIsUnderOneSecond() {
        // 15 speech frames (~0.5s) then 140 non-speech frames (~4.5s).
        val mask = BooleanArray(15) { true } + BooleanArray(140) { false }
        val input = concat(chunks(15), silentChunks(140))

        val result = preprocessor(ScriptedVadDetector(mask)).process(input, sampleRate)

        val insufficient = assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
        assertTrue(insufficient.detectedSpeechMillis < AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS)
    }

    @Test
    fun returnsInsufficientSpeechForPureNonSpeechNoise() {
        val input = chunks(160)
        val mask = BooleanArray(160) { false }

        val result = preprocessor(ScriptedVadDetector(mask)).process(input, sampleRate)

        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun returnsInsufficientSpeechForEmptyInput() {
        val result = preprocessor(energyFake()).process(ShortArray(0), sampleRate)
        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun returnsSuccessForAtLeastOneSecondOfSpeech() {
        val result = preprocessor(energyFake()).process(speech(1.2), sampleRate)
        assertIs<AudioPreprocessResult.Success>(result)
    }

    @Test
    fun doesNotMutateInputArray() {
        val input = concat(speech(1.0), silence(3.0), speech(1.0))
        val original = input.copyOf()

        preprocessor(energyFake()).process(input, sampleRate)

        assertTrue(input.contentEquals(original))
    }

    @Test
    fun fallbackEnergyDetectorFlagsLoudSpeechAndSilence() {
        val detector = FallbackEnergyVadDetector()
        val mask = detector.detect(concat(speech(0.1), silence(0.1)), sampleRate)

        assertTrue(mask[0])
        assertTrue(!mask.last())
    }
}
