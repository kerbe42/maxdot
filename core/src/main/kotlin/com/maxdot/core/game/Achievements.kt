package com.maxdot.core.game

/** Snapshot of lifetime stats used to evaluate achievements. */
data class StatsSnapshot(
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val bestStreak: Int = 0,
    val perfectPassages: Int = 0,
    val passagesCompleted: Int = 0,
    val booksFinished: Int = 0,
    val booksImported: Int = 0,
    val dayStreak: Int = 0,
    val level: Int = 1,
) {
    val accuracy: Int
        get() = if (totalAnswered == 0) 0 else (totalCorrect * 100) / totalAnswered
}

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: (StatsSnapshot) -> Boolean,
)

object Achievements {

    val ALL: List<AchievementDef> = listOf(
        AchievementDef("first_correct", "First Steps", "Answer your first challenge correctly", "🐣") {
            it.totalCorrect >= 1
        },
        AchievementDef("perfect_passage", "Flawless", "Complete a passage with no mistakes", "✨") {
            it.perfectPassages >= 1
        },
        AchievementDef("correct_100", "Centurion", "Get 100 correct answers", "💯") {
            it.totalCorrect >= 100
        },
        AchievementDef("correct_500", "Wordsmith", "Get 500 correct answers", "🖋️") {
            it.totalCorrect >= 500
        },
        AchievementDef("correct_2000", "Grammarian", "Get 2,000 correct answers", "🏛️") {
            it.totalCorrect >= 2000
        },
        AchievementDef("streak_10", "On a Roll", "Reach a 10-answer streak", "🔥") {
            it.bestStreak >= 10
        },
        AchievementDef("streak_25", "Unstoppable", "Reach a 25-answer streak", "⚡") {
            it.bestStreak >= 25
        },
        AchievementDef("sharpshooter", "Sharpshooter", "Reach 90% accuracy over 50+ answers", "🎯") {
            it.totalAnswered >= 50 && it.accuracy >= 90
        },
        AchievementDef("perfect_10", "Perfectionist", "Complete 10 flawless passages", "🌟") {
            it.perfectPassages >= 10
        },
        AchievementDef("book_finished", "Bookworm", "Finish an entire book", "📚") {
            it.booksFinished >= 1
        },
        AchievementDef("book_imported", "Collector", "Import your own book", "📥") {
            it.booksImported >= 1
        },
        AchievementDef("day_streak_7", "Dedicated", "Play 7 days in a row", "📅") {
            it.dayStreak >= 7
        },
        AchievementDef("level_10", "Scholar", "Reach level 10", "🎓") {
            it.level >= 10
        },
        AchievementDef("level_25", "Professor", "Reach level 25", "🦉") {
            it.level >= 25
        },
    )

    /** Returns achievements newly earned by [stats] that are not in [alreadyUnlocked]. */
    fun newlyUnlocked(stats: StatsSnapshot, alreadyUnlocked: Set<String>): List<AchievementDef> =
        ALL.filter { it.id !in alreadyUnlocked && it.isUnlocked(stats) }
}
