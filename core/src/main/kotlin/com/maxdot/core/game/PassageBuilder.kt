package com.maxdot.core.game

import com.maxdot.core.model.Difficulty

/**
 * Walks a book's sentence list, handing out consecutive sentence runs sized to the
 * difficulty's target word count.
 */
object PassageBuilder {

    const val MAX_SENTENCES = 5

    /**
     * Returns the sentences for the passage starting at [position], plus the next
     * position. Never returns an empty list unless [position] is past the end.
     */
    fun next(sentences: List<String>, position: Int, difficulty: Difficulty): Pair<List<String>, Int> {
        if (position >= sentences.size) return emptyList<String>() to position
        val chunk = mutableListOf<String>()
        var words = 0
        var pos = position
        while (pos < sentences.size && chunk.size < MAX_SENTENCES) {
            val sentence = sentences[pos]
            chunk.add(sentence)
            pos++
            words += sentence.count { it == ' ' } + 1
            if (words >= difficulty.targetWords) break
        }
        return chunk to pos
    }
}
