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
import com.maxdot.core.model.Challenge
import com.maxdot.core.model.Difficulty
import com.maxdot.core.model.Passage
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

data class GameUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val passage: Passage? = null,
    val answers: Map<Int, AnswerState> = emptyMap(),
    /** Challenge currently opened in the answer dialog. */
    val openChallenge: Challenge? = null,
    /** Option indices removed by a hint for the open challenge. */
    val eliminatedOptions: Set<Int> = emptySet(),
    val sessionCorrect: Int = 0,
    val sessionAnswered: Int = 0,
    val passageComplete: Boolean = false,
    val passageXp: Int = 0,
    val passagePerfect: Boolean = false,
    val bookFinished: Boolean = false,
    val progress: BookProgress = BookProgress(),
) {
    val allAnswered: Boolean
        get() = passage != null && passage.challenges.isNotEmpty() &&
            passage.challenges.all { answers.containsKey(it.id) }
}

/** One-shot celebration events surfaced as snackbars. */
data class Celebration(
    val message: String,
    val achievements: List<AchievementDef> = emptyList(),
)

class GameViewModel(
    private val book: Book,
    private val bookRepo: BookRepository,
    private val profileRepo: ProfileRepository,
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
        get() = profileRepo.settings.value.difficulty

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
            passage = passage,
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
                Celebration("Level up! You reached level $it 🎉 (+1 hint)")
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
