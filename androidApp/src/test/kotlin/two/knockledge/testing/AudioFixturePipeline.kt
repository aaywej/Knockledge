package two.knockledge.testing

import group.two.knockledge.pipeline.AudioPreprocessor
import group.two.knockledge.pipeline.SileroVadConfig
import group.two.knockledge.pipeline.VadDetector
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Encapsulates an audio test scenario with metadata and PCM waveform.
 *
 * @property id Unique identifier for the fixture.
 * @property description Human-readable test purpose.
 * @property pcm 16-bit mono PCM sample array.
 * @property sampleRate Audio sampling frequency in Hz (default 16,000).
 * @property speechDurationMillis Expected duration of spoken voice in milliseconds.
 * @property hasDeadAir Whether the fixture contains non-speech gaps exceeding 1,500 ms.
 * @property hasImpulsiveNoise Whether the fixture contains acoustic transients (knocks/clicks).
 */
data class AudioTestFixture(
    val id: String,
    val description: String,
    val pcm: ShortArray,
    val sampleRate: Int = 16_000,
    val speechDurationMillis: Long,
    val hasDeadAir: Boolean = false,
    val hasImpulsiveNoise: Boolean = false,
) {
    /** Total fixture duration in milliseconds. */
    val totalDurationMillis: Long = pcm.size * 1_000L / sampleRate

    /** True if detected voice duration is below the minimum threshold (1,000 ms). */
    val isInsufficientSpeech: Boolean = speechDurationMillis < AudioPreprocessor.MIN_SPEECH_DURATION_MILLIS

    /** Maximum absolute amplitude in the PCM array. */
    fun peak(): Int = pcm.maxOfOrNull { abs(it.toInt()) } ?: 0

    /** Root mean square (RMS) energy level across all samples. */
    fun rms(): Double {
        if (pcm.isEmpty()) return 0.0
        var sumSquares = 0.0
        for (sample in pcm) {
            val v = sample.toDouble()
            sumSquares += v * v
        }
        return sqrt(sumSquares / pcm.size)
    }

    /** Serializes the fixture to standard 16-bit mono RIFF WAV byte format. */
    fun toWavBytes(): ByteArray = AudioFixturePipeline.pcmToWav(pcm, sampleRate)

    /** Writes this fixture to a WAV file on disk. */
    fun writeWavFile(file: File): File {
        file.parentFile?.mkdirs()
        file.writeBytes(toWavBytes())
        return file
    }

    /** Creates a temporary WAV file for test execution. */
    fun createTempWavFile(prefix: String = "fixture"): File {
        val file = File.createTempFile("${prefix}_${id}_", ".wav")
        file.deleteOnExit()
        return writeWavFile(file)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioTestFixture) return false
        if (id != other.id) return false
        return pcm.contentEquals(other.pcm)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + pcm.contentHashCode()
        return result
    }
}

/**
 * Programmable audio fixture pipeline for feature 009-silero-vad-preprocessing.
 *
 * Generates calibrated synthetic PCM audio waveforms (voice, silence, knocks, noise)
 * and pre-configured test scenarios mapping directly to specification criteria.
 */
object AudioFixturePipeline {

    const val DEFAULT_SAMPLE_RATE = 16_000
    const val DEFAULT_FRAME_SIZE = 512

    // --- Fundamental Acoustic Generators ---

    /**
     * Generates a silent PCM buffer.
     *
     * @param durationSeconds Length of silence in seconds.
     * @param sampleRate Sampling rate in Hz.
     */
    fun silence(
        durationSeconds: Double,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): ShortArray = ShortArray((durationSeconds * sampleRate).toInt())

