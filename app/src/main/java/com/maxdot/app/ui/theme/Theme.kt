package com.maxdot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.maxdot.app.data.AppFont
import com.maxdot.app.data.SettingsState

/** Colors used for right/wrong feedback, sourced from the active theme. */
data class FeedbackColors(val correct: Color, val wrong: Color)

fun feedbackColors(theme: GameTheme) = FeedbackColors(theme.colors.success, theme.colors.hearts)

/** Legacy overload kept for existing call sites keyed on a dark flag. */
fun feedbackColors(dark: Boolean) =
    if (dark) FeedbackColors(Color(0xFF7BD88F), Color(0xFFFF8A80))
    else FeedbackColors(Color(0xFF1E7B34), Color(0xFFC62828))

fun AppFont.toFamily(): FontFamily = when (this) {
    AppFont.SERIF -> FontFamily.Serif
    AppFont.SANS -> FontFamily.SansSerif
    AppFont.MONO -> FontFamily.Monospace
    AppFont.CURSIVE -> FontFamily.Cursive
}

/** Whether the active theme is dark. Drives feedback tints and overlays. */
@Composable
fun isAppInDarkTheme(settings: SettingsState): Boolean =
    GameThemes.resolve(settings.selectedTheme).colors.dark

@Composable
fun MaxDotTheme(
    settings: SettingsState,
    content: @Composable () -> Unit,
) {
    val theme = GameThemes.resolve(settings.selectedTheme)
    CompositionLocalProvider(LocalGameTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.toColorScheme(),
            content = content,
        )
    }
}
