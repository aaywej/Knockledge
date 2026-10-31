package group.two.knockledge.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.ui.KnockRecordingController
import group.two.knockledge.ui.KnockRecordingUiState
import group.two.knockledge.ui.LiveTestController
import group.two.knockledge.ui.LiveTestUiState
import group.two.knockledge.ui.RecordingControls
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Record and Knock test tab displaying real-time audio buffering controls
 * and accelerometer knock gesture feedback.
 */
@Composable
fun RecordTab(
    recordingUiState: KnockRecordingUiState,
    liveTestUiState: LiveTestUiState,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onManualSave: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onSimulateKnock: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    currentTimeProvider: () -> Long = System::currentTimeMillis,
) {
    val scrollState = rememberScrollState()
    val isKnockPulseActive = remember(liveTestUiState.lastKnockEpochMs, currentTimeProvider()) {
        val lastMs = liveTestUiState.lastKnockEpochMs
        lastMs != null && (currentTimeProvider() - lastMs) < 1500L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Permission Request Warning
        if (!liveTestUiState.hasRecordAudioPermission) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.permission_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(text = stringResource(R.string.grant_permission))
                    }
                }
            }
        }

        // Title and Session Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.record_tab_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = if (recordingUiState.isRecording) onStopSession else onStartSession,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (recordingUiState.isRecording) {
                                stringResource(R.string.stop_session)
                            } else {
                                stringResource(R.string.start_session)
                            },
                        )
                    }
                }
            }
        }

        // Recording Buffer Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            RecordingControls(
                state = recordingUiState,
                onManualSave = onManualSave,
                onDurationChange = onDurationChange,
            )
        }

        // Knock Feedback and Trigger Test Card
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
                    text = "Knock Gesture Status & Simulation",
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.knock_count_label, liveTestUiState.knockCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    AnimatedVisibility(
                        visible = isKnockPulseActive,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = stringResource(R.string.knock_pulse_detected),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSimulateKnock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.simulate_knock))
                }
            }
        }
    }
}

/**
 * Convenience stateful overload for [RecordTab].
 */
@Composable
fun RecordTab(
    recordingViewModel: KnockRecordingController,
    liveTestController: LiveTestController,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recordingUiState by recordingViewModel.uiState.collectAsState()
    val liveTestUiState by liveTestController.uiState.collectAsState()

    RecordTab(
        recordingUiState = recordingUiState,
        liveTestUiState = liveTestUiState,
        onStartSession = {
            if (liveTestUiState.hasRecordAudioPermission) {
                recordingViewModel.startSession()
            } else {
                onRequestPermission()
            }
        },
        onStopSession = recordingViewModel::stopSession,
        onManualSave = recordingViewModel::triggerManualSave,
        onDurationChange = recordingViewModel::setBufferDurationSeconds,
        onSimulateKnock = {
            liveTestController.onKnockTriggered()
            recordingViewModel.triggerManualSave()
        },
        onRequestPermission = onRequestPermission,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordTabPreview() {
    KnockledgeTheme {
        RecordTab(
            recordingUiState = KnockRecordingUiState(
                isRecording = true,
                bufferedSeconds = 45,
                maxBufferSeconds = 300,
            ),
            liveTestUiState = LiveTestUiState(
                hasRecordAudioPermission = true,
                knockCount = 2,
            ),
            onStartSession = {},
            onStopSession = {},
            onManualSave = {},
            onDurationChange = {},
            onSimulateKnock = {},
            onRequestPermission = {},
        )
    }
}
