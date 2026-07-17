package com.maxdot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.maxdot.app.data.AppFont
import com.maxdot.app.data.SettingsState
import com.maxdot.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B3A5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3F8),
    onPrimaryContainer = Color(0xFF0A2540),
    secondary = Color(0xFF8A6D1D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E8B8),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF5C4A72),
    tertiaryContainer = Color(0xFFE9DDF7),
    onTertiaryContainer = Color(0xFF2A1B3D),
    surface = Color(0xFFFBF8F2),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEFE9DC),
    onSurfaceVariant = Color(0xFF4C4639),
    background = Color(0xFFFBF8F2),
    onBackground = Color(0xFF1C1B18),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FC2EC),
    onPrimary = Color(0xFF0A2540),
    primaryContainer = Color(0xFF2C4A6E),
    onPrimaryContainer = Color(0xFFD3E3F8),
    secondary = Color(0xFFE8CB6E),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF5C4A16),
    onSecondaryContainer = Color(0xFFF8E8B8),
    tertiary = Color(0xFFCDB8E8),
    tertiaryContainer = Color(0xFF44335C),
    onTertiaryContainer = Color(0xFFE9DDF7),
    surface = Color(0xFF15140F),
    onSurface = Color(0xFFE7E2D9),
    surfaceVariant = Color(0xFF2A2820),
    onSurfaceVariant = Color(0xFFCEC6B4),
    background = Color(0xFF15140F),
    onBackground = Color(0xFFE7E2D9),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/** Colors used for right/wrong feedback, tuned per theme. */
data class FeedbackColors(val correct: Color, val wrong: Color)

fun feedbackColors(dark: Boolean) =
    if (dark) FeedbackColors(Color(0xFF7BD88F), Color(0xFFFF8A80))
    else FeedbackColors(Color(0xFF1E7B34), Color(0xFFC62828))

fun AppFont.toFamily(): FontFamily = when (this) {
    AppFont.SERIF -> FontFamily.Serif
    AppFont.SANS -> FontFamily.SansSerif
    AppFont.MONO -> FontFamily.Monospace
    AppFont.CURSIVE -> FontFamily.Cursive
}

@Composable
fun isAppInDarkTheme(settings: SettingsState): Boolean = when (settings.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun MaxDotTheme(
    settings: SettingsState,
    content: @Composable () -> Unit,
) {
    val dark = isAppInDarkTheme(settings)
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
