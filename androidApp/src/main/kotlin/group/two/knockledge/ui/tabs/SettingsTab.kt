package group.two.knockledge.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.ui.auth.AuthState
import group.two.knockledge.ui.settings.AiProcessingSettingsControl
import group.two.knockledge.ui.settings.SettingsUiState
import group.two.knockledge.ui.theme.KnockledgeTheme
import java.util.Locale

/**
 * Utilitarian settings tab displaying active user profile and runtime options.
 */
@Composable
fun SettingsTab(
    settingsUiState: SettingsUiState,
    authState: AuthState,
    autoAiEnabled: Boolean,
    onAutoAiChanged: (Boolean) -> Unit,
    onBufferDurationChanged: (Int) -> Unit,
    onKnockThresholdChanged: (Float) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_tab_title),
            style = MaterialTheme.typography.titleMedium,
        )

        // Account Profile Card
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
                    text = stringResource(R.string.account_status_title),
                    style = MaterialTheme.typography.titleSmall,
                )

                val user = authState.user
                if (user != null) {
                    Text(
                        text = stringResource(
                            R.string.signed_in_as,
                            user.displayName.ifBlank { user.email },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(text = stringResource(R.string.sign_out))
                    }
                }
            }
        }

        // Runtime Settings & AI Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Runtime & AI Settings",
                    style = MaterialTheme.typography.titleSmall,
                )

                // Auto-AI Processing Switch
                AiProcessingSettingsControl(
                    enabled = autoAiEnabled,
                    onEnabledChanged = onAutoAiChanged,
                )

                HorizontalDivider()

                // Buffer Duration Slider
                Column {
                    Text(
                        text = "${stringResource(R.string.buffer_duration_setting)}: ${settingsUiState.bufferDurationSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = settingsUiState.bufferDurationSeconds.toFloat(),
                        onValueChange = { onBufferDurationChanged(it.toInt()) },
                        valueRange = RewindPolicy.MIN_WINDOW_SECONDS.toFloat()..RewindPolicy.MAX_WINDOW_SECONDS.toFloat(),
                        steps = (RewindPolicy.MAX_WINDOW_SECONDS - RewindPolicy.MIN_WINDOW_SECONDS) / RewindPolicy.CHUNK_SECONDS - 1,
                    )
                }

                HorizontalDivider()

                // Knock Sensor Threshold Slider
                Column {
                    Text(
                        text = "${stringResource(R.string.knock_threshold_setting)}: ${String.format(Locale.US, "%.1fg", settingsUiState.knockSensorThreshold)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.knock_threshold_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = settingsUiState.knockSensorThreshold,
                        onValueChange = onKnockThresholdChanged,
                        valueRange = 1.0f..5.0f,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsTabPreview() {
    KnockledgeTheme {
        SettingsTab(
            settingsUiState = SettingsUiState(
                bufferDurationSeconds = 300,
                knockSensorThreshold = 2.5f,
            ),
            authState = AuthState(),
            autoAiEnabled = true,
            onAutoAiChanged = {},
            onBufferDurationChanged = {},
            onKnockThresholdChanged = {},
            onSignOut = {},
        )
    }
}
