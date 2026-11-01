package group.two.knockledge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.remote.GoogleIdTokenClient
import group.two.knockledge.di.KnockledgeComponent
import group.two.knockledge.ui.auth.AuthScreen
import group.two.knockledge.ui.auth.AuthViewModel
import group.two.knockledge.ui.playback.AudioPlaybackControls
import group.two.knockledge.ui.playback.NotePlaybackViewModel
import group.two.knockledge.ui.processing.NoteProcessingControls
import group.two.knockledge.ui.processing.NoteProcessingUiState
import group.two.knockledge.ui.processing.NoteProcessingViewModel
import group.two.knockledge.ui.settings.SettingsViewModel
import group.two.knockledge.ui.tabs.SettingsTab
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings Gear Icon for the top left header.
 */
@Composable
fun SettingsGearIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.width * 0.36f
        val innerR = size.width * 0.18f

        drawCircle(
            color = tint,
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val startX = (cx + (outerR - 1.dp.toPx()) * kotlin.math.cos(angle)).toFloat()
            val startY = (cy + (outerR - 1.dp.toPx()) * kotlin.math.sin(angle)).toFloat()
            val endX = (cx + (outerR + 3.5.dp.toPx()) * kotlin.math.cos(angle)).toFloat()
            val endY = (cy + (outerR + 3.5.dp.toPx()) * kotlin.math.sin(angle)).toFloat()
            drawLine(
                color = tint,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun SearchLensIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val radius = w * 0.32f

        drawCircle(
            color = tint,
            radius = radius,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.65f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ClearCrossIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.75f, h * 0.25f),
            end = Offset(w * 0.25f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun PlayTriangleIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.3f, h * 0.8f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun PauseBarsIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 3.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.25f),
            end = Offset(w * 0.35f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.25f),
            end = Offset(w * 0.65f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun TrashBinIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 1.75.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.28f),
            end = Offset(w * 0.8f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.16f),
            size = Size(w * 0.24f, h * 0.22f),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.32f),
            size = Size(w * 0.48f, h * 0.52f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.41f, h * 0.42f),
            end = Offset(w * 0.41f, h * 0.72f),
            strokeWidth = stroke,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.59f, h * 0.42f),
            end = Offset(w * 0.59f, h * 0.72f),
            strokeWidth = stroke,
        )
    }
}

@Composable
fun BackArrowNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = tint,
            start = Offset(w * 0.85f, h * 0.5f),
            end = Offset(w * 0.15f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun rememberFormattedDateTitle(epochMs: Long): String {
    val formatter = remember(epochMs) {
        SimpleDateFormat("MMM d, h.mm a", Locale.US)
    }
    return formatter.format(Date(epochMs))
}

@Composable
private fun rememberFormattedSubtext(epochMs: Long, durationMs: Long): String {
    val dateFmt = remember(epochMs) {
        SimpleDateFormat("M/d/yy", Locale.US)
    }
    val durSec = durationMs / 1000L
    val durFmt = String.format(Locale.US, "%02d:%02d", durSec / 60, durSec % 60)
    return "$durFmt  ${dateFmt.format(Date(epochMs))}"
}

@Composable
fun NoteListItem(
    note: Note,
    isSelected: Boolean,
    isPlayingThisNote: Boolean,
    onSelectNote: (Note) -> Unit,
    onTogglePlayNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedTitle = if (note.title.isNotBlank()) note.title else rememberFormattedDateTitle(note.createdAtEpochMs)
    val subtext = rememberFormattedSubtext(note.createdAtEpochMs, note.audio.durationMs)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectNote(note) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onTogglePlayNote(note) },
                contentAlignment = Alignment.Center,
            ) {
                if (isPlayingThisNote) {
                    PauseBarsIcon(tint = MaterialTheme.colorScheme.onSurface)
                } else {
                    PlayTriangleIcon(tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { onDeleteNote(note.id) },
                modifier = Modifier.size(36.dp),
            ) {
                TrashBinIcon(tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

/**
 * Custom Canvas composable that draws a pixelated retro ring contour matching Figma mockup styling.
 */
@Composable
fun PixelatedRing(
    radius: androidx.compose.ui.unit.Dp,
    pixelSize: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radiusPx = radius.toPx()
        val stepPx = pixelSize.toPx()
        val steps = (2 * Math.PI * radiusPx / stepPx).toInt().coerceAtLeast(32)

        for (i in 0 until steps) {
            val angle = (2 * Math.PI * i / steps).toFloat()
            val rawX = radiusPx * kotlin.math.cos(angle)
            val rawY = radiusPx * kotlin.math.sin(angle)
            val snappedX = (rawX / stepPx).toInt() * stepPx
            val snappedY = (rawY / stepPx).toInt() * stepPx

            drawRect(
                color = color,
                topLeft = Offset(centerX + snappedX - stepPx / 2f, centerY + snappedY - stepPx / 2f),
                size = Size(stepPx, stepPx),
            )
        }
    }
}

/**
 * Floating Recording Button reproducing the RecordTab circle button verbatim.
 */
@Composable
fun RecordFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(88.dp),
        shape = CircleShape,
        color = ObsidianMidnight,
        shadowElevation = 12.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PixelatedRing(
                radius = 36.dp,
                pixelSize = 2.5.dp,
                color = AccentGold.copy(alpha = 0.85f),
                modifier = Modifier.size(76.dp),
            )
            PixelatedRing(
                radius = 28.dp,
                pixelSize = 2.5.dp,
                color = AccentGold.copy(alpha = 0.55f),
                modifier = Modifier.size(60.dp),
            )
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ParchmentCream),
            )
        }
    }
}

