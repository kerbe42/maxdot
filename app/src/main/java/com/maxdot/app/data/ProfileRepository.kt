package com.maxdot.app.data

import android.content.Context
import android.content.SharedPreferences
import com.maxdot.core.game.AchievementDef
import com.maxdot.core.game.Achievements
import com.maxdot.core.game.Leveling
import com.maxdot.core.game.StatsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppFont(val label: String) {
    SERIF("Serif"),
    SANS("Sans Serif"),
    MONO("Monospace"),
    CURSIVE("Cursive"),
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedTheme: com.maxdot.app.ui.theme.GameThemeId = com.maxdot.app.ui.theme.GameThemeId.PLAYFUL,
    val fontScale: Float = 1.0f,
    val font: AppFont = AppFont.SERIF,
    val difficulty: com.maxdot.core.model.Difficulty = com.maxdot.core.model.Difficulty.MEDIUM,
    val gameMode: com.maxdot.core.model.GameMode = com.maxdot.core.model.GameMode.GUIDED,
    val hapticsEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val dailyGoalXp: Int = 100,
)

data class ProfileState(
    val xp: Int = 0,
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val perfectPassages: Int = 0,
    val passagesCompleted: Int = 0,
    val booksFinished: Int = 0,
    val booksImported: Int = 0,
    val hints: Int = 3,
    val dailyXp: Int = 0,
    val dayStreak: Int = 0,
    val selectedBackground: String = "parchment",
    val unlockedAchievements: Set<String> = emptySet(),
) {
    val level: Int get() = Leveling.levelForXp(xp)
    val accuracy: Int get() = if (totalAnswered == 0) 0 else (totalCorrect * 100) / totalAnswered

    fun toSnapshot() = StatsSnapshot(
        totalCorrect = totalCorrect,
        totalAnswered = totalAnswered,
        bestStreak = bestStreak,
        perfectPassages = perfectPassages,
        passagesCompleted = passagesCompleted,
        booksFinished = booksFinished,
        booksImported = booksImported,
        dayStreak = dayStreak,
        level = level,
    )
}

/** Events produced by recording an answer, for the UI to celebrate. */
data class ProgressEvents(
    val xpGained: Int = 0,
    val leveledUpTo: Int? = null,
    val newAchievements: List<AchievementDef> = emptyList(),
)

/**
 * Single source of truth for player progress and app settings, persisted in
 * SharedPreferences and exposed as [StateFlow]s.
 */
class ProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("maxdot_profile", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<ProfileState> = _profile

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsState> = _settings

    // --- Answers & XP -------------------------------------------------------

    /** Records one answered challenge. Returns celebration events. */
    fun recordAnswer(correct: Boolean): ProgressEvents {
        rollDailyState()
        val before = _profile.value
        val streak = if (correct) before.currentStreak + 1 else 0
        val xpGained = if (correct) Leveling.xpForAnswer(streak) else 0
        var after = before.copy(
            xp = before.xp + xpGained,
            totalCorrect = before.totalCorrect + if (correct) 1 else 0,
            totalAnswered = before.totalAnswered + 1,
            currentStreak = streak,
            bestStreak = maxOf(before.bestStreak, streak),
            dailyXp = before.dailyXp + xpGained,
        )
        val leveledUp = after.level > before.level
        if (leveledUp) after = after.copy(hints = after.hints + (after.level - before.level))
        return commitAndCelebrate(before, after, xpGained, leveledUp)
    }

    /** Records a finished passage. Returns celebration events. */
    fun recordPassageComplete(perfect: Boolean): ProgressEvents {
        val before = _profile.value
        val bonus = if (perfect) Leveling.PERFECT_PASSAGE_BONUS else 0
        var after = before.copy(
            xp = before.xp + bonus,
            dailyXp = before.dailyXp + bonus,
            perfectPassages = before.perfectPassages + if (perfect) 1 else 0,
            passagesCompleted = before.passagesCompleted + 1,
        )
        val leveledUp = after.level > before.level
        if (leveledUp) after = after.copy(hints = after.hints + (after.level - before.level))
        return commitAndCelebrate(before, after, bonus, leveledUp)
    }

    fun recordBookFinished(): ProgressEvents {
        val before = _profile.value
        val after = before.copy(booksFinished = before.booksFinished + 1)
        return commitAndCelebrate(before, after, 0, false)
    }

    fun recordBookImported(): ProgressEvents {
        val before = _profile.value
        val after = before.copy(booksImported = before.booksImported + 1)
        return commitAndCelebrate(before, after, 0, false)
    }

    fun consumeHint(): Boolean {
        val before = _profile.value
        if (before.hints <= 0) return false
        update(before.copy(hints = before.hints - 1))
        return true
    }

    fun selectBackground(id: String) = update(_profile.value.copy(selectedBackground = id))

    fun resetProgress() {
        prefs.edit().clear().apply()
        _profile.value = ProfileState()
        saveSettings(_settings.value) // keep user settings across resets
    }

    private fun commitAndCelebrate(
        before: ProfileState,
        after: ProfileState,
        xpGained: Int,
        leveledUp: Boolean,
    ): ProgressEvents {
        val newAchievements = Achievements.newlyUnlocked(after.toSnapshot(), after.unlockedAchievements)
        val final = after.copy(
            unlockedAchievements = after.unlockedAchievements + newAchievements.map { it.id },
        )
        update(final)
        return ProgressEvents(
            xpGained = xpGained,
            leveledUpTo = if (leveledUp) final.level else null,
            newAchievements = newAchievements,
        )
    }

    /** Resets the daily counter and maintains the day streak when the date changes. */
    private fun rollDailyState() {
        val today = LocalDate.now().toEpochDay()
        val lastPlayed = prefs.getLong("lastPlayedDay", -1L)
        if (lastPlayed == today) return
        val p = _profile.value
        val newDayStreak = if (lastPlayed == today - 1) p.dayStreak + 1 else 1
        prefs.edit().putLong("lastPlayedDay", today).apply()
        update(p.copy(dailyXp = 0, dayStreak = newDayStreak))
    }

    // --- Settings -----------------------------------------------------------

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }
    fun setTheme(id: com.maxdot.app.ui.theme.GameThemeId) = updateSettings { it.copy(selectedTheme = id) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(fontScale = scale.coerceIn(0.8f, 1.6f)) }
    fun setFont(font: AppFont) = updateSettings { it.copy(font = font) }
    fun setDifficulty(d: com.maxdot.core.model.Difficulty) = updateSettings { it.copy(difficulty = d) }
    fun setGameMode(mode: com.maxdot.core.model.GameMode) = updateSettings { it.copy(gameMode = mode) }
    fun setHaptics(enabled: Boolean) = updateSettings { it.copy(hapticsEnabled = enabled) }
    fun setSfx(enabled: Boolean) = updateSettings { it.copy(sfxEnabled = enabled) }
    fun setDailyGoal(xp: Int) = updateSettings { it.copy(dailyGoalXp = xp) }

    // --- Persistence --------------------------------------------------------

    private fun update(state: ProfileState) {
        _profile.value = state
        prefs.edit()
            .putInt("xp", state.xp)
            .putInt("totalCorrect", state.totalCorrect)
            .putInt("totalAnswered", state.totalAnswered)
            .putInt("currentStreak", state.currentStreak)
            .putInt("bestStreak", state.bestStreak)
            .putInt("perfectPassages", state.perfectPassages)
            .putInt("passagesCompleted", state.passagesCompleted)
            .putInt("booksFinished", state.booksFinished)
            .putInt("booksImported", state.booksImported)
            .putInt("hints", state.hints)
            .putInt("dailyXp", state.dailyXp)
            .putInt("dayStreak", state.dayStreak)
            .putString("selectedBackground", state.selectedBackground)
            .putStringSet("achievements", state.unlockedAchievements)
            .apply()
    }

    private fun loadProfile() = ProfileState(
        xp = prefs.getInt("xp", 0),
        totalCorrect = prefs.getInt("totalCorrect", 0),
        totalAnswered = prefs.getInt("totalAnswered", 0),
        currentStreak = prefs.getInt("currentStreak", 0),
        bestStreak = prefs.getInt("bestStreak", 0),
        perfectPassages = prefs.getInt("perfectPassages", 0),
        passagesCompleted = prefs.getInt("passagesCompleted", 0),
        booksFinished = prefs.getInt("booksFinished", 0),
        booksImported = prefs.getInt("booksImported", 0),
        hints = prefs.getInt("hints", 3),
        dailyXp = prefs.getInt("dailyXp", 0),
        dayStreak = prefs.getInt("dayStreak", 0),
        selectedBackground = prefs.getString("selectedBackground", "parchment") ?: "parchment",
        unlockedAchievements = prefs.getStringSet("achievements", emptySet()) ?: emptySet(),
    )

    private fun updateSettings(transform: (SettingsState) -> SettingsState) {
        val next = transform(_settings.value)
        _settings.value = next
        saveSettings(next)
    }

    private fun saveSettings(s: SettingsState) {
        prefs.edit()
            .putString("themeMode", s.themeMode.name)
            .putString("selectedTheme", s.selectedTheme.name)
            .putFloat("fontScale", s.fontScale)
            .putString("font", s.font.name)
            .putString("difficulty", s.difficulty.name)
            .putString("gameMode", s.gameMode.name)
            .putBoolean("haptics", s.hapticsEnabled)
            .putBoolean("sfx", s.sfxEnabled)
            .putInt("dailyGoalXp", s.dailyGoalXp)
            .apply()
    }

    private fun loadSettings() = SettingsState(
        themeMode = enumOrDefault(prefs.getString("themeMode", null), ThemeMode.SYSTEM),
        selectedTheme = enumOrDefault(prefs.getString("selectedTheme", null), com.maxdot.app.ui.theme.GameThemeId.PLAYFUL),
        fontScale = prefs.getFloat("fontScale", 1.0f),
        font = enumOrDefault(prefs.getString("font", null), AppFont.SERIF),
        difficulty = enumOrDefault(prefs.getString("difficulty", null), com.maxdot.core.model.Difficulty.MEDIUM),
        gameMode = enumOrDefault(prefs.getString("gameMode", null), com.maxdot.core.model.GameMode.GUIDED),
        hapticsEnabled = prefs.getBoolean("haptics", true),
        sfxEnabled = prefs.getBoolean("sfx", true),
        dailyGoalXp = prefs.getInt("dailyGoalXp", 100),
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
