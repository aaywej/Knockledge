package group.two.knockledge.ui.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.ui.FunctionalWaveform
import group.two.knockledge.ui.KnockRecordingController
import group.two.knockledge.ui.KnockRecordingUiState
import group.two.knockledge.ui.LiveTestController
import group.two.knockledge.ui.LiveTestUiState
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainerHigh
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.WaveformTeal
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private data class SuctionParticle(
    val initialAngleRad: Float,
    val initialRadiusDp: Float,
    val speedMultiplier: Float,
    val particleSizeDp: Float,
    val color: Color,
)

/**
 * Custom Canvas composable that draws a pixelated retro ring contour matching Figma mockup styling.
 */
@Composable
fun PixelatedRing(
    radius: Dp,
    pixelSize: Dp,
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
            val rawX = radiusPx * cos(angle)
            val rawY = radiusPx * sin(angle)
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
 * Recording screen following Figma design (Knockledge UI - Frame "3 Recording")
 * with pixelated retro ring contour, central mic ring acting as the Capture trigger,
 * glowing ambient halo, black hole particle vortex suction animation, compact Auto AI toggle,
 * functional live sound wave, and stacked capture cards.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun RecordTab(
    recordingUiState: KnockRecordingUiState,
    liveTestUiState: LiveTestUiState,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onManualSave: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    autoAiEnabled: Boolean = true,
    onAutoAiChanged: (Boolean) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onSimulateKnock: () -> Unit = {},
    onNavSettings: (() -> Unit)? = null,
    onNavProfile: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var isSinking by remember { mutableStateOf(false) }

    val suctionAnim = remember { Animatable(0f) }
    val particles = remember {
        val rng = Random(42L)
        val particleColors = listOf(AccentGold, AntiqueBrass, WaveformTeal, ParchmentCream)
        List(32) {
            SuctionParticle(
                initialAngleRad = rng.nextFloat() * (2f * Math.PI.toFloat()),
                initialRadiusDp = 95f + rng.nextFloat() * 55f,
                speedMultiplier = 0.85f + rng.nextFloat() * 0.45f,
                particleSizeDp = 2.5f + rng.nextFloat() * 2.5f,
                color = particleColors[rng.nextInt(particleColors.size)],
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )
    val glowHaloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_halo_alpha",
    )

    // Tactile sinking / suction click physics
    val buttonPressScale by animateFloatAsState(
        targetValue = if (isSinking) 0.82f else 1.0f,
        animationSpec = if (isSinking) {
            tween(durationMillis = 100, easing = FastOutLinearInEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "button_press_scale",
    )

    val suctionDepthAlpha by animateFloatAsState(
        targetValue = if (isSinking) 0.50f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "suction_depth_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianMidnight),
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentGold.copy(alpha = 0.08f),
                            Color.Transparent,
                            AccentGold.copy(alpha = 0.06f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Permission Banner
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

            // Session Header Timer
            Text(
                text = stringResource(
                    R.string.recording_session_timer,
                    formatTimerSeconds(recordingUiState.bufferedSeconds),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGold,
                textAlign = TextAlign.Center,
            )

            // Central Mic Ring Container + Glowing Halo & Black Hole Suction Vortex
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(buttonPressScale),
                contentAlignment = Alignment.Center,
            ) {
                // Outer Radial Glowing Halo
                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(if (recordingUiState.isRecording) pulseScale else 1f),
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGold.copy(
                                    alpha = (if (recordingUiState.isRecording) glowHaloAlpha else 0.35f) * suctionDepthAlpha,
                                ),
                                AntiqueBrass.copy(
                                    alpha = (if (recordingUiState.isRecording) glowHaloAlpha * 0.5f else 0.15f) * suctionDepthAlpha,
                                ),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.width / 2f,
                        ),
                        radius = size.width / 2f,
                    )
                }

                // Black hole particle suction overlay
                if (suctionAnim.value > 0f && suctionAnim.value < 1f) {
                    val progress = suctionAnim.value
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        particles.forEach { p ->
                            val pProgress = (progress * p.speedMultiplier).coerceIn(0f, 1f)
                            if (pProgress in 0.01f..0.99f) {
                                val currentRadiusPx = p.initialRadiusDp.dp.toPx() * (1f - pProgress.toDouble().pow(2.2).toFloat())
                                val currentAngle = p.initialAngleRad + pProgress * 1.8f
                                val px = centerX + currentRadiusPx * cos(currentAngle)
                                val py = centerY + currentRadiusPx * sin(currentAngle)
                                val alpha = (1f - pProgress * pProgress) * 0.92f
                                val radiusPx = (p.particleSizeDp.dp.toPx() * (1f - pProgress * 0.5f)).coerceAtLeast(1.5f)

                                drawCircle(
                                    color = p.color.copy(alpha = alpha),
                                    radius = radiusPx,
                                    center = Offset(px, py),
                                )
                            }
                        }
                    }
                }

                // Interactive Mic Ring Button with Pixelated Rings
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = !recordingUiState.isSavingSnapshot,
                            onClick = {
                                scope.launch {
                                    isSinking = true
                                    suctionAnim.snapTo(0f)
                                    suctionAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 480,
                                            easing = FastOutLinearInEasing,
                                        ),
                                    )
                                    isSinking = false
                                }
                                if (recordingUiState.isRecording) {
                                    onManualSave()
                                } else {
                                    onStartSession()
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Outer Pixelated Ring
                    PixelatedRing(
                        radius = 76.dp,
                        pixelSize = 3.dp,
                        color = AccentGold.copy(
                            alpha = (if (recordingUiState.isRecording) pulseAlpha else 0.5f) * suctionDepthAlpha,
                        ),
                        modifier = Modifier
                            .size(160.dp)
                            .scale(if (recordingUiState.isRecording) pulseScale else 1f),
                    )

                    // Inner Pixelated Ring
                    PixelatedRing(
                        radius = 64.dp,
                        pixelSize = 3.dp,
                        color = AccentGold.copy(
                            alpha = (if (recordingUiState.isRecording) (pulseAlpha * 0.8f) else 0.35f) * suctionDepthAlpha,
                        ),
                        modifier = Modifier
                            .size(136.dp)
                            .scale(if (recordingUiState.isRecording) (pulseScale * 0.98f) else 1f),
                    )

                    // Center Mic Pill Icon - Glowing Parchment Cream
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(40.dp)
                            .scale(if (isSinking) 0.85f else 1.0f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ParchmentCream.copy(alpha = suctionDepthAlpha)),
                    )
                }
            }

            // Status Texts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (recordingUiState.isRecording) {
                        stringResource(R.string.recording_status_active)
                    } else {
                        stringResource(R.string.recording_status_inactive)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = ParchmentCream,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(
                        R.string.recording_buffer_status,
                        formatBufferTime(recordingUiState.bufferedSeconds),
                        formatBufferTime(recordingUiState.maxBufferSeconds),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = if (recordingUiState.isGestureActive) {
                        stringResource(R.string.recording_gesture_status_active)
                    } else {
                        stringResource(R.string.recording_gesture_status_inactive)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                    textAlign = TextAlign.Center,
                )
            }

            // Compact Auto AI Processing Quick Toggle
            FilterChip(
                selected = autoAiEnabled,
                onClick = { onAutoAiChanged(!autoAiEnabled) },
                label = {
                    Text(
                        text = if (autoAiEnabled) "Auto AI: ON" else "Auto AI: OFF",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ObsidianMidnight,
                    labelColor = ParchmentCream,
                    selectedContainerColor = AccentGold,
                    selectedLabelColor = TextInk,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = AccentGold.copy(alpha = 0.5f),
                    selectedBorderColor = AntiqueBrass,
                    enabled = true,
                    selected = autoAiEnabled,
                ),
            )

            // Functional Sound Waveform Component
            FunctionalWaveform(
                isRecording = recordingUiState.isRecording,
                level = recordingUiState.level,
            )

            // Hint Text
            Text(
                text = stringResource(R.string.recording_hint_text),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGold,
                textAlign = TextAlign.Center,
            )

            // Stacked Capture Cards directly on layout (No outer container card)
            StackedCaptureDeck()

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Session Control Button
            Button(
                onClick = if (recordingUiState.isRecording) onStopSession else onStartSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AntiqueBrass,
                    contentColor = TextInk,
                ),
            ) {
                Text(
                    text = if (recordingUiState.isRecording) {
                        stringResource(R.string.stop_session)
                    } else {
                        stringResource(R.string.start_session)
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Individual capture row card following original Figma styling with spacious breathing room.
 */
@Composable
fun CaptureRowCard(
    title: String,
    date: String,
    duration: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerHigh, RoundedCornerShape(8.dp))
            .border(
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(AccentGold, AntiqueBrass)),
                ),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val triangle = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(triangle, color = AccentGold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ParchmentCream,
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold,
            )
        }
    }
}

