package group.two.knockledge.pipeline

import group.two.knockledge.core.formatDuration

/**
 * Stand-ins until the on-device models are wired in (see docs/ARCHITECTURE.md).
 * They let the full flow run end to end on both platforms today.
 */
class PlaceholderTranscriber : Transcriber {
    /** Returns an empty transcript; stand-in until a real engine is connected. */
    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult =
        TranscriptResult(emptyList(), "", false, "unknown", TranscriptionEngineType.LOCAL_WHISPER)
}

/** Returns a fixed card saying the models are not connected yet. */
class PlaceholderSummarizer : Summarizer {
    /** Builds the placeholder card, echoing the captured duration and redaction count. */
    override suspend fun summarize(request: SummaryRequest): Summary = Summary(
        title = "Untitled rewind",
        gist = "Captured ${formatDuration(request.durationMillis)} of audio. The on-device " +
            "transcription and summary models are not connected yet, so this is a placeholder.",
        points = listOf(
            "The audio was held in memory only and has already been wiped.",
            "Once a model is connected, this card shows a short recap of what was said.",
        ),
        redactedCount = request.redactedCount,
    )
}

