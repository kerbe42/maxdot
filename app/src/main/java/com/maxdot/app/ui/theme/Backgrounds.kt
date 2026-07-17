package com.maxdot.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** An unlockable reading background, with variants for light and dark mode. */
data class BackgroundTheme(
    val id: String,
    val name: String,
    val unlockLevel: Int,
    val lightColors: List<Color>,
    val darkColors: List<Color>,
) {
    fun brush(dark: Boolean): Brush =
        Brush.verticalGradient(if (dark) darkColors else lightColors)

    fun previewBrush(dark: Boolean): Brush = brush(dark)
}

object Backgrounds {

    val ALL: List<BackgroundTheme> = listOf(
        BackgroundTheme(
            "parchment", "Parchment", 1,
            listOf(Color(0xFFFBF6E9), Color(0xFFF1E6CC)),
            listOf(Color(0xFF1C1A14), Color(0xFF262117)),
        ),
        BackgroundTheme(
            "midnight", "Midnight Ink", 2,
            listOf(Color(0xFFE8EDF5), Color(0xFFC9D6EA)),
            listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)),
        ),
        BackgroundTheme(
            "ocean", "Ocean Depths", 4,
            listOf(Color(0xFFE0F4F5), Color(0xFFB3DFE3)),
            listOf(Color(0xFF04252E), Color(0xFF0A3D45)),
        ),
        BackgroundTheme(
            "evergreen", "Evergreen", 6,
            listOf(Color(0xFFE6F2E4), Color(0xFFC2DDBD)),
            listOf(Color(0xFF102012), Color(0xFF1D3320)),
        ),
        BackgroundTheme(
            "sunset", "Sunset Amber", 8,
            listOf(Color(0xFFFFF0DF), Color(0xFFFFD9B0)),
            listOf(Color(0xFF2B160A), Color(0xFF45220D)),
        ),
        BackgroundTheme(
            "lavender", "Lavender Dusk", 10,
            listOf(Color(0xFFF1EAFB), Color(0xFFDCCBF2)),
            listOf(Color(0xFF1E1430), Color(0xFF2F2048)),
        ),
        BackgroundTheme(
            "rose", "Rose Quartz", 12,
            listOf(Color(0xFFFDEDF1), Color(0xFFF6CBD7)),
            listOf(Color(0xFF2B111A), Color(0xFF441B2A)),
        ),
        BackgroundTheme(
            "slate", "Slate Storm", 14,
            listOf(Color(0xFFECEFF1), Color(0xFFCCD6DB)),
            listOf(Color(0xFF14181B), Color(0xFF232B30)),
        ),
        BackgroundTheme(
            "aurora", "Aurora", 16,
            listOf(Color(0xFFE3F7EE), Color(0xFFCBE8F6), Color(0xFFE7DcF7)),
            listOf(Color(0xFF061A18), Color(0xFF0A2A33), Color(0xFF1D1433)),
        ),
        BackgroundTheme(
            "gold", "Gold Leaf", 20,
            listOf(Color(0xFFFDF7E3), Color(0xFFF3E3AC)),
            listOf(Color(0xFF211A06), Color(0xFF39300E)),
        ),
    )

    fun byId(id: String): BackgroundTheme = ALL.firstOrNull { it.id == id } ?: ALL.first()

    fun unlockedAt(level: Int): List<BackgroundTheme> = ALL.filter { it.unlockLevel <= level }

    /** Backgrounds newly unlocked when moving from [oldLevel] to [newLevel]. */
    fun newlyUnlocked(oldLevel: Int, newLevel: Int): List<BackgroundTheme> =
        ALL.filter { it.unlockLevel in (oldLevel + 1)..newLevel }
}
