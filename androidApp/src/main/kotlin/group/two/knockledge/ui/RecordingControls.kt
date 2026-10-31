package group.two.knockledge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Recording status, manual-save button, and buffer-length slider.
 */
@Composable
fun RecordingControls(
    state: KnockRecordingUiState,
    onManualSave: () -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Buffered ${state.bufferedSeconds}s of ${state.maxBufferSeconds}s", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onManualSave, enabled = state.isRecording && !state.isSavingSnapshot) {
            Text(if (state.isSavingSnapshot) "Saving..." else "Save now")
        }
        when (state.lastSaveSuccess) {
            true -> Text("Saved")
            false -> Text("Not enough audio yet")
            null -> Unit
        }
        Text("Buffer length: ${state.maxBufferSeconds}s", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = state.maxBufferSeconds.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = RewindPolicy.MIN_WINDOW_SECONDS.toFloat()..RewindPolicy.MAX_WINDOW_SECONDS.toFloat(),
            steps = (RewindPolicy.MAX_WINDOW_SECONDS - RewindPolicy.MIN_WINDOW_SECONDS) / RewindPolicy.CHUNK_SECONDS - 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordingControlsRecordingPreview() {
    KnockledgeTheme {
        RecordingControls(
            KnockRecordingUiState(isRecording = true, bufferedSeconds = 95, maxBufferSeconds = 180, isGestureActive = true),
            onManualSave = {},
            onDurationChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordingControlsSavingPreview() {
    KnockledgeTheme {
        RecordingControls(
            KnockRecordingUiState(isRecording = true, bufferedSeconds = 300, isSavingSnapshot = true, lastSaveSuccess = true),
            onManualSave = {},
            onDurationChange = {},
        )
    }
}
