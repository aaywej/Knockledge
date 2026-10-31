package group.two.knockledge.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.ui.theme.KnockCream
import group.two.knockledge.ui.theme.KnockInk
import group.two.knockledge.ui.theme.KnockledgeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    notes: List<Note>,
    onStartListening: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your Anchors",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Settings",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
        }
        Spacer(Modifier.height(24.dp))
        if (notes.isEmpty()) {
            Text(
                "Nothing anchored yet. Start a session and knock when it matters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    AnchorCard(note, onClick = { onOpenNote(note.id) })
                }
            }
        }
        Button(
            onClick = onStartListening,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Start listening", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnchorCard(note: Note, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(KnockCream, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            anchorDate(note.createdAtEpochMs),
            style = MaterialTheme.typography.labelMedium,
            color = KnockInk,
        )
        Text(
            note.title,
            style = MaterialTheme.typography.titleLarge,
            color = KnockInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            note.summary?.executiveSummary ?: "Still writing the chapter",
            style = MaterialTheme.typography.bodyMedium,
            color = KnockInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun anchorDate(epochMs: Long): String =
    SimpleDateFormat("MMM dd  HH:mm", Locale.US).format(Date(epochMs)).uppercase(Locale.US)

private fun sampleNote(id: String, title: String, summary: String, epochMs: Long) = Note(
    id = id,
    userId = "preview",
    title = title,
    createdAtEpochMs = epochMs,
    updatedAtEpochMs = epochMs,
    audio = AudioMetadata("/preview/$id.wav", null, 25_000L, 16_000, TriggerType.GESTURE),
    summary = SummaryMetadata(summary, emptyList(), emptyList(), AiEngineType.LOCAL_ON_DEVICE),
)

@Preview(showBackground = true, backgroundColor = 0xFF14122B, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    KnockledgeTheme {
        HomeScreen(
            notes = listOf(
                sampleNote("1", "Photosynthesis", "Light energy is captured by chlorophyll and used to make ATP.", 1_792_584_240_000L),
                sampleNote("2", "Binary trees", "A tree has a root, and each node can have at most two children.", 1_792_497_840_000L),
                sampleNote("3", "Sprint planning", "Team agreed on the next three tasks and who owns each one.", 1_792_324_800_000L),
            ),
            onStartListening = {},
            onOpenNote = {},
            onOpenSettings = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14122B, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenEmptyPreview() {
    KnockledgeTheme {
        HomeScreen(emptyList(), {}, {}, {})
    }
}