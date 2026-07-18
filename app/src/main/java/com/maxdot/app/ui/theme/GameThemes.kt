package com.maxdot.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** The three built-in game themes and helpers to look them up and bridge to M3. */
object GameThemes {

    val PLAYFUL = GameTheme(
        id = GameThemeId.PLAYFUL,
        name = GameThemeId.PLAYFUL.displayName,
        colors = GameColors(
            bg = listOf(Color(0xFFF6F0FF), Color(0xFFEDE4FF)),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFEDE6FB),
            primary = Color(0xFF6C4CF1),
            onPrimary = Color(0xFFFFFFFF),
            accent = Color(0xFFFFC531),
            hearts = Color(0xFFFF5A6E),
            success = Color(0xFF22C39A),
            warning = Color(0xFFFF9F1C),
            textPrimary = Color(0xFF2E2542),
            textSecondary = Color(0xFF6E6588),
            errorHighlight = Color(0xFFFFE1E5),
            dark = false,
            glow = false,
        ),
        type = GameTypography(
            display = FontFamily.SansSerif,
            hud = FontFamily.SansSerif,
            body = FontFamily.SansSerif,
        ),
        shapes = GameShapes(
            button = ButtonStyle.PRESSABLE_3D,
            node = NodeShape.SQUIRCLE,
            cardCorner = 16.dp,
            controlCorner = 14.dp,
        ),
    )

    val NEON = GameTheme(
        id = GameThemeId.NEON,
        name = GameThemeId.NEON.displayName,
        colors = GameColors(
            bg = listOf(Color(0xFF0B0F1C), Color(0xFF10182F)),
            surface = Color(0xFF121A2E),
            surfaceAlt = Color(0xFF1B2340),
            primary = Color(0xFF25E0D4),
            onPrimary = Color(0xFF04121A),
            accent = Color(0xFFFF3D8B),
            hearts = Color(0xFFFF3D8B),
            success = Color(0xFFB6FF3D),
            warning = Color(0xFFFFC531),
            textPrimary = Color(0xFFDCE6FA),
            textSecondary = Color(0xFF8FA0C8),
            errorHighlight = Color(0xFF3A1020),
            dark = true,
            glow = true,
        ),
        type = GameTypography(
            display = FontFamily.SansSerif,
            hud = FontFamily.SansSerif,
            body = FontFamily.SansSerif,
        ),
        shapes = GameShapes(
            button = ButtonStyle.NEON_GLOW,
            node = NodeShape.GLOW_CIRCLE,
            cardCorner = 14.dp,
            controlCorner = 12.dp,
        ),
    )

    val RETRO = GameTheme(
        id = GameThemeId.RETRO,
        name = GameThemeId.RETRO.displayName,
        colors = GameColors(
            bg = listOf(Color(0xFF161233), Color(0xFF161233)),
            surface = Color(0xFF0E0A26),
            surfaceAlt = Color(0xFF1E1642),
            primary = Color(0xFFF5C542),
            onPrimary = Color(0xFF161233),
            accent = Color(0xFF9B6BFF),
            hearts = Color(0xFFF5C542),
            success = Color(0xFF5CE08B),
            warning = Color(0xFFF5C542),
            textPrimary = Color(0xFFE8E0FF),
            textSecondary = Color(0xFFB7A9E0),
            errorHighlight = Color(0xFF3A0E1C),
            dark = true,
            glow = false,
        ),
        type = GameTypography(
            display = FontFamily.Monospace,
            hud = FontFamily.Monospace,
            body = FontFamily.SansSerif,
        ),
        shapes = GameShapes(
            button = ButtonStyle.PIXEL_BLOCK,
            node = NodeShape.DIAMOND,
            cardCorner = 0.dp,
            controlCorner = 0.dp,
        ),
    )

    val ALL: List<GameTheme> = listOf(PLAYFUL, NEON, RETRO)

    fun byId(id: GameThemeId): GameTheme = when (id) {
        GameThemeId.PLAYFUL -> PLAYFUL
        GameThemeId.NEON -> NEON
        GameThemeId.RETRO -> RETRO
    }

    fun resolve(id: GameThemeId): GameTheme = byId(id)
}

/**
 * Derives a Material 3 [ColorScheme] from the theme so stock M3 components
 * (dialogs, snackbars, chips, sliders) stay on-brand automatically.
 */
fun GameTheme.toColorScheme(): ColorScheme {
    val c = colors
    val base = if (c.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.surfaceAlt,
        onPrimaryContainer = c.textPrimary,
        secondary = c.accent,
        onSecondary = c.onPrimary,
        secondaryContainer = c.surfaceAlt,
        onSecondaryContainer = c.textPrimary,
        tertiary = c.accent,
        tertiaryContainer = c.surfaceAlt,
        onTertiaryContainer = c.primary,
        background = c.bg.first(),
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceAlt,
        onSurfaceVariant = c.textSecondary,
        error = c.hearts,
        onError = c.onPrimary,
        outline = c.textSecondary,
    )
}
