package group.two.knockledge.ui.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Main audio playback controls component displaying play/pause, seekbar, time, and chapter list.
 */
@Composable
fun AudioPlaybackControls(
    state: AudioPlaybackState,
    chapters: List<ChapterMarker>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Progress bar with chapter ticks
        ChapterProgressBar(
            currentPositionMs = state.currentPositionMs,
            durationMs = state.durationMs,
            chapters = chapters,
            onSeekTo = onSeekTo
        )

        // Time display & Play/Pause controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatTimeMs(state.currentPositionMs)} / ${formatTimeMs(state.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else if (state.isPlaying) {
                    Text(
                        text = "⏸",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "▶",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Active Chapter Header
        state.activeChapter?.let { active ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Now Playing: ${active.title}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Chapter List Section
        if (chapters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chapter_list_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chapters.forEach { chapter ->
                    val isActive = state.activeChapter?.startTimeMs == chapter.startTimeMs
                    ChapterItem(
                        chapter = chapter,
                        isActive = isActive,
                        onClick = { onSelectChapter(chapter) }
                    )
                }
            }
        }
    }
}

/**
 * Progress seekbar with chapter tick indicators.
 */
@Composable
fun ChapterProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    chapters: List<ChapterMarker>,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationFloat = durationMs.coerceAtLeast(1L).toFloat()
    val progressFloat = currentPositionMs.coerceIn(0L, durationMs).toFloat()

    Box(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progressFloat,
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..durationFloat,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Individual chapter list item.
 */
@Composable
fun ChapterItem(
    chapter: ChapterMarker,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimeMs(chapter.startTimeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (chapter.keyBullets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                chapter.keyBullets.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Formats milliseconds into mm:ss string format.
 */
fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun AudioPlaybackControlsPreview() {
    val sampleChapters = listOf(
        ChapterMarker(
            startTimeMs = 0L,
            title = "Introduction",
            keyBullets = listOf("Welcome and overview", "Agenda for today")
        ),
        ChapterMarker(
            startTimeMs = 45000L,
            title = "Project Updates",
            keyBullets = listOf("Milestone 1 completed", "Next steps defined")
        )
    )

    KnockledgeTheme {
        AudioPlaybackControls(
            state = AudioPlaybackState(
                currentPositionMs = 15000L,
                durationMs = 120000L,
                isPlaying = true,
                activeChapter = sampleChapters[0]
            ),
            chapters = sampleChapters,
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {}
        )
    }
}
