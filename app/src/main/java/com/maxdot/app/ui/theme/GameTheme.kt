package com.maxdot.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp

/** The player-selectable visual identities. */
enum class GameThemeId(val displayName: String) {
    PLAYFUL("Playful Pop"),
    NEON("Neon Arcade"),
    RETRO("Retro Quest"),
}

/** How primary buttons render for a theme. */
enum class ButtonStyle {
    /** Soft fill with a darker bottom "lip" that presses down. */
    PRESSABLE_3D,

    /** Bright fill wrapped in a layered translucent glow. */
    NEON_GLOW,

    /** Flat fill, sharp corners, 2px block border. */
    PIXEL_BLOCK,
}

/** The shape of world-map / progress nodes for a theme. */
enum class NodeShape { SQUIRCLE, GLOW_CIRCLE, DIAMOND }

/**
 * The full color token set for a theme. Every surface, accent, and text tier the
 * UI needs. [dark] drives the Material 3 base scheme; [glow] enables the soft
 * outer-glow treatment behind accents (Neon only).
 */
data class GameColors(
    val bg: List<Color>,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val hearts: Color,
    val success: Color,
    val warning: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val errorHighlight: Color,
    val dark: Boolean,
    val glow: Boolean,
)

/** Font roles per theme. Prose stays legible even when display/HUD go stylized. */
data class GameTypography(
    val display: FontFamily,
    val hud: FontFamily,
    val body: FontFamily,
)

/** Shape language: button treatment, node silhouette, corner radii. */
data class GameShapes(
    val button: ButtonStyle,
    val node: NodeShape,
    val cardCorner: Dp,
    val controlCorner: Dp,
)

/** A complete theme the whole UI reads its design tokens from. */
data class GameTheme(
    val id: GameThemeId,
    val name: String,
    val colors: GameColors,
    val type: GameTypography,
    val shapes: GameShapes,
)

/**
 * The active theme, provided by [MaxDotTheme]. Every themed composable reads
 * `LocalGameTheme.current`. Defaults to Playful Pop.
 */
val LocalGameTheme = staticCompositionLocalOf { GameThemes.PLAYFUL }
