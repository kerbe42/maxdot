package com.maxdot.app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameThemesTest {

    @Test
    fun everyIdResolves() {
        GameThemeId.entries.forEach { id -> assertNotNull(GameThemes.resolve(id)) }
    }

    @Test
    fun idsAreConsistent() {
        GameThemeId.entries.forEach { id -> assertEquals(id, GameThemes.resolve(id).id) }
    }

    @Test
    fun themesAreDistinct() {
        assertEquals(3, GameThemes.ALL.size)
        assertEquals(3, GameThemes.ALL.map { it.colors.primary }.toSet().size)
    }

    @Test
    fun neonAndRetroAreDark() {
        assertTrue(GameThemes.NEON.colors.dark)
        assertTrue(GameThemes.RETRO.colors.dark)
        assertFalse(GameThemes.PLAYFUL.colors.dark)
    }

    @Test
    fun colorSchemeBridgesPrimary() {
        val scheme = GameThemes.PLAYFUL.toColorScheme()
        assertEquals(GameThemes.PLAYFUL.colors.primary, scheme.primary)
    }
}
