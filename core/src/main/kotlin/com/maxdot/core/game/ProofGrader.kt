package com.maxdot.core.game

import com.maxdot.core.model.ProofPassage
import com.maxdot.core.model.ProofResult
import com.maxdot.core.model.TokenOutcome

/**
 * Grades an Advanced-mode passage. Given the player's edits (token index -> the
 * text they typed), classifies every token as caught, missed, a false flag, or
 * an untouched-correct word, and tallies the score.
 *
 * Comparison is whitespace-lenient (leading/trailing and repeated spaces are
 * ignored, and a space directly before a punctuation mark is dropped) but
 * case-sensitive, because fixing capitalization is itself a mistake type.
 */
object ProofGrader {

    fun grade(passage: ProofPassage, edits: Map<Int, String>): ProofResult {
        val outcomes = HashMap<Int, TokenOutcome>(passage.tokens.size)
        var caught = 0
        var missed = 0
        var falseFlags = 0

        for (token in passage.tokens) {
            val finalText = edits[token.index] ?: token.shownText
            val matchesCorrect = normalize(finalText) == normalize(token.correctText)
            val outcome = when {
                token.isMistake && matchesCorrect -> TokenOutcome.CAUGHT
                token.isMistake -> TokenOutcome.MISSED
                !matchesCorrect -> TokenOutcome.FALSE_FLAG
                else -> TokenOutcome.UNTOUCHED_CORRECT
            }
            when (outcome) {
                TokenOutcome.CAUGHT -> caught++
                TokenOutcome.MISSED -> missed++
                TokenOutcome.FALSE_FLAG -> falseFlags++
                TokenOutcome.UNTOUCHED_CORRECT -> Unit
            }
            outcomes[token.index] = outcome
        }

        return ProofResult(
            outcomes = outcomes,
            caught = caught,
            missed = missed,
            falseFlags = falseFlags,
            totalMistakes = passage.mistakeTotal,
        )
    }

    private val SPACE_BEFORE_PUNCT = Regex("""\s+([,.;:!?])""")
    private val WHITESPACE = Regex("""\s+""")

    private fun normalize(text: String): String =
        text.trim()
            .replace(WHITESPACE, " ")
            .replace(SPACE_BEFORE_PUNCT, "$1")
}
