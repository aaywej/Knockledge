package group.two.knockledge.ai

import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptSentence
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType

/** Runs an injected transcribe function as a [TranscriptionEngine], for the on-device Whisper path. */
class WhisperTranscriberAdapter(
    private val transcribeText: suspend (ShortArray) -> String,
) : TranscriptionEngine {
    override val engineType = TranscriptionEngineType.LOCAL_WHISPER

    /** Reports the whole segment as one speaker, since Whisper here does no diarization. */
    override suspend fun process(segment: AudioSegment): TranscriptResult {
        val text = transcribeText(segment.pcm).trim()
        val sentences = if (text.isEmpty()) emptyList()
        else listOf(TranscriptSentence("Speaker", 0, segment.durationMillis, text))
        return TranscriptResult(sentences, text, false, "unknown", engineType)
    }
}
