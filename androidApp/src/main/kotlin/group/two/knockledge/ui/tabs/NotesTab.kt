package group.two.knockledge.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.ui.playback.AudioPlaybackControls
import group.two.knockledge.ui.processing.NoteProcessingControls
import group.two.knockledge.ui.processing.NoteProcessingUiState
import group.two.knockledge.ui.theme.KnockledgeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single item representation of a captured [Note].
 */
@Composable
fun NoteListItem(
    note: Note,
    isSelected: Boolean,
    onSelectNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationSeconds = note.audio.durationMs / 1000L
    val durationFormatted = String.format(
        Locale.US,
        "%d:%02d",
        durationSeconds / 60,
        durationSeconds % 60,
    )
    val dateFormatted = rememberDateFormatted(note.createdAtEpochMs)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectNote(note) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.note_duration, durationFormatted)} • ${stringResource(R.string.note_created_at, dateFormatted)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (note.transcript != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Transcript", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    if (note.summary != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Summary", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
            TextButton(
                onClick = { onDeleteNote(note.id) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(text = stringResource(R.string.delete_note))
            }
        }
    }
}

@Composable
private fun rememberDateFormatted(epochMs: Long): String {
    val formatter = remember(epochMs) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    }
    return formatter.format(Date(epochMs))
}

/**
 * Notes history and audio playback / AI inspection tab.
 */
@Composable
fun NotesTab(
    notes: List<Note>,
    selectedNote: Note?,
    playbackState: AudioPlaybackState,
    processingUiState: NoteProcessingUiState,
    onSelectNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    onProcessNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.notes_tab_title),
            style = MaterialTheme.typography.titleMedium,
        )

        // Note List Column or Empty State
        if (notes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(R.string.no_notes_found),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = notes,
                    key = { it.id },
                ) { note ->
                    NoteListItem(
                        note = note,
                        isSelected = selectedNote?.id == note.id,
                        onSelectNote = onSelectNote,
                        onDeleteNote = onDeleteNote,
                    )
                }
            }
        }

        HorizontalDivider()

        // Selected Note Detail View
        if (selectedNote == null) {
            Text(
                text = stringResource(R.string.select_note_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Playback Controls
                item {
                    Text(
                        text = "Playback Controls: ${selectedNote.title.ifBlank { "Untitled Note" }}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    AudioPlaybackControls(
                        state = playbackState,
                        chapters = selectedNote.summary?.chapterMarkers ?: emptyList(),
                        onTogglePlayPause = onTogglePlayPause,
                        onSeekTo = onSeekTo,
                        onSelectChapter = onSelectChapter,
                    )
                }

                // AI Processing Controls
                item {
                    NoteProcessingControls(
                        uiState = processingUiState,
                        onProcessNote = { onProcessNote(selectedNote.id) },
                    )
                }

                // AI Note Intelligence Details
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.ai_intelligence_title),
                                style = MaterialTheme.typography.titleSmall,
                            )

                            // Summary Section
                            Text(
                                text = stringResource(R.string.summary_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            val summary = selectedNote.summary
                            if (summary != null) {
                                Text(
                                    text = summary.executiveSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (summary.keyPoints.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.key_bullet_points),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    summary.keyPoints.forEach { point ->
                                        Text(
                                            text = "• $point",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Transcript Section
                            Text(
                                text = stringResource(R.string.transcript_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            val transcript = selectedNote.transcript
                            if (transcript != null) {
                                Text(
                                    text = transcript.fullText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (transcript.sentences.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    transcript.sentences.forEach { sentence ->
                                        val startSec = sentence.startTimeMs / 1000L
                                        val startFmt = String.format(Locale.US, "%d:%02d", startSec / 60, startSec % 60)
                                        Text(
                                            text = "[$startFmt] ${sentence.speaker}: ${sentence.text}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_transcript),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesTabPreview() {
    val sampleNote = Note(
        id = "note_1",
        userId = "user_1",
        title = "Team Sync Discussion",
        createdAtEpochMs = System.currentTimeMillis(),
        updatedAtEpochMs = System.currentTimeMillis(),
        audio = AudioMetadata(
            localPath = "/tmp/audio.wav",
            durationMs = 125000L,
            sampleRate = 16000,
            triggerType = TriggerType.GESTURE,
        ),
        summary = SummaryMetadata(
            executiveSummary = "Discussion regarding backend sync performance and audio buffering strategy.",
            keyPoints = listOf("Optimized circular buffer allocation", "Added Silero VAD preprocessing"),
            chapterMarkers = emptyList(),
            generationMode = AiEngineType.CLOUD_FIREBASE_AI,
        ),
    )

    KnockledgeTheme {
        NotesTab(
            notes = listOf(sampleNote),
            selectedNote = sampleNote,
            playbackState = AudioPlaybackState(),
            processingUiState = NoteProcessingUiState(),
            onSelectNote = {},
            onDeleteNote = {},
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onProcessNote = {},
        )
    }
}