/**
 * Stacked capture deck rendering recording cards directly with an overlaying effect
 * (front active card peeking over 1 previous card underneath) without an outer container card.
 */
@Composable
fun StackedCaptureDeck(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 1. Back Card (Previous recording peeking underneath)
        CaptureRowCard(
            title = "Cell Division",
            date = "OCT20 16:30",
            duration = "0:42",
            status = stringResource(R.string.processed_label),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .offset(y = 28.dp)
                .alpha(0.60f),
        )

        // 2. Front Card (Most recent recording on top)
        CaptureRowCard(
            title = "Photosynthesis",
            date = "OCT21 12:04",
            duration = "0:25",
            status = stringResource(R.string.unprocessed_label),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 0.dp),
        )
    }
}

private fun formatTimerSeconds(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun formatBufferTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) String.format(Locale.US, "%d:%02d", m, s) else "${s}s"
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
    autoAiEnabled: Boolean = true,
    onAutoAiChanged: (Boolean) -> Unit = {},
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
        onRequestPermission = onRequestPermission,
        modifier = modifier,
        autoAiEnabled = autoAiEnabled,
        onAutoAiChanged = onAutoAiChanged,
        onDurationChange = recordingViewModel::setBufferDurationSeconds,
        onSimulateKnock = {
            liveTestController.onKnockTriggered()
            recordingViewModel.triggerManualSave()
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordTabPreview() {
    KnockledgeTheme {
        RecordTab(
            recordingUiState = KnockRecordingUiState(
                isRecording = true,
                bufferedSeconds = 222,
                maxBufferSeconds = 300,
                level = 0.65f,
            ),
            liveTestUiState = LiveTestUiState(
                hasRecordAudioPermission = true,
                knockCount = 2,
            ),
            onStartSession = {},
            onStopSession = {},
            onManualSave = {},
            onRequestPermission = {},
        )
    }
}
