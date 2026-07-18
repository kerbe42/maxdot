package com.maxdot.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxdot.app.data.Book
import com.maxdot.app.data.BookProgress
import com.maxdot.app.data.BookRepository
import com.maxdot.app.data.ProfileRepository
import com.maxdot.core.game.AchievementDef
import com.maxdot.core.game.ChallengeGenerator
import com.maxdot.core.game.PassageBuilder
import com.maxdot.core.game.ProofGrader
import com.maxdot.core.model.Challenge
import com.maxdot.core.model.Difficulty
import com.maxdot.core.model.GameMode
import com.maxdot.core.model.Passage
import com.maxdot.core.model.ProofPassage
import com.maxdot.core.model.ProofResult
import com.maxdot.core.model.TokenOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Per-challenge answer state within the current passage. */
data class AnswerState(
    val selectedIndex: Int,
    val correct: Boolean,
)

/** Advanced-mode play state for the current passage. */
data class ProofUiState(
    val passage: ProofPassage,
    /** Player edits keyed by token index; absent means the token is unchanged. */
    val edits: Map<Int, String> = emptyMap(),
    /** Token whose editor dialog is open, if any. */
    val openTokenIndex: Int? = null,
    /** Grading result once the passage is checked; null while still editable. */
    val result: ProofResult? = null,
    /** Mistake token indices whose location a hint has revealed. */
    val revealedHints: Set<Int> = emptySet(),
) {
    val checked: Boolean get() = result != null
}

data class GameUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val mode: GameMode = GameMode.GUIDED,
    val passage: Passage? = null,
    val answers: Map<Int, AnswerState> = emptyMap(),
    /** Challenge currently opened in the answer dialog. */
    val openChallenge: Challenge? = null,
    /** Option indices removed by a hint for the open challenge. */
    val eliminatedOptions: Set<Int> = emptySet(),
    /** Advanced-mode state; non-null only when [mode] is ADVANCED. */
    val proof: ProofUiState? = null,
    val sessionCorrect: Int = 0,
    val sessionAnswered: Int = 0,
    val passageComplete: Boolean = false,
    val passageXp: Int = 0,
    val passagePerfect: Boolean = false,
    val bookFinished: Boolean = false,
    val isBoss: Boolean = false,
    val progress: BookProgress = BookProgress(),
) {
    val allAnswered: Boolean
        get() = passage != null && passage.challenges.isNotEmpty() &&
            passage.challenges.all { answers.containsKey(it.id) }
}

/** One-shot celebration events surfaced as snackbars or a full-screen moment. */
data class Celebration(
    val message: String,
    val achievements: List<AchievementDef> = emptyList(),
    /** Non-null when this celebration is a level-up (the new level), for a big overlay. */
    val levelUp: Int? = null,
)