    /**
     * Generates synthetic human voice tones with multiple harmonic overtones and smooth envelope.
     *
     * @param durationSeconds Length of speech in seconds.
     * @param amplitude Peak target amplitude (default 14,000).
     * @param fundamentalHz Pitch fundamental frequency (default 150 Hz, typical speech).
     * @param sampleRate Sampling rate in Hz.
     */
    fun syntheticVoice(
        durationSeconds: Double,
        amplitude: Int = 14_000,
        fundamentalHz: Double = 150.0,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): ShortArray {
        val count = (durationSeconds * sampleRate).toInt()
        val out = ShortArray(count)
        val rampSamples = min(160, count / 4)

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val s0 = sin(2.0 * PI * fundamentalHz * t)
            val s1 = sin(2.0 * PI * (fundamentalHz * 3.5) * t) * 0.45
            val s2 = sin(2.0 * PI * (fundamentalHz * 9.0) * t) * 0.20
            val normalized = (s0 + s1 + s2) / 1.65

            val envelope = when {
                i < rampSamples -> i.toDouble() / rampSamples
                i > count - rampSamples -> (count - i).toDouble() / rampSamples
                else -> 1.0
            }

            val sample = (normalized * envelope * amplitude).toInt()
            out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /**
     * Generates an acoustic impulse spike with exponential decay, modeling a desk knock or keystroke.
     *
     * @param durationSeconds Length of transient window (default 40 ms).
     * @param amplitude Maximum peak amplitude (default 30,000).
     * @param frequencyHz Resonant frequency of impulse (default 350 Hz).
     * @param decayFactor Damping decay factor (default 90.0).
     * @param sampleRate Sampling rate in Hz.
     */
    fun impulsiveKnock(
        durationSeconds: Double = 0.04,
        amplitude: Int = 30_000,
        frequencyHz: Double = 350.0,
        decayFactor: Double = 90.0,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): ShortArray {
        val count = (durationSeconds * sampleRate).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-decayFactor * t)
            val osc = sin(2.0 * PI * frequencyHz * t)
            val sample = (osc * decay * amplitude).toInt()
            out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    /**
     * Generates stationary ambient noise modeling background air conditioning or fan hum.
     *
     * @param durationSeconds Length of noise in seconds.
     * @param amplitude Maximum noise amplitude (default 600).
     * @param seed Pseudorandom generator seed for deterministic reproducibility.
     * @param sampleRate Sampling rate in Hz.
     */
    fun stationaryNoise(
        durationSeconds: Double,
        amplitude: Int = 600,
        seed: Long = 42L,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): ShortArray {
        val count = (durationSeconds * sampleRate).toInt()
        val rng = Random(seed)
        return ShortArray(count) {
            rng.nextInt(-amplitude, amplitude + 1).toShort()
        }
    }

    /**
     * Concatenates multiple PCM sample arrays in sequential order.
     */
    fun concat(vararg clips: ShortArray): ShortArray {
        val totalLength = clips.sumOf { it.size }
        val out = ShortArray(totalLength)
        var offset = 0
        for (clip in clips) {
            clip.copyInto(out, offset)
            offset += clip.size
        }
        return out
    }

    /**
     * Mixes an overlay audio signal onto a copy of a base audio signal with saturation clipping.
     *
     * @param base Background or primary audio signal.
     * @param overlay Audio signal added onto the base.
     * @param offsetSamples Starting sample index where overlay is applied.
     */
    fun mix(
        base: ShortArray,
        overlay: ShortArray,
        offsetSamples: Int = 0,
    ): ShortArray {
        val out = base.copyOf()
        for (i in overlay.indices) {
            val targetIndex = offsetSamples + i
            if (targetIndex in out.indices) {
                val sum = out[targetIndex].toInt() + overlay[i].toInt()
                out[targetIndex] = sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }

    // --- Standard Spec Scenarios ---

    /**
     * Calculates the sample count aligned to whole [frameSize] chunks.
     */
    fun frameAlignedSamples(
        durationSeconds: Double,
        frameSize: Int = DEFAULT_FRAME_SIZE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): Int {
        val raw = (durationSeconds * sampleRate).toInt()
        val frames = (raw + frameSize - 1) / frameSize
        return frames * frameSize
    }

    /**
     * Scenario 1: Clean continuous speech meeting duration requirements (> 1.0s).
     * Tests baseline speech retention and volume normalization.
     */
    fun cleanSpeechScenario(
        durationSeconds: Double = 2.0,
        amplitude: Int = 14_000,
        frameSize: Int = DEFAULT_FRAME_SIZE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val totalSamples = frameAlignedSamples(durationSeconds, frameSize, sampleRate)
        val pcm = syntheticVoice(totalSamples.toDouble() / sampleRate, amplitude, sampleRate = sampleRate)
        return AudioTestFixture(
            id = "clean_speech",
            description = "Continuous speech exceeding 1.0s without dead air",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = (pcm.size * 1_000L / sampleRate),
            hasDeadAir = false,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Scenario 2: Two speech segments separated by dead air exceeding 1,500 ms.
     * Tests FR-003 dead air truncation down to 300 ms silence padding.
     */
    fun speechWithDeadAirScenario(
        speechDurationSeconds: Double = 1.0,
        deadAirDurationSeconds: Double = 3.0,
        amplitude: Int = 14_000,
        frameSize: Int = DEFAULT_FRAME_SIZE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val speechSamples = frameAlignedSamples(speechDurationSeconds, frameSize, sampleRate)
        val deadAirSamples = frameAlignedSamples(deadAirDurationSeconds, frameSize, sampleRate)

        val s1 = syntheticVoice(speechSamples.toDouble() / sampleRate, amplitude, fundamentalHz = 140.0, sampleRate = sampleRate)
        val gap = silence(deadAirSamples.toDouble() / sampleRate, sampleRate)
        val s2 = syntheticVoice(speechSamples.toDouble() / sampleRate, amplitude, fundamentalHz = 160.0, sampleRate = sampleRate)
        val pcm = concat(s1, gap, s2)
        val totalSpeechMillis = (2 * speechSamples * 1_000L / sampleRate)

        return AudioTestFixture(
            id = "speech_with_dead_air",
            description = "Two speech bursts separated by a 3.0s silent gap",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = totalSpeechMillis,
            hasDeadAir = true,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Scenario 3: Two speech segments separated by a natural pause under 1,500 ms.
     * Tests that conversational pauses under 1,500 ms remain intact.
     */
    fun speechWithNaturalPauseScenario(
        speechDurationSeconds: Double = 1.0,
        pauseDurationSeconds: Double = 0.5,
        amplitude: Int = 14_000,
        frameSize: Int = DEFAULT_FRAME_SIZE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val speechSamples = frameAlignedSamples(speechDurationSeconds, frameSize, sampleRate)
        val pauseSamples = frameAlignedSamples(pauseDurationSeconds, frameSize, sampleRate)

        val s1 = syntheticVoice(speechSamples.toDouble() / sampleRate, amplitude, sampleRate = sampleRate)
        val pause = silence(pauseSamples.toDouble() / sampleRate, sampleRate)
        val s2 = syntheticVoice(speechSamples.toDouble() / sampleRate, amplitude, sampleRate = sampleRate)
        val pcm = concat(s1, pause, s2)
        val totalSpeechMillis = (2 * speechSamples * 1_000L / sampleRate)

        return AudioTestFixture(
            id = "speech_with_natural_pause",
            description = "Two speech bursts separated by a 500ms conversational pause",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = totalSpeechMillis,
            hasDeadAir = false,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Scenario 4: Spoken words mixed with high-amplitude table knocks and keyboard clicks.
     * Tests SC-001 rejection of non-speech impulse sounds.
     */
    fun speechWithDeskKnocksScenario(
        speechDurationSeconds: Double = 2.0,
        knockCount: Int = 4,
        speechAmplitude: Int = 12_000,
        knockAmplitude: Int = 30_000,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        var pcm = syntheticVoice(speechDurationSeconds, speechAmplitude, sampleRate = sampleRate)
        val interval = pcm.size / (knockCount + 1)
        for (k in 1..knockCount) {
            val knock = impulsiveKnock(durationSeconds = 0.03, amplitude = knockAmplitude, sampleRate = sampleRate)
            pcm = mix(pcm, knock, offsetSamples = k * interval)
        }

        return AudioTestFixture(
            id = "speech_with_desk_knocks",
            description = "Spoken speech mixed with high-amplitude transient knocks",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = (speechDurationSeconds * 1_000L).toLong(),
            hasDeadAir = false,
            hasImpulsiveNoise = true,
        )
    }

    /**
     * Scenario 5: Ambient recording containing only table taps and clicks, without speech.
     * Tests FR-007 rejection of non-speech audio.
     */
    fun knocksOnlyScenario(
        durationSeconds: Double = 3.0,
        knockCount: Int = 5,
        knockAmplitude: Int = 30_000,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        var pcm = silence(durationSeconds, sampleRate)
        val interval = pcm.size / (knockCount + 1)
        for (k in 1..knockCount) {
            val knock = impulsiveKnock(durationSeconds = 0.04, amplitude = knockAmplitude, sampleRate = sampleRate)
            pcm = mix(pcm, knock, offsetSamples = k * interval)
        }

        return AudioTestFixture(
            id = "knocks_only",
            description = "Room recording with only table knocks and no speech",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = 0L,
            hasDeadAir = false,
            hasImpulsiveNoise = true,
        )
    }

    /**
     * Scenario 6: Voice content lasting less than 1.0 second total.
     * Tests FR-007 abort and InsufficientSpeech result.
     */
    fun insufficientSpeechScenario(
        speechDurationSeconds: Double = 0.6,
        silenceDurationSeconds: Double = 2.4,
        amplitude: Int = 14_000,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val voice = syntheticVoice(speechDurationSeconds, amplitude, sampleRate = sampleRate)
        val gap = silence(silenceDurationSeconds, sampleRate)
        val pcm = concat(voice, gap)

        return AudioTestFixture(
            id = "insufficient_speech",
            description = "Brief 600ms utterance below the 1.0s speech threshold",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = (speechDurationSeconds * 1_000L).toLong(),
            hasDeadAir = false,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Scenario 7: Quiet speech at low amplitude (5,000).
     * Tests FR-005 volume normalization to target peak headroom (29,490).
     */
    fun quietSpeechScenario(
        durationSeconds: Double = 1.5,
        lowAmplitude: Int = 5_000,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val pcm = syntheticVoice(durationSeconds, lowAmplitude, sampleRate = sampleRate)
        return AudioTestFixture(
            id = "quiet_speech",
            description = "Quiet 5,000 peak voice for headroom normalization",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = (durationSeconds * 1_000L).toLong(),
            hasDeadAir = false,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Scenario 8: Pure room silence.
     * Tests empty audio and pure silence handling.
     */
    fun pureSilenceScenario(
        durationSeconds: Double = 4.0,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
    ): AudioTestFixture {
        val pcm = silence(durationSeconds, sampleRate)
        return AudioTestFixture(
            id = "pure_silence",
            description = "Zero-amplitude baseline silence",
            pcm = pcm,
            sampleRate = sampleRate,
            speechDurationMillis = 0L,
            hasDeadAir = false,
            hasImpulsiveNoise = false,
        )
    }

    /**
     * Generates a complete suite containing all standard test scenarios.
     */
    fun allStandardScenarios(): List<AudioTestFixture> = listOf(
        cleanSpeechScenario(),
        speechWithDeadAirScenario(),
        speechWithNaturalPauseScenario(),
        speechWithDeskKnocksScenario(),
        knocksOnlyScenario(),
        insufficientSpeechScenario(),
        quietSpeechScenario(),
        pureSilenceScenario(),
    )

    // --- Serialization Helpers ---

    /**
     * Converts a 16-bit mono PCM sample array into a standard 44-byte RIFF WAV byte array.
     */
    fun pcmToWav(pcm: ShortArray, sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val dataBytes = pcm.size * 2
        val totalBytes = 44 + dataBytes
        val buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(36 + dataBytes)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // "fmt " chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat: 1 = PCM
        buffer.putShort(1) // NumChannels: 1 = Mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2) // ByteRate = SampleRate * NumChannels * BitsPerSample / 8
        buffer.putShort(2) // BlockAlign = NumChannels * BitsPerSample / 8
        buffer.putShort(16) // BitsPerSample: 16-bit

        // "data" chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(dataBytes)

        for (sample in pcm) {
            buffer.putShort(sample)
        }

        return buffer.array()
    }

    /**
     * Decodes a standard 16-bit PCM WAV byte array into mono [ShortArray],
     * automatically converting multi-channel to mono and resampling to [targetSampleRate].
     */
    fun decodeWav(bytes: ByteArray, targetSampleRate: Int = DEFAULT_SAMPLE_RATE): ShortArray {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") { "not a WAV file" }
        var pos = 12
        var channels = 1
        var sourceRate = 16_000
        var bits = 16
        var dataOffset = -1
        var dataSize = 0
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = b.getInt(pos + 4)
            when (id) {
                "fmt " -> {
                    channels = b.getShort(pos + 10).toInt()
                    sourceRate = b.getInt(pos + 12)
                    bits = b.getShort(pos + 22).toInt()
                }
                "data" -> {
                    dataOffset = pos + 8
                    dataSize = size
                }
            }
            pos += 8 + size + (size and 1)
        }
        require(dataOffset >= 0) { "missing data chunk in WAV" }
        require(bits == 16) { "only 16-bit PCM supported, got $bits" }

        val totalFrames = dataSize / (2 * channels)
        val monoSource = ShortArray(totalFrames)
        b.position(dataOffset)
        for (i in 0 until totalFrames) {
            var sum = 0
            for (c in 0 until channels) {
                sum += b.short.toInt()
            }
            monoSource[i] = (sum / channels).toShort()
        }

        if (sourceRate == targetSampleRate) {
            return monoSource
        }

        // Resample monoSource to targetSampleRate using linear interpolation
        val targetLength = ((totalFrames.toLong() * targetSampleRate) / sourceRate).toInt()
        val resampled = ShortArray(targetLength)
        val ratio = sourceRate.toDouble() / targetSampleRate
        for (i in 0 until targetLength) {
            val srcIndex = i * ratio
            val index0 = srcIndex.toInt().coerceIn(0, totalFrames - 1)
            val index1 = (index0 + 1).coerceIn(0, totalFrames - 1)
            val frac = srcIndex - index0
            val val0 = monoSource[index0].toDouble()
            val val1 = monoSource[index1].toDouble()
            resampled[i] = (val0 + (val1 - val0) * frac).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return resampled
    }
}

/**
 * Deterministic [VadDetector] that returns a pre-configured boolean mask.
 */
class FrameMaskVadDetector(
    private val mask: BooleanArray,
) : VadDetector {
    override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray = mask
}

/**
 * Deterministic [VadDetector] that classifies frames based on pre-defined millisecond time ranges.
 *
 * @param speechRanges Millisecond ranges of human speech within the audio.
 * @param frameSize Frame chunk size in samples (default 512).
 */
class MillisecondPatternVadDetector(
    private val speechRanges: List<LongRange>,
    private val frameSize: Int = SileroVadConfig().frameSize,
) : VadDetector {

    override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray {
        val frameCount = (pcm.size + frameSize - 1) / frameSize
        val frameDurationMs = frameSize * 1_000L / sampleRate

        return BooleanArray(frameCount) { index ->
            val frameStartMs = index * frameDurationMs
            val frameEndMs = frameStartMs + frameDurationMs
            speechRanges.any { range ->
                range.first < frameEndMs && range.last > frameStartMs
            }
        }
    }
}
