package group.two.knockledge.pipeline

import group.two.knockledge.audio.AudioConditioner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Tries the [online] engine first when a connection is available, and falls back
 * to [offline] on timeout or failure. Audio is conditioned before either sees it.
 */
class CompositeTranscriber(
    private val online: TranscriptionEngine,
    private val offline: TranscriptionEngine,
    private val isOnline: () -> Boolean,
    private val timeoutMillis: Long = 10_000,
) : Transcriber {
    /** Conditions [pcm], then transcribes online if possible and offline otherwise. */
    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult {
        val conditioned = AudioConditioner.condition(pcm, sampleRate)
        if (conditioned.isEmpty()) {
            return TranscriptResult(emptyList(), "", false, "unknown", TranscriptionEngineType.LOCAL_WHISPER)
        }
        val segment = AudioSegment(conditioned, sampleRate, conditioned.size * 1000L / sampleRate, isGainNormalized = true)
        if (isOnline()) {
            try {
                return withTimeout(timeoutMillis) { online.process(segment) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
            }
        }
        return offline.process(segment)
    }
}
