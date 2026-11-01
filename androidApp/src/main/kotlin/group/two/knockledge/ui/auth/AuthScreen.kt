package group.two.knockledge.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.MutedSand
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.OutlineVariant
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainer
import group.two.knockledge.ui.theme.SurfaceIndigo
import group.two.knockledge.ui.theme.TextInk

/**
 * Sign-in / registration screen adhering to the Obsidian & Brass terminal aesthetic
 * enhanced with Material 3 design principles.
 */
@Composable
fun AuthScreen(
    state: AuthState,
    onGoogle: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val fieldShape = MaterialTheme.shapes.extraSmall
    val buttonShape = MaterialTheme.shapes.extraSmall

    val errorMessage = state.error ?: state.collisionNotice
    val isError = errorMessage != null

    val submitForm = remember(registering, name, email, password, onRegister, onSignIn, focusManager) {
        {
            focusManager.clearFocus()
            if (registering) {
                onRegister(name, email, password)
            } else {
                onSignIn(email, password)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianMidnight),
    ) {
        // Ambient Gold Top Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentGold.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        // Ambient Gold Bottom Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AccentGold.copy(alpha = 0.08f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            // Top Bar Navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.busy,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterStart)
                            .border(1.dp, OutlineVariant, buttonShape)
                            .background(SurfaceContainer, buttonShape),
                    ) {
                        BackArrowIcon(tint = ParchmentCream)
                    }
                }

                Text(
                    text = stringResource(
                        if (registering) R.string.auth_register_title else R.string.auth_sign_in_title
                    ),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = ParchmentCream,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            }

            // Subtitle Description
            Text(
                text = stringResource(
                    if (registering) R.string.auth_register_subtitle else R.string.auth_sign_in_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Auth Form Container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Display Name Field with Animated Visibility
                AnimatedVisibility(
                    visible = registering,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.auth_display_name_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentGold,
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = isError,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.auth_display_name_placeholder),
                                    color = MutedSand.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            shape = fieldShape,
                            colors = textFieldColors(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Email Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.auth_email_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.auth_email_placeholder),
                                color = MutedSand.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        shape = fieldShape,
                        colors = textFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Password Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.auth_password_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitForm() }
                        ),
                        trailingIcon = {
                            val toggleDesc = stringResource(R.string.auth_password_toggle_cd)
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = !state.busy,
                                modifier = Modifier.semantics { contentDescription = toggleDesc },
                            ) {
                                PasswordEyeIcon(
                                    visible = passwordVisible,
                                    tint = AccentGold,
                                )
                            }
                        },
                        shape = fieldShape,
                        colors = textFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }


                // Error Notice
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Primary Submit Action
                Button(
                    onClick = { submitForm() },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(elevation = 8.dp, shape = buttonShape, spotColor = AccentGold.copy(alpha = 0.25f)),
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AntiqueBrass,
                        contentColor = TextInk,
                        disabledContainerColor = AntiqueBrass.copy(alpha = 0.5f),
                        disabledContentColor = TextInk.copy(alpha = 0.5f),
                    ),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextInk,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (registering) R.string.auth_create_account_button else R.string.auth_sign_in_button
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // Visual Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AntiqueBrass.copy(alpha = 0.4f),
                    )
                    Text(
                        text = stringResource(R.string.auth_or_with_google),
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSand,
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = AntiqueBrass.copy(alpha = 0.4f),
                    )
                }

                // Google Sign In Action Below Form
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onGoogle()
                    },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = buttonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceContainer,
                        contentColor = ParchmentCream,
                        disabledContainerColor = SurfaceContainer.copy(alpha = 0.5f),
                        disabledContentColor = ParchmentCream.copy(alpha = 0.5f),
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueBrass),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GoogleLogoIcon()
                        Text(
                            text = stringResource(R.string.auth_sign_in_with_google),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Registration / Sign In Mode Switch
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        if (registering) R.string.auth_have_account else R.string.auth_new_to_knockledge
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedButton(
                    onClick = { registering = !registering },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = buttonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceContainer,
                        contentColor = ParchmentCream,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueBrass),
                ) {
                    Text(
                        text = stringResource(
                            if (registering) R.string.auth_sign_in_button else R.string.auth_create_account_button
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceIndigo,
    unfocusedContainerColor = SurfaceIndigo,
    disabledContainerColor = SurfaceIndigo.copy(alpha = 0.5f),
    focusedBorderColor = AntiqueBrass,
    unfocusedBorderColor = AntiqueBrass.copy(alpha = 0.4f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    cursorColor = AccentGold,
    focusedTextColor = ParchmentCream,
    unfocusedTextColor = ParchmentCream,
    focusedLabelColor = AccentGold,
    unfocusedLabelColor = AccentGold,
)

@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        // Shaft
        drawLine(
            color = tint,
            start = Offset(w * 0.85f, h * 0.5f),
            end = Offset(w * 0.15f, h * 0.5f),
            strokeWidth = strokeWidth,
        )
        // Upper arm
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = strokeWidth,
        )
        // Lower arm
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun PasswordEyeIcon(
    visible: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val w = size.width
        val h = size.height

        // Upper arc
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.1f, h * 0.2f),
            size = Size(w * 0.8f, h * 0.6f),
            style = Stroke(width = strokeWidth),
        )
        // Lower arc
        drawArc(
            color = tint,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.1f, h * 0.2f),
            size = Size(w * 0.8f, h * 0.6f),
            style = Stroke(width = strokeWidth),
        )
        // Iris / pupil
        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.5f),
        )
        // Slash line when password is key-masked
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(w * 0.2f, h * 0.8f),
                end = Offset(w * 0.8f, h * 0.2f),
                strokeWidth = strokeWidth * 1.2f,
            )
        }
    }
}

@Composable
private fun GoogleLogoIcon(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.22f
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(w - strokeWidth, h - strokeWidth)
        val center = Offset(w / 2f, h / 2f)

        // Red arc (top)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 190f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        // Yellow arc (left bottom)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        // Green arc (bottom right)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 20f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        // Blue arc & crossbar (right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -50f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        // Blue bar in center
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(w - strokeWidth / 2f, center.y),
            strokeWidth = strokeWidth,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    KnockledgeTheme {
        AuthScreen(
            state = AuthState(error = "Enter a valid email address"),
            onGoogle = {},
            onSignIn = { _, _ -> },
            onRegister = { _, _, _ -> },
            onBack = null,
        )
    }
}
