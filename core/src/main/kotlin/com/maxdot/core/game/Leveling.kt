package com.maxdot.core.game

/** XP curve, per-answer scoring, and level math. */
object Leveling {

    const val XP_PER_CORRECT = 10
    const val PERFECT_PASSAGE_BONUS = 25
    const val MAX_LEVEL = 50

    /** XP needed to go from [level] to level + 1. */
    fun xpForNextLevel(level: Int): Int = 75 + 25 * level

    /** Total XP required to reach [level] from zero. */
    fun totalXpForLevel(level: Int): Int {
        var total = 0
        for (l in 1 until level) total += xpForNextLevel(l)
        return total
    }

    fun levelForXp(xp: Int): Int {
        var level = 1
        var remaining = xp
        while (level < MAX_LEVEL && remaining >= xpForNextLevel(level)) {
            remaining -= xpForNextLevel(level)
            level++
        }
        return level
    }

    /** XP earned within the current level, for progress bars. */
    fun xpIntoLevel(xp: Int): Int = xp - totalXpForLevel(levelForXp(xp))

    /**
     * XP for one correct answer including the streak bonus.
     * [streak] is the streak count *including* this answer.
     */
    fun xpForAnswer(streak: Int): Int = XP_PER_CORRECT + 2 * (streak - 1).coerceIn(0, 10)
}
