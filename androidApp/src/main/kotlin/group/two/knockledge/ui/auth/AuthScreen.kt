package group.two.knockledge.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Sign-in / registration screen with email, password, and Google sign-in.
 */
@Composable
fun AuthScreen(
    state: AuthState,
    onGoogle: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(if (registering) "Create account" else "Sign in", style = MaterialTheme.typography.headlineMedium)
        if (registering) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
        }
        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.collisionNotice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { if (registering) onRegister(name, email, password) else onSignIn(email, password) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (registering) "Register" else "Sign in") }
        OutlinedButton(onClick = onGoogle, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Google")
        }
        TextButton(onClick = { registering = !registering }) {
            Text(if (registering) "Have an account? Sign in" else "New here? Create account")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    KnockledgeTheme { AuthScreen(AuthState(error = "Enter a valid email address"), {}, { _, _ -> }, { _, _, _ -> }) }
}
