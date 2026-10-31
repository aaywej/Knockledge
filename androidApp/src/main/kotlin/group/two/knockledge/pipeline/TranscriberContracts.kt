package group.two.knockledge.pipeline

/** Turns raw PCM into a transcript. */
interface Transcriber {
    /** Transcribes [pcm] recorded at [sampleRate]. */
    suspend fun transcribe(pcm: ShortArray, sampleRate: Int = 16_000): TranscriptResult
}

/** A concrete speech-to-text engine (cloud or on-device) behind a [Transcriber]. */
interface TranscriptionEngine {
    /** Which engine this is, reported on the result for diagnostics. */
    val engineType: TranscriptionEngineType

    /** Transcribes one conditioned audio [segment]. */
    suspend fun process(segment: AudioSegment): TranscriptResult
}
