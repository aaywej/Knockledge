package group.two.knockledge.pipeline

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.min

/**
 * Production [VadDetector] running the bundled Silero VAD ONNX model through ONNX
 * Runtime. The model bytes are loaded lazily on first [detect]; when missing or
 * failing to load, detection degrades to [FallbackEnergyVadDetector] so the rewind
 * pipeline keeps working.
 *
 * @property modelBytes Lambda returning the bytes of the Silero ONNX model, read lazily. Null means unavailable.
 * @property config Frame sizing and speech probability threshold.
 */
class OnnxSileroVadDetector(
    private val modelBytes: () -> ByteArray?,
    private val config: SileroVadConfig = SileroVadConfig(),
) : VadDetector {

    private val detector: VadDetector by lazy {
        loadNeural()?.also { logActive("Silero VAD neural model") }
            ?: FallbackEnergyVadDetector().also { logActive("energy fallback, no Silero model") }
    }

    /** True once the ONNX session is live; forces the lazy model load on first read. */
    val isNeural: Boolean get() = detector is OnnxDetector

    override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray =
        detector.detect(pcm, sampleRate)

    override fun close() = detector.close()

    /** Builds the ONNX-backed detector, or null when the model file cannot be read. */
    private fun loadNeural(): VadDetector? = try {
        modelBytes()?.let { OnnxDetector(it, config) }
    } catch (_: Exception) {
        null
    }

    /** android.util.Log is stubbed on the JVM unit test classpath; ignore failures there. */
    private fun logActive(which: String) = runCatching { Log.i(TAG, "voice detection: $which") }

    /**
     * Silero VAD v5 session: feeds 512-sample chunks sequentially and carries the
     * recurrent [2, 1, 128] state tensor between them so speech onsets are not clipped.
     */
    private class OnnxDetector(
        modelBytes: ByteArray,
        private val config: SileroVadConfig,
    ) : VadDetector {
        private val env = OrtEnvironment.getEnvironment()
        private val session = env.createSession(modelBytes)
        private val state = FloatArray(STATE_ELEMENTS)

        override fun detect(pcm: ShortArray, sampleRate: Int): BooleanArray {
            state.fill(0f)
            val frames = (pcm.size + config.frameSize - 1) / config.frameSize
            val result = BooleanArray(frames)
            for (i in 0 until frames) {
                result[i] = runFrame(pcm, i, sampleRate)
            }
            return result
        }

        /** Runs one chunk through the session and returns whether it holds speech. */
        private fun runFrame(pcm: ShortArray, frame: Int, sampleRate: Int): Boolean {
            val chunk = FloatArray(config.frameSize)
            val from = frame * config.frameSize
            val n = min(config.frameSize, pcm.size - from)
            for (j in 0 until n) chunk[j] = pcm[from + j] / PCM_FULL_SCALE

            val input = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(chunk), longArrayOf(1, config.frameSize.toLong()),
            )
            val stateTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(state), STATE_SHAPE,
            )
            val rateTensor = OnnxTensor.createTensor(
                env, LongBuffer.wrap(longArrayOf(sampleRate.toLong())), longArrayOf(1),
            )
            try {
                session.run(
                    mapOf("input" to input, "state" to stateTensor, "sr" to rateTensor),
                ).use { output ->
                    val probability = (output.get("output").get().value as Array<FloatArray>)[0][0]
                    @Suppress("UNCHECKED_CAST")
                    val next = output.get("stateN").get().value as Array<Array<FloatArray>>
                    next[0][0].copyInto(state, 0)
                    next[1][0].copyInto(state, STATE_ELEMENTS / 2)
                    return probability >= config.speechThreshold
                }
            } finally {
                input.close()
                stateTensor.close()
                rateTensor.close()
            }
        }

        override fun close() {
            session.close()
        }

        private companion object {
            val STATE_SHAPE = longArrayOf(2, 1, 128)
            const val STATE_ELEMENTS = 2 * 128
            const val PCM_FULL_SCALE = 32_768f
        }
    }

    private companion object {
        const val TAG = "SileroVad"
    }
}
