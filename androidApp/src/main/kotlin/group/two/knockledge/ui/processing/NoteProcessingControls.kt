package group.two.knockledge.ui.processing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.pipeline.RewindStage

/**
 * UI Controls for on-demand AI processing trigger, progress status, and error retry handling.
 */
@Composable
fun NoteProcessingControls(
    uiState: NoteProcessingUiState,
    onProcessNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        when {
            uiState.isProcessing -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(24.dp).height(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val stageText = if (uiState.stage == RewindStage.Transcribing) {
                            stringResource(R.string.processing_transcribing)
                        } else {
                            stringResource(R.string.processing_summarizing)
                        }
                        Text(
                            text = stageText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            uiState.state == NoteAiProcessingState.FAILED -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = uiState.errorMessage ?: stringResource(R.string.processing_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onProcessNote,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }

            uiState.isSuccess || uiState.state == NoteAiProcessingState.COMPLETED -> {
                Text(
                    text = stringResource(R.string.processing_completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            else -> {
                OutlinedButton(
                    onClick = onProcessNote,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.process_ai_notes))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoteProcessingControlsIdlePreview() {
    MaterialTheme {
        NoteProcessingControls(
            uiState = NoteProcessingUiState(state = NoteAiProcessingState.IDLE),
            onProcessNote = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteProcessingControlsTranscribingPreview() {
    MaterialTheme {
        NoteProcessingControls(
            uiState = NoteProcessingUiState(
                isProcessing = true,
                state = NoteAiProcessingState.TRANSCRIBING,
                stage = RewindStage.Transcribing,
            ),
            onProcessNote = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteProcessingControlsFailedPreview() {
    MaterialTheme {
        NoteProcessingControls(
            uiState = NoteProcessingUiState(
                state = NoteAiProcessingState.FAILED,
                errorMessage = "Network timeout while generating summary.",
            ),
            onProcessNote = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteProcessingControlsCompletedPreview() {
    MaterialTheme {
        NoteProcessingControls(
            uiState = NoteProcessingUiState(
                state = NoteAiProcessingState.COMPLETED,
                isSuccess = true,
            ),
            onProcessNote = {},
        )
    }
}