class GameViewModel(
    private val book: Book,
    private val bookRepo: BookRepository,
    private val profileRepo: ProfileRepository,
    /** When true this is a boss node: harder passage sizing and boss framing. */
    private val boss: Boolean = false,
) : ViewModel() {

    private val generator = ChallengeGenerator()

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations

    private var sentences: List<String> = emptyList()
    private var position = 0
    private var progress = BookProgress()

    private val difficulty: Difficulty
        get() = if (boss) Difficulty.HARD else profileRepo.settings.value.difficulty

    private val gameMode: GameMode
        get() = profileRepo.settings.value.gameMode

    init {
        viewModelScope.launch {
            try {
                sentences = withContext(Dispatchers.IO) { bookRepo.sentences(book) }
                progress = bookRepo.progress(book.id).let {
                    if (it.sentenceCount != sentences.size || it.position >= sentences.size) {
                        it.copy(position = it.position.coerceIn(0, maxOf(0, sentences.size - 1)), sentenceCount = sentences.size)
                    } else {
                        it
                    }
                }
                position = progress.position
                nextPassage()
            } catch (e: Exception) {
                _state.value = GameUiState(loading = false, error = e.message ?: "Could not load book")
            }
        }
    }

    fun nextPassage() {
        if (gameMode == GameMode.ADVANCED) nextProofPassage() else nextGuidedPassage()
    }

    private fun nextGuidedPassage() {
        val (chunk, nextPos) = PassageBuilder.next(sentences, position, difficulty)
        if (chunk.isEmpty()) {
            finishBook()
            return
        }
        var passage = generator.generate(chunk, difficulty)
        var advanceTo = nextPos
        // Extremely short/odd chunks can yield no challenges; skip forward until one does.
        while (passage.challenges.isEmpty() && advanceTo < sentences.size) {
            val (moreChunk, morePos) = PassageBuilder.next(sentences, advanceTo, difficulty)
            if (moreChunk.isEmpty()) break
            passage = generator.generate(moreChunk, difficulty)
            advanceTo = morePos
        }
        if (passage.challenges.isEmpty()) {
            finishBook()
            return
        }
        position = advanceTo
        _state.value = GameUiState(
            loading = false,
            mode = GameMode.GUIDED,
            passage = passage,
            isBoss = boss,
            progress = progress,
            sessionCorrect = _state.value.sessionCorrect,
            sessionAnswered = _state.value.sessionAnswered,
        )
    }

    private fun nextProofPassage() {
        val (chunk, nextPos) = PassageBuilder.next(sentences, position, difficulty)
        if (chunk.isEmpty()) {
            finishBook()
            return
        }
        var passage = generator.generateAdvanced(chunk, difficulty)
        var advanceTo = nextPos
        while (passage.mistakeTotal == 0 && advanceTo < sentences.size) {
            val (moreChunk, morePos) = PassageBuilder.next(sentences, advanceTo, difficulty)
            if (moreChunk.isEmpty()) break
            passage = generator.generateAdvanced(moreChunk, difficulty)
            advanceTo = morePos
        }
        if (passage.mistakeTotal == 0) {
            finishBook()
            return
        }
        position = advanceTo
        _state.value = GameUiState(
            loading = false,
            mode = GameMode.ADVANCED,
            proof = ProofUiState(passage = passage),
            isBoss = boss,
            progress = progress,
            sessionCorrect = _state.value.sessionCorrect,
            sessionAnswered = _state.value.sessionAnswered,
        )
    }

    private fun finishBook() {
        val events = profileRepo.recordBookFinished()
        progress = progress.copy(position = 0, timesFinished = progress.timesFinished + 1)
        position = 0
        bookRepo.saveProgress(book.id, progress)
        celebrate(events.newAchievements)
        _state.value = _state.value.copy(loading = false, bookFinished = true, progress = progress)
    }

    fun openChallenge(id: Int) {
        val passage = _state.value.passage ?: return
        if (_state.value.answers.containsKey(id)) return
        _state.value = _state.value.copy(
            openChallenge = passage.challenge(id),
            eliminatedOptions = emptySet(),
        )
    }

    fun dismissChallenge() {
        _state.value = _state.value.copy(openChallenge = null, eliminatedOptions = emptySet())
    }

    /** Spends a hint to eliminate up to two wrong options in the open dialog. */
    fun useHint() {
        val st = _state.value
        val challenge = st.openChallenge ?: return
        val wrong = challenge.options.indices
            .filter { it != challenge.correctIndex && it !in st.eliminatedOptions }
        if (wrong.size < 2) return // nothing meaningful to eliminate
        if (!profileRepo.consumeHint()) return
        _state.value = st.copy(eliminatedOptions = st.eliminatedOptions + wrong.shuffled().take(2))
    }

    fun answer(optionIndex: Int) {
        val st = _state.value
        val challenge = st.openChallenge ?: return
        if (st.answers.containsKey(challenge.id)) return

        val correct = optionIndex == challenge.correctIndex
        val events = profileRepo.recordAnswer(correct)
        progress = progress.copy(
            correct = progress.correct + if (correct) 1 else 0,
            answered = progress.answered + 1,
        )

        var newState = st.copy(
            answers = st.answers + (challenge.id to AnswerState(optionIndex, correct)),
            sessionCorrect = st.sessionCorrect + if (correct) 1 else 0,
            sessionAnswered = st.sessionAnswered + 1,
            passageXp = st.passageXp + events.xpGained,
        )
        celebrateEvents(events)

        if (newState.passage != null &&
            newState.passage!!.challenges.all { newState.answers.containsKey(it.id) }
        ) {
            val perfect = newState.passage!!.challenges.all { newState.answers[it.id]?.correct == true }
            val completion = profileRepo.recordPassageComplete(perfect)
            progress = progress.copy(position = position, sentenceCount = sentences.size)
            bookRepo.saveProgress(book.id, progress)
            celebrateEvents(completion)
            newState = newState.copy(
                passageComplete = true,
                passagePerfect = perfect,
                passageXp = newState.passageXp + completion.xpGained,
                progress = progress,
            )
        }
        _state.value = newState
    }

    // --- Advanced (proofreading) mode --------------------------------------

    fun openToken(index: Int) {
        val proof = _state.value.proof ?: return
        if (proof.checked) return
        _state.value = _state.value.copy(proof = proof.copy(openTokenIndex = index))
    }

    fun dismissToken() {
        val proof = _state.value.proof ?: return
        _state.value = _state.value.copy(proof = proof.copy(openTokenIndex = null))
    }

    /** Records the player's edit of a token, closing the editor. */
    fun editToken(index: Int, text: String) {
        val proof = _state.value.proof ?: return
        if (proof.checked) return
        val token = proof.passage.tokens.getOrNull(index) ?: return
        val trimmed = text.trim()
        // Reverting to the shown text (or clearing it) means "no change".
        val edits = if (trimmed.isEmpty() || trimmed == token.shownText) {
            proof.edits - index
        } else {
            proof.edits + (index to trimmed)
        }
        _state.value = _state.value.copy(
            proof = proof.copy(edits = edits, openTokenIndex = null),
        )
    }

    /** Reveals the location of one still-uncaught mistake, spending a hint. */
    fun useProofHint() {
        val proof = _state.value.proof ?: return
        if (proof.checked) return
        val preview = ProofGrader.grade(proof.passage, proof.edits)
        val target = proof.passage.tokens.firstOrNull {
            it.isMistake && it.index !in proof.revealedHints &&
                preview.outcomes[it.index] == TokenOutcome.MISSED
        } ?: return
        if (!profileRepo.consumeHint()) return
        _state.value = _state.value.copy(
            proof = proof.copy(revealedHints = proof.revealedHints + target.index),
        )
    }

    /** Grades the current Advanced passage and commits XP/progress. */
    fun checkPassage() {
        val st = _state.value
        val proof = st.proof ?: return
        if (proof.checked) return

        val result = ProofGrader.grade(proof.passage, proof.edits)

        // One recorded answer per planted mistake (caught => correct), so streaks,
        // levels, and achievements advance exactly as in guided mode.
        var xp = 0
        for (token in proof.passage.tokens) {
            if (!token.isMistake) continue
            val caught = result.outcomes[token.index] == TokenOutcome.CAUGHT
            val events = profileRepo.recordAnswer(caught)
            xp += events.xpGained
            celebrateEvents(events)
        }
        val completion = profileRepo.recordPassageComplete(result.perfect)
        xp += completion.xpGained
        celebrateEvents(completion)

        progress = progress.copy(
            correct = progress.correct + result.caught,
            answered = progress.answered + result.totalMistakes,
            position = position,
            sentenceCount = sentences.size,
        )
        bookRepo.saveProgress(book.id, progress)

        _state.value = _state.value.copy(
            proof = proof.copy(result = result),
            passageComplete = true,
            passagePerfect = result.perfect,
            passageXp = xp,
            sessionCorrect = st.sessionCorrect + result.caught,
            sessionAnswered = st.sessionAnswered + result.totalMistakes,
            progress = progress,
        )
    }

    fun restartBook() {
        progress = progress.copy(position = 0)
        position = 0
        bookRepo.saveProgress(book.id, progress)
        _state.value = GameUiState(loading = false)
        nextPassage()
    }

    fun consumeCelebration() {
        _celebrations.value = _celebrations.value.drop(1)
    }

    private fun celebrateEvents(events: com.maxdot.app.data.ProgressEvents) {
        events.leveledUpTo?.let {
            _celebrations.value = _celebrations.value +
                Celebration("Level up! You reached level $it 🎉 (+1 hint)", levelUp = it)
        }
        celebrate(events.newAchievements)
    }

    private fun celebrate(achievements: List<AchievementDef>) {
        if (achievements.isEmpty()) return
        _celebrations.value = _celebrations.value + achievements.map {
            Celebration("Achievement unlocked: ${it.emoji} ${it.title}")
        }
    }
}
