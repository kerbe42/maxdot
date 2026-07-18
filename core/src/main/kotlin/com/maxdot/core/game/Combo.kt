package com.maxdot.core.game

/**
 * Display-only combo model derived from the answer streak. The XP economy is
 * unchanged (see [Leveling]); the combo tier is surfaced as juice: a big HUD
 * multiplier that pumps up as the streak climbs and cracks back to x1 on a miss.
 *
 * Tiers by streak (consecutive correct, including the current answer):
 *   0-1 -> x1,  2-3 -> x2,  4-6 -> x3,  7-10 -> x5,  11+ -> x8
 */
object Combo {

    /** (minStreak, multiplier) in ascending order. */
    private val TIERS = listOf(
        0 to 1,
        2 to 2,
        4 to 3,
        7 to 5,
        11 to 8,
    )

    fun multiplier(streak: Int): Int {
        var m = TIERS.first().second
        for ((min, mult) in TIERS) if (streak >= min) m = mult
        return m
    }

    fun label(streak: Int): String = "x${multiplier(streak)}"

    /** Index of the current tier in [TIERS]. */
    private fun tierIndex(streak: Int): Int {
        var idx = 0
        for (i in TIERS.indices) if (streak >= TIERS[i].first) idx = i
        return idx
    }

    /** Progress (0..1) toward the next tier; always 1 at the top tier. */
    fun progress(streak: Int): Float {
        val idx = tierIndex(streak)
        if (idx >= TIERS.lastIndex) return 1f
        val start = TIERS[idx].first
        val next = TIERS[idx + 1].first
        val span = (next - start).coerceAtLeast(1)
        return ((streak - start).toFloat() / span).coerceIn(0f, 1f)
    }

    /** True when moving [fromStreak] -> [toStreak] crosses into a higher tier. */
    fun isTierUp(fromStreak: Int, toStreak: Int): Boolean =
        multiplier(toStreak) > multiplier(fromStreak)
}
