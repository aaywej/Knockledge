package group.two.knockledge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.QueueStatus
import group.two.knockledge.data.model.Sentence
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.model.TranscriptMetadata
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.model.TriggerType

/** Room row for a note; transcript and summary columns are nullable until the pipeline fills them. */
@Entity(
    tableName = "notes",
    indices = [Index("user_id"), Index(value = ["created_at_epoch_ms"])],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val title: String,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @ColumnInfo(name = "audio_local_path") val audioLocalPath: String,
    @ColumnInfo(name = "audio_remote_url") val audioRemoteUrl: String?,
    @ColumnInfo(name = "audio_duration_ms") val audioDurationMs: Long,
    @ColumnInfo(name = "audio_sample_rate") val audioSampleRate: Int,
    @ColumnInfo(name = "audio_trigger_type") val audioTriggerType: TriggerType,
    @ColumnInfo(name = "transcript_full_text") val transcriptFullText: String?,
    @ColumnInfo(name = "transcript_sentences_json") val transcriptSentences: List<Sentence>?,
    @ColumnInfo(name = "transcript_is_diarized") val transcriptIsDiarized: Boolean?,
    @ColumnInfo(name = "transcript_detected_language") val transcriptDetectedLanguage: String?,
    @ColumnInfo(name = "transcript_engine_type") val transcriptEngineType: TranscriptionEngineType?,
    @ColumnInfo(name = "summary_executive_summary") val summaryExecutiveSummary: String?,
    @ColumnInfo(name = "summary_key_points_json") val summaryKeyPoints: List<String>?,
    @ColumnInfo(name = "summary_chapter_markers_json") val summaryChapterMarkers: List<ChapterMarker>?,
    @ColumnInfo(name = "summary_generation_mode") val summaryGenerationMode: AiEngineType?,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
)

/** Room row for one queued sync task; deleting its note cascades the task away. */
@Entity(
    tableName = "sync_queue",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id"), Index(value = ["user_id", "status"])],
)
data class SyncQueueEntity(
    @PrimaryKey @ColumnInfo(name = "queue_id") val queueId: String,
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "task_type") val taskType: SyncTaskType,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    val status: QueueStatus,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

/** Rebuilds the domain model, supplying defaults (no diarization, Whisper, on-device) for nullable transcript and summary columns. */
fun NoteEntity.toNote() = Note(
    id = id,
    userId = userId,
    title = title,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    audio = AudioMetadata(audioLocalPath, audioRemoteUrl, audioDurationMs, audioSampleRate, audioTriggerType),
    transcript = transcriptFullText?.let {
        TranscriptMetadata(
            fullText = it,
            sentences = transcriptSentences.orEmpty(),
            isDiarized = transcriptIsDiarized ?: false,
            detectedLanguage = transcriptDetectedLanguage.orEmpty(),
            engineType = transcriptEngineType ?: TranscriptionEngineType.LOCAL_WHISPER,
        )
    },
    summary = summaryExecutiveSummary?.let {
        SummaryMetadata(
            executiveSummary = it,
            keyPoints = summaryKeyPoints.orEmpty(),
            chapterMarkers = summaryChapterMarkers.orEmpty(),
            generationMode = summaryGenerationMode ?: AiEngineType.LOCAL_ON_DEVICE,
        )
    },
    syncStatus = syncStatus,
)

/** Flattens the note into its columns, turning absent transcript or summary into nulls. */
fun Note.toEntity() = NoteEntity(
    id = id,
    userId = userId,
    title = title,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    audioLocalPath = audio.localPath,
    audioRemoteUrl = audio.remoteUrl,
    audioDurationMs = audio.durationMs,
    audioSampleRate = audio.sampleRate,
    audioTriggerType = audio.triggerType,
    transcriptFullText = transcript?.fullText,
    transcriptSentences = transcript?.sentences,
    transcriptIsDiarized = transcript?.isDiarized,
    transcriptDetectedLanguage = transcript?.detectedLanguage,
    transcriptEngineType = transcript?.engineType,
    summaryExecutiveSummary = summary?.executiveSummary,
    summaryKeyPoints = summary?.keyPoints,
    summaryChapterMarkers = summary?.chapterMarkers,
    summaryGenerationMode = summary?.generationMode,
    syncStatus = syncStatus,
)
