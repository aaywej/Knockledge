package group.two.knockledge.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.WaveformPink
import group.two.knockledge.ui.theme.WaveformTeal
import kotlin.math.sin

/**
 * Functional sound wave visualization with high-density thin vertical bars reacting live to [level].
 */
@Composable
fun FunctionalWaveform(
    isRecording: Boolean,
    level: Float,
    modifier: Modifier = Modifier,
) {
    val barCount = 42
    val baseEnvelope = remember {
        FloatArray(barCount) { i ->
            val x = i.toFloat() / (barCount - 1)
            val p1 = kotlin.math.exp(-((x - 0.10f) / 0.04f) * ((x - 0.10f) / 0.04f)) * 0.95f
            val p2 = kotlin.math.exp(-((x - 0.25f) / 0.05f) * ((x - 0.25f) / 0.05f)) * 0.75f
            val p3 = kotlin.math.exp(-((x - 0.45f) / 0.06f) * ((x - 0.45f) / 0.06f)) * 0.65f
            val p4 = kotlin.math.exp(-((x - 0.72f) / 0.08f) * ((x - 0.72f) / 0.08f)) * 1.00f
            val p5 = kotlin.math.exp(-((x - 0.88f) / 0.05f) * ((x - 0.88f) / 0.05f)) * 0.60f
            val ripple = kotlin.math.abs(sin(i * 0.85f)) * 0.22f
            (0.08f + p1 + p2 + p3 + p4 + p5 + ripple).coerceIn(0.06f, 1.00f)
        }
    }

    val maxBarHeight = 150.dp
    val minBarHeight = 4.dp

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase_angle",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxBarHeight)
            .padding(vertical = 4.dp),
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val strokeWidthPx = 2.5.dp.toPx()
        val spacingPx = (totalWidth - (barCount * strokeWidthPx)) / (barCount + 1)
        val minHeightPx = minBarHeight.toPx()
        val maxHeightPx = maxBarHeight.toPx()

        for (i in 0 until barCount) {
            val x = spacingPx + i * (strokeWidthPx + spacingPx) + strokeWidthPx / 2f
            val targetHeightRatio = if (isRecording) {
                val waveOffset = sin(phase + i * 0.35f)
                val dynamicFactor = 0.35f + 0.65f * ((waveOffset + 1f) / 2f)
                val activeVol = level.coerceIn(0.12f, 1.0f)
                (baseEnvelope[i] * activeVol * dynamicFactor).coerceIn(0.05f, 1.0f)
            } else {
                0.05f
            }

            val barHeightPx = (maxHeightPx * targetHeightRatio).coerceAtLeast(minHeightPx)
            val startY = centerY - barHeightPx / 2f
            val endY = centerY + barHeightPx / 2f

            val barColor = when {
                i in 3..5 || i in 28..32 -> WaveformPink
                i in 9..12 || i in 35..38 -> WaveformTeal
                else -> ParchmentCream
            }

            drawLine(
                color = barColor,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Recording status, manual-save button, and buffer-length segmented selector.
 */
@Composable
fun RecordingControls(
    state: KnockRecordingUiState,
    onManualSave: () -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                R.string.recording_buffer_status,
                formatBufferTime(state.bufferedSeconds),
                formatBufferTime(state.maxBufferSeconds),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AccentGold,
        )

        FunctionalWaveform(
            isRecording = state.isRecording,
            level = state.level,
        )

        Text(
            text = stringResource(R.string.buffer_window_selector_title),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            val minutesList = listOf(1, 2, 3, 4, 5)
            val selectedMinutes = (state.maxBufferSeconds / 60).coerceIn(1, 5)

            minutesList.forEach { mins ->
                val isSelected = mins == selectedMinutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationChange(mins * 60) },
                    label = { Text("${mins}m") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ObsidianMidnight,
                        labelColor = ParchmentCream,
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = TextInk,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = AccentGold,
                        selectedBorderColor = AntiqueBrass,
                        enabled = true,
                        selected = isSelected,
                    ),
                )
            }
        }

        Button(
            onClick = onManualSave,
            enabled = state.isRecording && !state.isSavingSnapshot,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AntiqueBrass,
                contentColor = TextInk,
            ),
        ) {
            Text(if (state.isSavingSnapshot) "Saving..." else stringResource(R.string.save_snapshot))
        }

        when (state.lastSaveSuccess) {
            true -> Text("Saved successfully", style = MaterialTheme.typography.bodySmall, color = WaveformTeal)
            false -> Text("Not enough audio buffered yet", style = MaterialTheme.typography.bodySmall, color = WaveformPink)
            null -> Unit
        }
    }
}

private fun formatBufferTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) String.format(java.util.Locale.US, "%d:%02d", m, s) else "${s}s"
}

@Preview(showBackground = true)
@Composable
private fun RecordingControlsRecordingPreview() {
    KnockledgeTheme {
        RecordingControls(
            KnockRecordingUiState(
                isRecording = true,
                bufferedSeconds = 95,
                maxBufferSeconds = 300,
                isGestureActive = true,
                level = 0.65f,
            ),
            onManualSave = {},
            onDurationChange = {},
        )
    }
}
