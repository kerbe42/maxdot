package com.maxdot.core.model

/** The kind of mistake embedded in a passage. */
enum class ChallengeType {
    /** A punctuation mark was removed (or a decoy empty slot was inserted). */
    PUNCTUATION,

    /** A word was replaced with a commonly-confused wrong word (their/there, was/were...). */
    GRAMMAR,

    /** A contraction's apostrophe was removed or misplaced (dont / do'nt). */
    APOSTROPHE,

    /** A sentence-start capital letter was lowercased. */
    CAPITALIZATION,
}

/**
 * One tappable mistake inside a passage.
 *
 * @param shownText what the passage displays before the player answers (the wrong word,
 *   or [PUNCTUATION_SLOT] for a punctuation gap).
 * @param options the multiple-choice answers, already shuffled. Punctuation challenges may
 *   contain [NO_PUNCTUATION] meaning "nothing goes here".
 * @param correctIndex index into [options] of the right answer.
 * @param resolvedText what the passage should display once answered (the correct text;
 *   empty string when the right answer is "no punctuation").
 * @param explanation a short teaching note shown after the player answers.
 */
data class Challenge(
    val id: Int,
    val type: ChallengeType,
    val shownText: String,
    val options: List<String>,
    val correctIndex: Int,
    val resolvedText: String,
    val explanation: String,
) {
    val correctAnswer: String get() = options[correctIndex]

    companion object {
        const val NO_PUNCTUATION = "nothing"
        const val PUNCTUATION_SLOT = "[?]"
    }
}

/**
 * A piece of passage text. Segments with a non-null [challengeId] are tappable.
 * Segments carry their own leading/trailing spacing so the passage is rebuilt by
 * simple concatenation.
 */
data class PassageSegment(
    val text: String,
    val challengeId: Int? = null,
)

/** A chunk of a book, rendered as tappable rich text. */
data class Passage(
    val segments: List<PassageSegment>,
    val challenges: List<Challenge>,
) {
    fun challenge(id: Int): Challenge = challenges.first { it.id == id }
}

/** Result of parsing an imported book file. */
data class ParsedBook(
    val title: String?,
    val author: String?,
    val text: String,
)

enum class Difficulty(
    val label: String,
    /** How many mistakes to embed per passage (upper bound). */
    val maxChallenges: Int,
    /** Number of answer options to aim for. */
    val optionCount: Int,
    /** Whether to insert decoy punctuation slots whose answer is "nothing". */
    val decoySlots: Boolean,
    /** Target passage length in words. */
    val targetWords: Int,
) {
    EASY("Easy", 3, 3, false, 35),
    MEDIUM("Medium", 5, 4, false, 50),
    HARD("Hard", 6, 5, true, 65),
}