/**
 * Main application Home Screen hosting the recordings list content.
 */
@Composable
fun HomeScreen(
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
    onManualSave: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            RecordFloatingActionButton(
                onClick = onManualSave,
            )
        },
    ) { innerPadding ->
        HomeRecordingsContent(
            notes = notes,
            selectedNote = selectedNote,
            playbackState = playbackState,
            processingUiState = processingUiState,
            onSelectNote = onSelectNote,
            onDeleteNote = onDeleteNote,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSelectChapter = onSelectChapter,
            onProcessNote = onProcessNote,
            modifier = Modifier.padding(innerPadding),
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

/**
 * Migrated Home Screen Recordings view content.
 */
@Composable
fun HomeRecordingsContent(
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
    onNavigateToSettings: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Recorder", "Calls", "Apps")

    val filteredNotes = remember(notes, searchQuery, selectedCategory) {
        notes.filter { note ->
            val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                (note.transcript?.fullText?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = when (selectedCategory) {
                "Recorder" -> note.audio.triggerType == TriggerType.MANUAL || note.audio.triggerType == TriggerType.GESTURE
                "Calls" -> false
                "Apps" -> false
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // Top Header Row: Recordings title on left, Settings gear icon on top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Recordings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(44.dp),
                ) {
                    SettingsGearIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search all recordings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                leadingIcon = {
                    SearchLensIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            ClearCrossIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            // Filter Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            // Note List or Empty State
            if (filteredNotes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.no_notes_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(
                        items = filteredNotes,
                        key = { it.id },
                    ) { note ->
                        val isSelected = selectedNote?.id == note.id
                        val isPlayingThisNote = isSelected && playbackState.isPlaying

                        NoteListItem(
                            note = note,
                            isSelected = isSelected,
                            isPlayingThisNote = isPlayingThisNote,
                            onSelectNote = { selected ->
                                onSelectNote(selected)
                            },
                            onTogglePlayNote = { selected ->
                                if (selectedNote?.id != selected.id) {
                                    onSelectNote(selected)
                                }
                                onTogglePlayPause()
                            },
                            onDeleteNote = onDeleteNote,
                        )
                    }

                    // Selected Note Detail Section
                    if (selectedNote != null) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Playback: ${selectedNote.title.ifBlank { rememberFormattedDateTitle(selectedNote.createdAtEpochMs) }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AudioPlaybackControls(
                                state = playbackState,
                                chapters = selectedNote.summary?.chapterMarkers ?: emptyList(),
                                onTogglePlayPause = onTogglePlayPause,
                                onSeekTo = onSeekTo,
                                onSelectChapter = onSelectChapter,
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            NoteProcessingControls(
                                uiState = processingUiState,
                                onProcessNote = { onProcessNote(selectedNote.id) },
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
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
                                        fontWeight = FontWeight.Bold,
                                    )

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
    }
}

/**
 * Full application shell host binding all subsystem ViewModels and passing state into [HomeScreen].
 */
@Composable
fun LiveTestAppShell(
    component: KnockledgeComponent,
    hasRecordAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val graph = remember(component) { component.graph() }
    val dataGraph = remember(component) { component.data() }
    val appSettings = remember(component) { component.appSettings() }
    val audioPlayer = remember(component) { component.audioPlayer() }
    val pipeline = remember(component) { component.pipeline() }
    val aiNotifier = remember(component) { component.aiNotificationNotifier() }

    val recordingViewModel = remember(component, graph, appSettings) {
        KnockRecordingViewModel(graph.controller, appSettings)
    }
    val playbackViewModel = remember(component, audioPlayer) {
        NotePlaybackViewModel(audioPlayer)
    }
    val processingViewModel = remember(component, dataGraph, pipeline, aiNotifier) {
        NoteProcessingViewModel(dataGraph.notes, dataGraph.auth, pipeline, aiNotifier)
    }
    val settingsViewModel = remember(component, appSettings) {
        SettingsViewModel(appSettings)
    }
    val context = LocalContext.current
    val authViewModel = remember(component, dataGraph, context) {
        AuthViewModel(dataGraph.auth, googleToken = {
            val appContext = context.applicationContext
            val webClientIdRes = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            val webClientId = if (webClientIdRes != 0) appContext.getString(webClientIdRes) else ""
            if (webClientId.isBlank()) {
                throw IllegalStateException("Google Sign-In is not configured: default_web_client_id missing in google-services.json")
            }
            GoogleIdTokenClient(webClientId).fetch(context)
        })
    }

    val authState by authViewModel.state.collectAsState()
    val currentUser = authState.user

    if (currentUser == null) {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            AuthScreen(
                state = authState,
                onGoogle = authViewModel::signInWithGoogle,
                onSignIn = { email, pass -> authViewModel.signInWithEmail(email, pass) },
                onRegister = { name, email, pass -> authViewModel.register(name, email, pass) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    } else {
        val activeUserId = currentUser.uid
        val notesFlow = remember(dataGraph, activeUserId) { dataGraph.notes.observeNotes(activeUserId) }
        val notes by notesFlow.collectAsState(initial = emptyList())

        var selectedNote by remember { mutableStateOf<Note?>(null) }
        var showSettings by remember { mutableStateOf(false) }
        var showRecord by remember { mutableStateOf(false) }
        val playbackState by playbackViewModel.playbackState.collectAsState()
        val processingUiState by processingViewModel.processingState.collectAsState()
        val settingsUiState by settingsViewModel.uiState.collectAsState()
        val autoAiEnabled by appSettings.autoAiProcessingEnabled.collectAsState()

        if (showSettings) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SettingsTab(
                    settingsUiState = settingsUiState,
                    authState = authState,
                    autoAiEnabled = autoAiEnabled,
                    onAutoAiChanged = appSettings::setAutoAiProcessingEnabled,
                    onBufferDurationChanged = settingsViewModel::setBufferDurationSeconds,
                    onKnockThresholdChanged = settingsViewModel::setKnockSensorThreshold,
                    onSignOut = authViewModel::signOut,
                    onNavigateToHome = { showSettings = false },
                )
            }
        } else if (showRecord) {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                RecordScreen(
                    recordingViewModel = recordingViewModel,
                    hasRecordAudioPermission = hasRecordAudioPermission,
                    onRequestPermission = onRequestPermission,
                    onNavigateToHome = { showRecord = false },
                    autoAiEnabled = autoAiEnabled,
                    onAutoAiChanged = appSettings::setAutoAiProcessingEnabled,
                    modifier = modifier,
                )
            }
        } else {
            HomeScreen(
                notes = notes,
                selectedNote = selectedNote,
                playbackState = playbackState,
                processingUiState = processingUiState,
                onSelectNote = { note ->
                    selectedNote = note
                    playbackViewModel.loadNote(note)
                },
                onDeleteNote = { noteId ->
                    scope.launch {
                        dataGraph.notes.deleteNote(noteId, activeUserId)
                        if (selectedNote?.id == noteId) {
                            selectedNote = null
                        }
                    }
                },
                onTogglePlayPause = playbackViewModel::togglePlayPause,
                onSeekTo = playbackViewModel::seekTo,
                onSelectChapter = playbackViewModel::selectChapter,
                onProcessNote = processingViewModel::processNote,
                onManualSave = {
                    showRecord = true
                },
                onNavigateToSettings = {
                    showSettings = true
                },
                modifier = modifier,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    KnockledgeTheme {
        HomeScreen(
            notes = emptyList(),
            selectedNote = null,
            playbackState = AudioPlaybackState(),
            processingUiState = NoteProcessingUiState(),
            onSelectNote = {},
            onDeleteNote = {},
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onProcessNote = {},
            onManualSave = {},
            onNavigateToSettings = {},
        )
    }
}
