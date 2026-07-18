package com.maxdot.core

import com.maxdot.core.game.Combo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComboTest {

    @Test
    fun lowStreaksAreMultiplierOne() {
        assertEquals(1, Combo.multiplier(0))
        assertEquals(1, Combo.multiplier(1))
    }

    @Test
    fun tiersRampUp() {
        assertEquals(2, Combo.multiplier(2))
        assertEquals(2, Combo.multiplier(3))
        assertEquals(3, Combo.multiplier(4))
        assertEquals(3, Combo.multiplier(6))
        assertEquals(5, Combo.multiplier(7))
        assertEquals(5, Combo.multiplier(10))
        assertEquals(8, Combo.multiplier(11))
        assertEquals(8, Combo.multiplier(50))
    }

    @Test
    fun labelHasTimesPrefix() {
        assertEquals("x1", Combo.label(0))
        assertEquals("x3", Combo.label(5))
        assertEquals("x8", Combo.label(20))
    }

    @Test
    fun progressIsZeroToOneAndFullAtMaxTier() {
        // streak 2 is the start of the x2 tier (2..3) -> low progress
        assertTrue(Combo.progress(2) in 0f..1f)
        // just before the next tier boundary -> high progress
        assertTrue(Combo.progress(3) > Combo.progress(2))
        // max tier is always full
        assertEquals(1f, Combo.progress(11))
        assertEquals(1f, Combo.progress(99))
    }

    @Test
    fun tierIncreasesAtBoundaries() {
        assertTrue(Combo.isTierUp(fromStreak = 1, toStreak = 2))   // x1 -> x2
        assertTrue(Combo.isTierUp(fromStreak = 3, toStreak = 4))   // x2 -> x3
        assertTrue(!Combo.isTierUp(fromStreak = 2, toStreak = 3))  // still x2
    }
}
