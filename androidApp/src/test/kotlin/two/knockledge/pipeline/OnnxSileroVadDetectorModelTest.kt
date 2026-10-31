package two.knockledge.pipeline

import group.two.knockledge.pipeline.AudioPreprocessResult
import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.OnnxSileroVadDetector
import group.two.knockledge.pipeline.SileroAudioPreprocessor
import group.two.knockledge.pipeline.SileroVadConfig
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue
import two.knockledge.testing.AudioFixturePipeline

/**
 * Real ONNX Runtime inference over the downloaded Silero VAD model on the JVM.
 *
 * Exercises the neural VAD model against synthetic and real audio fixtures generated
 * by [AudioFixturePipeline]. Requires `silero_vad.onnx` under `src/test/resources-local/`.
 */
class OnnxSileroVadDetectorModelTest {

    private val config = SileroVadConfig()
    private val modelFile = sequenceOf(
        File("src/main/assets/silero_vad.onnx"),
        File("androidApp/src/main/assets/silero_vad.onnx"),
        File("src/test/resources-local/silero_vad.onnx"),
    ).firstOrNull { it.isFile && it.length() > 0 } ?: File("src/main/assets/silero_vad.onnx")
    private val sampleWavFile = File("src/androidTest/assets/sample.wav")

    private fun assumeModel(): File {
        assumeTrue(
            "silero_vad.onnx missing in src/main/assets/",
            modelFile.isFile && modelFile.length() > 0,
        )
        return modelFile
    }

    private fun createNeuralPreprocessor(): SileroAudioPreprocessor {
        val model = assumeModel()
        return SileroAudioPreprocessor(OnnxSileroVadDetector({ model.readBytes() }), config)
    }

    @Test
    fun loadsTheNeuralModelFromDisk() {
        val model = assumeModel()
        val detector = OnnxSileroVadDetector({ model.readBytes() })

        assertTrue(detector.isNeural)
        detector.close()
    }

    @Test
    fun silenceDetectsNoSpeechFrames() {
        val model = assumeModel()
        val detector = OnnxSileroVadDetector({ model.readBytes() })

        val pcm = ShortArray(2 * config.sampleRate)
        val mask = detector.detect(pcm, config.sampleRate)

        assertEquals((pcm.size + config.frameSize - 1) / config.frameSize, mask.size)
        assertTrue(mask.none { it })
        detector.close()
    }

    @Test
    fun returnsOneFlagPerFrameForSyntheticInput() {
        val model = assumeModel()
        val detector = OnnxSileroVadDetector({ model.readBytes() })

        // Loud synthetic tone: exercises the session without asserting it is speech.
        val pcm = ShortArray(config.sampleRate) {
            (sin(it * 2.0 * Math.PI * 220.0 / config.sampleRate) * 20_000).toInt().toShort()
        }
        val mask = detector.detect(pcm, config.sampleRate)

        assertEquals((pcm.size + config.frameSize - 1) / config.frameSize, mask.size)
        assertTrue(detector.isNeural)
        detector.close()
    }

    @Test
    fun missingModelFallsBackToEnergyDetector() {
        val detector = OnnxSileroVadDetector({ null })

        kotlin.test.assertFalse(detector.isNeural)
        detector.close()
    }

    @Test
    fun realModelClassifiesSilenceFixtureAsInsufficientSpeech() {
        val preprocessor = createNeuralPreprocessor()
        val fixture = AudioFixturePipeline.pureSilenceScenario(durationSeconds = 3.0)

        val result = preprocessor.process(fixture.pcm, fixture.sampleRate)
        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun realModelClassifiesTableKnocksAsNonSpeech() {
        val preprocessor = createNeuralPreprocessor()
        // 5 sharp table knocks over 3 seconds
        val fixture = AudioFixturePipeline.knocksOnlyScenario(durationSeconds = 3.0, knockCount = 5)

        val result = preprocessor.process(fixture.pcm, fixture.sampleRate)
        // Silero VAD neural network rejects impulsive non-speech knocks
        assertIs<AudioPreprocessResult.InsufficientSpeech>(result)
    }

    @Test
    fun realModelClassifiesStationaryNoiseAsNonSpeech() {
        val model = assumeModel()
        val detector = OnnxSileroVadDetector({ model.readBytes() })
        val noise = AudioFixturePipeline.stationaryNoise(durationSeconds = 2.0, amplitude = 800)

        val mask = detector.detect(noise, config.sampleRate)
        // Background stationary hum should not trigger speech detection
        assertTrue(mask.none { it })
        detector.close()
    }

    @Test
    fun realModelDetectsRealHumanSpeechFromSampleWav() {
        assumeTrue("sample.wav missing in src/androidTest/assets", sampleWavFile.isFile)
        val pcm = AudioFixturePipeline.decodeWav(sampleWavFile.readBytes())

        val preprocessor = createNeuralPreprocessor()
        val result = preprocessor.process(pcm, config.sampleRate)

        val success = assertIs<AudioPreprocessResult.Success>(result)
        // Real human speech is detected and preserved
        assertTrue(success.pcm.isNotEmpty())
        // Peak amplitude is normalized to headroom target 29,490
        val peak = success.pcm.maxOf { abs(it.toInt()) }
        assertEquals(AudioPreprocessor.TARGET_PEAK_AMPLITUDE.toInt(), peak)
    }

    @Test
    fun realModelTruncatesDeadAirBetweenRealSpeechBursts() {
        assumeTrue("sample.wav missing in src/androidTest/assets", sampleWavFile.isFile)
        val fullPcm = AudioFixturePipeline.decodeWav(sampleWavFile.readBytes())

        // Extract a 1.5s real speech snippet (first phrase: "Okay, tara na")
        val speechSnippet = fullPcm.copyOfRange(0, minOf(fullPcm.size, (1.5 * config.sampleRate).toInt()))
        // 3.0s dead air gap (exceeding 1.5s max gap threshold)
        val deadAir = AudioFixturePipeline.silence(3.0, config.sampleRate)
        val composite = AudioFixturePipeline.concat(speechSnippet, deadAir, speechSnippet)

        val preprocessor = createNeuralPreprocessor()
        val result = preprocessor.process(composite, config.sampleRate)

        val success = assertIs<AudioPreprocessResult.Success>(result)
        // Input had 3.0s (48,000 samples) dead air. Truncated output must be significantly shorter.
        val originalSamples = composite.size
        assertTrue(success.pcm.size < originalSamples - (2.0 * config.sampleRate).toInt())
    }
}
