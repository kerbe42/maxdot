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

/** How the player answers a passage. Independent of [Difficulty]. */
enum class GameMode(val label: String) {
    /** Mistakes highlighted; tap opens a multiple-choice dialog. */
    GUIDED("Guided"),

    /** Mistakes hidden; the player finds each one and types the fix in place. */
    ADVANCED("Advanced"),
}

/**
 * One editable word-token in an Advanced ([GameMode.ADVANCED]) passage. Every
 * token is tappable; a few are planted mistakes. Tokens carry their own leading
 * space so the passage is rebuilt by simple concatenation.
 *
 * @param shownText the (possibly corrupted) text first displayed: word + trailing punctuation.
 * @param correctText the correct text: word + correct trailing punctuation.
 * @param mistakeType non-null when this token is a planted mistake.
 * @param explanation teaching note, present when [mistakeType] is non-null.
 */
data class ProofToken(
    val index: Int,
    val leadingSpace: Boolean,
    val shownText: String,
    val correctText: String,
    val mistakeType: ChallengeType? = null,
    val explanation: String? = null,
) {
    val isMistake: Boolean get() = mistakeType != null
}

/** A passage for Advanced mode: every token is editable; a few are planted mistakes. */
data class ProofPassage(
    val tokens: List<ProofToken>,
    /** Number of planted mistakes per type, for the "N to find" brief. */
    val mistakeCounts: Map<ChallengeType, Int>,
) {
    val mistakeTotal: Int get() = tokens.count { it.isMistake }
}

/** Per-token result after an Advanced passage is checked. */
enum class TokenOutcome {
    /** A planted mistake the player fixed correctly. */
    CAUGHT,

    /** A planted mistake the player left unfixed (or fixed wrong). */
    MISSED,

    /** A correct word the player changed to something wrong. */
    FALSE_FLAG,

    /** A correct word the player (correctly) left alone or edited back to correct. */
    UNTOUCHED_CORRECT,
}

/** Outcome of checking an Advanced passage. */
data class ProofResult(
    val outcomes: Map<Int, TokenOutcome>,
    val caught: Int,
    val missed: Int,
    val falseFlags: Int,
    val totalMistakes: Int,
) {
    val perfect: Boolean get() = caught == totalMistakes && falseFlags == 0
}

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
