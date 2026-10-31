package group.two.knockledge.pipeline

/**
 * Result of the pre-processing unit.
 */
sealed interface AudioPreprocessResult {
    /**
     * Successfully conditioned audio with dead air eradicated and levels normalized.
     *
     * @property pcm The processed 16-bit mono PCM sample array.
     * @property durationMillis Duration of the conditioned speech in milliseconds.
     * @property sampleRate Sampling rate in Hz (typically 16,000).
     */
    data class Success(
        val pcm: ShortArray,
        val durationMillis: Long,
        val sampleRate: Int = 16_000,
    ) : AudioPreprocessResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return pcm.contentEquals(other.pcm) && durationMillis == other.durationMillis && sampleRate == other.sampleRate
        }

        override fun hashCode(): Int = (pcm.contentHashCode() * 31 + durationMillis.hashCode()) * 31 + sampleRate
    }

    /**
     * Active speech content fell below the minimum viable threshold for AI processing.
     *
     * @property detectedSpeechMillis Milliseconds of speech detected before reaching threshold check.
     * @property minimumRequiredMillis Minimum speech duration required to proceed (1,000 ms).
     */
    data class InsufficientSpeech(
        val detectedSpeechMillis: Long,
        val minimumRequiredMillis: Long = 1_000L,
    ) : AudioPreprocessResult
}

/**
 * Pre-processes audio before handing it off to the AI transcription engine.
 * Eradicates extended dead air, applies volume normalization, and verifies speech duration.
 */
interface AudioPreprocessor {
    /**
     * Processes input [pcm] audio. Returns [AudioPreprocessResult.Success] if speech >= 1.0s,
     * or [AudioPreprocessResult.InsufficientSpeech] if speech is under 1.0s.
     *
     * The input array is not mutated; a new conditioned sample array is returned upon success.
     */
    fun process(
        pcm: ShortArray,
        sampleRate: Int = 16_000,
    ): AudioPreprocessResult

    companion object {
        /** Minimum non-silent speech duration required to run AI models (1 second). */
        const val MIN_SPEECH_DURATION_MILLIS: Long = 1_000L

        /** Maximum silent gap allowed before truncating dead air (1.5 seconds). */
        const val MAX_DEAD_AIR_GAP_MILLIS: Long = 1_500L

        /** Ambient silence padding to retain around speech segments (300 ms). */
        const val SILENCE_PADDING_MILLIS: Long = 300L

        /** Target peak amplitude for normalization (~ -1 dBFS). */
        const val TARGET_PEAK_AMPLITUDE: Short = 29_490
    }
}
