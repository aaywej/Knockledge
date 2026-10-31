package group.two.knockledge.data.repository

import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SentenceInput
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.TranscriptResult
import java.io.File
import group.two.knockledge.data.model.TranscriptionEngineType as ModelEngineType
import group.two.knockledge.data.model.TriggerType as ModelTriggerType
import group.two.knockledge.pipeline.TranscriptionEngineType as PipelineEngineType

/** Turns one finished recording, its transcript and its summary into a stored note. */
fun interface NoteSink {
    /** Persists the recording; implementations decide where the note lands. */
    suspend fun record(
        file: File,
        startEpochMs: Long,
        endEpochMs: Long,
        trigger: TriggerType,
        transcript: TranscriptResult,
        summary: Summary,
    )
}

/**
 * [NoteSink] that writes through [NoteRepository]: create the note, then attach the transcript and
 * the summary. The pipeline produces no chapter markers, so the saved summary has none.
 */
class SnapshotNoteSink(
    private val notes: NoteRepository,
    private val auth: AuthRepository,
) : NoteSink {
    /** Saves nothing when no user is signed in; otherwise makes three repository writes. */
    override suspend fun record(
        file: File,
        startEpochMs: Long,
        endEpochMs: Long,
        trigger: TriggerType,
        transcript: TranscriptResult,
        summary: Summary,
    ) {
        val user = auth.currentUser.value ?: return
        val note = notes.createFromSnapshot(
            AudioSnapshotInput(
                file = file,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                durationMs = endEpochMs - startEpochMs,
                triggerType = ModelTriggerType.valueOf(trigger.name),
            ),
            user.uid,
        )
        notes.attachTranscript(
            note.id,
            user.uid,
            TranscriptInput(
                fullText = transcript.fullText,
                sentences = transcript.sentences.map {
                    SentenceInput(it.speaker, it.startTimeMs, it.endTimeMs, it.text, it.confidence)
                },
                isDiarized = transcript.isDiarized,
                detectedLanguage = transcript.detectedLanguage,
                engineType = ModelEngineType.valueOf(transcript.engineType.name),
            ),
        )
        notes.attachSummary(
            note.id,
            user.uid,
            SummaryInput(
                title = summary.title,
                executiveSummary = summary.gist,
                keyPoints = summary.points,
                chapterMarkers = emptyList(),
                generationMode = if (transcript.engineType == PipelineEngineType.CLOUD_FIREBASE_AI) {
                    AiEngineType.CLOUD_FIREBASE_AI
                } else {
                    AiEngineType.LOCAL_ON_DEVICE
                },
            ),
        )
    }
}
