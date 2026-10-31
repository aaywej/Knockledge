package two.knockledge.testing

import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.SileroAudioPreprocessor
import group.two.knockledge.pipeline.SileroVadConfig
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AudioFixturePipelineTest {

    private val sampleRate = AudioFixturePipeline.DEFAULT_SAMPLE_RATE
    private val config = SileroVadConfig()
    private val tempFiles = mutableListOf<File>()

    @AfterTest
    fun cleanup() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun silenceGeneratorProducesExactZeroSamples() {
        val pcm = AudioFixturePipeline.silence(1.5, sampleRate)
        assertEquals((1.5 * sampleRate).toInt(), pcm.size)
        assertTrue(pcm.all { it == 0.toShort() })
    }

    @Test
    fun syntheticVoiceProducesValidAudioWaveform() {
        val pcm = AudioFixturePipeline.syntheticVoice(1.0, amplitude = 15_000, sampleRate = sampleRate)
        assertEquals(sampleRate, pcm.size)
        val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(peak in 10_000..15_000)
    }

    @Test
    fun impulsiveKnockDecaysRapidly() {
        val knock = AudioFixturePipeline.impulsiveKnock(durationSeconds = 0.05, amplitude = 30_000, sampleRate = sampleRate)
        assertTrue(knock.isNotEmpty())
        val peakInitial = knock.take(160).maxOf { kotlin.math.abs(it.toInt()) }
        val peakTail = knock.takeLast(160).maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(peakInitial > 20_000)
        assertTrue(peakTail < peakInitial / 10)
    }

    @Test
    fun wavSerializationProducesCompliantRiffHeader() {
        val fixture = AudioFixturePipeline.cleanSpeechScenario(durationSeconds = 0.5)
        val bytes = fixture.toWavBytes()
        assertEquals(44 + fixture.pcm.size * 2, bytes.size)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val riff = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("RIFF", riff)

        val chunkSize = buffer.int
        assertEquals(36 + fixture.pcm.size * 2, chunkSize)

        val wave = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("WAVE", wave)

        val fmt = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("fmt ", fmt)

        val subchunk1Size = buffer.int
        assertEquals(16, subchunk1Size)

        val audioFormat = buffer.short
        assertEquals(1.toShort(), audioFormat) // PCM

        val numChannels = buffer.short
        assertEquals(1.toShort(), numChannels) // Mono

        val rate = buffer.int
        assertEquals(sampleRate, rate)

        val byteRate = buffer.int
        assertEquals(sampleRate * 2, byteRate)

        val blockAlign = buffer.short
        assertEquals(2.toShort(), blockAlign)

        val bitsPerSample = buffer.short
        assertEquals(16.toShort(), bitsPerSample)

        val data = ByteArray(4).also { buffer.get(it) }.toString(Charsets.US_ASCII)
        assertEquals("data", data)

        val dataSize = buffer.int
        assertEquals(fixture.pcm.size * 2, dataSize)
    }

    @Test
    fun fixtureTempWavFileCanBeWrittenAndRead() {
        val fixture = AudioFixturePipeline.cleanSpeechScenario(durationSeconds = 0.2)
        val temp = fixture.createTempWavFile()
        tempFiles += temp

        assertTrue(temp.exists())
        assertEquals(44 + fixture.pcm.size * 2, temp.length().toInt())
    }

    @Test
    fun cleanSpeechScenarioSucceedsAndNormalizesHeadroom() {
        val fixture = AudioFixturePipeline.cleanSpeechScenario(durationSeconds = 2.0, amplitude = 12_000)
        val detector = MillisecondPatternVadDetector(listOf(0L..2_000L))
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        val success = assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt(), success.pcm.maxOf { kotlin.math.abs(it.toInt()) })
    }

    @Test
    fun speechWithDeadAirScenarioTruncatesGapToSilencePadding() {
        val speechSeconds = 1.0
        val deadAirSeconds = 3.0
        val fixture = AudioFixturePipeline.speechWithDeadAirScenario(
            speechDurationSeconds = speechSeconds,
            deadAirDurationSeconds = deadAirSeconds,
            frameSize = config.frameSize,
            sampleRate = sampleRate,
        )
        val speechFrames = AudioFixturePipeline.frameAlignedSamples(speechSeconds, config.frameSize, sampleRate) / config.frameSize
        val gapFrames = AudioFixturePipeline.frameAlignedSamples(deadAirSeconds, config.frameSize, sampleRate) / config.frameSize
        val mask = BooleanArray(speechFrames) { true } + BooleanArray(gapFrames) { false } + BooleanArray(speechFrames) { true }

        val detector = FrameMaskVadDetector(mask)
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        val success = assertIs<AudioPreprocessResult.Success>(result)

        // Expected samples: 2 * speechFrames * frameSize + 300ms silence padding
        val expectedSamples = 2 * speechFrames * config.frameSize +
            (config.silencePaddingMillis * sampleRate / 1_000).toInt()
        assertEquals(expectedSamples, success.pcm.size)
    }

    @Test
    fun speechWithNaturalPauseScenarioPreservesPauseIntact() {
        val speechSeconds = 1.0
        val pauseSeconds = 0.5
        val fixture = AudioFixturePipeline.speechWithNaturalPauseScenario(
            speechDurationSeconds = speechSeconds,
            pauseDurationSeconds = pauseSeconds,
            frameSize = config.frameSize,
            sampleRate = sampleRate,
        )
        val speechFrames = AudioFixturePipeline.frameAlignedSamples(speechSeconds, config.frameSize, sampleRate) / config.frameSize
        val pauseFrames = AudioFixturePipeline.frameAlignedSamples(pauseSeconds, config.frameSize, sampleRate) / config.frameSize
        val mask = BooleanArray(speechFrames) { true } + BooleanArray(pauseFrames) { false } + BooleanArray(speechFrames) { true }

        val detector = FrameMaskVadDetector(mask)
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        val success = assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(fixture.pcm.size, success.pcm.size)
    }

    @Test
    fun speechWithDeskKnocksExcludesKnocksOutsideSpeechIntervals() {
        val fixture = AudioFixturePipeline.speechWithDeskKnocksScenario(speechDurationSeconds = 2.0, knockCount = 3)
        // Only speech intervals are active voice; knock transients are classified as non-speech
        val detector = MillisecondPatternVadDetector(listOf(0L..2_000L))
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        assertIs<AudioPreprocessResult.Success>(result)
    }

    @Test
    fun insufficientSpeechScenarioReturnsInsufficientSpeechResult() {
        // 0.6s speech < 1.0s threshold
        val fixture = AudioFixturePipeline.insufficientSpeechScenario(speechDurationSeconds = 0.6, silenceDurationSeconds = 2.0)
        val detector = MillisecondPatternVadDetector(listOf(0L..600L))
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        val insufficient = assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
        assertTrue(insufficient.detectedSpeechMillis < AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS)
    }

    @Test
    fun pureSilenceScenarioReturnsInsufficientSpeechResult() {
        val fixture = AudioFixturePipeline.pureSilenceScenario(durationSeconds = 4.0)
        val detector = MillisecondPatternVadDetector(emptyList())
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun quietSpeechScenarioNormalizesGainToTargetPeakHeadroom() {
        val fixture = AudioFixturePipeline.quietSpeechScenario(durationSeconds = 1.5, lowAmplitude = 5_000)
        val detector = MillisecondPatternVadDetector(listOf(0L..1_500L))
        val preprocessor = SileroAudioPreprocessor(detector, config)

        val result = preprocessor.process(fixture.pcm, sampleRate)
        val success = assertIs<AudioPreprocessResult.Success>(result)
        assertEquals(AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt(), success.pcm.maxOf { kotlin.math.abs(it.toInt()) })
    }

    @Test
    fun allStandardScenariosHaveValidMetadataAndDurations() {
        val scenarios = AudioFixturePipeline.allStandardScenarios()
        assertEquals(8, scenarios.size)
        for (scenario in scenarios) {
            assertTrue(scenario.id.isNotBlank())
            assertTrue(scenario.description.isNotBlank())
            assertTrue(scenario.pcm.isNotEmpty())
            assertEquals(scenario.totalDurationMillis, scenario.pcm.size * 1_000L / sampleRate)
        }
    }
}
