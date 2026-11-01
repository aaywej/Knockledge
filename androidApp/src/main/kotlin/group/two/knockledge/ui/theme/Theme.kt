package group.two.knockledge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AntiqueBrass,
    onPrimary = TextInk,
    primaryContainer = BrassContainer,
    onPrimaryContainer = ParchmentCream,
    secondary = ParchmentCream,
    onSecondary = TextInk,
    secondaryContainer = SurfaceContainerHigh,
    onSecondaryContainer = ParchmentCream,
    tertiary = WaveformTeal,
    onTertiary = TextInk,
    background = ObsidianMidnight,
    onBackground = ParchmentCream,
    surface = ObsidianMidnight,
    onSurface = ParchmentCream,
    surfaceVariant = SurfaceIndigo,
    onSurfaceVariant = MutedSand,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = AntiqueBrass,
    outlineVariant = OutlineVariant,
    error = ErrorRed,
    onError = TextInk,
)

// Knockledge enforces dark obsidian terminal aesthetic by default
private val LightColorScheme = DarkColorScheme

/**
 * Knockledge Material 3 theme.
 * Dynamic color is disabled by default to preserve the Obsidian & Brass identity.
 */
@Composable
fun KnockledgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
