package com.maxdot.core

import com.maxdot.core.game.Achievements
import com.maxdot.core.game.ChallengeGenerator
import com.maxdot.core.game.Leveling
import com.maxdot.core.game.PassageBuilder
import com.maxdot.core.game.StatsSnapshot
import com.maxdot.core.model.Challenge
import com.maxdot.core.model.ChallengeType
import com.maxdot.core.model.Difficulty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChallengeGeneratorTest {

    private val sentences = listOf(
        "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
        "However little known the feelings or views of such a man may be, the neighbourhood considers him their rightful property.",
        "\"My dear Mr. Bennet,\" said his lady to him one day, \"have you heard that Netherfield Park is let at last?\"",
        "Mr. Bennet replied that he had not, and that was an end of it.",
        "But it's a fine thing for our girls, and I don't intend to stop there.",
    )

    private fun reassemble(passage: com.maxdot.core.model.Passage): String =
        passage.segments.joinToString("") { seg ->
            val ch = seg.challengeId?.let { passage.challenge(it) }
            if (ch != null) ch.resolvedText else seg.text
        }

    @Test
    fun `generates challenges within difficulty bounds`() {
        for (difficulty in Difficulty.entries) {
            repeat(20) { seed ->
                val passage = ChallengeGenerator(Random(seed.toLong())).generate(sentences, difficulty)
                assertTrue(passage.challenges.isNotEmpty(), "no challenges for $difficulty seed $seed")
                assertTrue(passage.challenges.size <= difficulty.maxChallenges)
            }
        }
    }

    @Test
    fun `resolving every challenge reproduces the original text`() {
        val original = sentences.joinToString(" ")
        repeat(50) { seed ->
            for (difficulty in Difficulty.entries) {
                val passage = ChallengeGenerator(Random(seed.toLong())).generate(sentences, difficulty)
                assertEquals(original, reassemble(passage), "seed $seed difficulty $difficulty")
            }
        }
    }

    @Test
    fun `correct option matches resolved text for word challenges`() {
        repeat(30) { seed ->
            val passage = ChallengeGenerator(Random(seed.toLong())).generate(sentences, Difficulty.HARD)
            for (ch in passage.challenges) {
                assertTrue(ch.correctIndex in ch.options.indices, "correctIndex out of range")
                if (ch.type != ChallengeType.PUNCTUATION) {
                    assertEquals(ch.resolvedText, ch.correctAnswer)
                    assertTrue(ch.shownText != ch.correctAnswer, "shown text must be the wrong form")
                }
                assertEquals(ch.options.size, ch.options.distinct().size, "duplicate options")
            }
        }
    }

    @Test
    fun `punctuation challenge resolved text matches correct option`() {
        repeat(30) { seed ->
            val passage = ChallengeGenerator(Random(seed.toLong())).generate(sentences, Difficulty.MEDIUM)
            for (ch in passage.challenges.filter { it.type == ChallengeType.PUNCTUATION }) {
                if (ch.correctAnswer == Challenge.NO_PUNCTUATION) {
                    assertEquals("", ch.resolvedText)
                } else {
                    assertEquals(ch.correctAnswer, ch.resolvedText)
                }
                assertEquals(Challenge.PUNCTUATION_SLOT, ch.shownText)
            }
        }
    }

    @Test
    fun `grammar challenges appear on medium and above`() {
        var sawGrammar = false
        repeat(30) { seed ->
            val passage = ChallengeGenerator(Random(seed.toLong())).generate(sentences, Difficulty.MEDIUM)
            if (passage.challenges.any { it.type == ChallengeType.GRAMMAR || it.type == ChallengeType.APOSTROPHE }) {
                sawGrammar = true
            }
        }
        assertTrue(sawGrammar, "expected grammar/apostrophe challenges on medium difficulty")
    }
}

class LevelingTest {

    @Test
    fun `level math is consistent`() {
        assertEquals(1, Leveling.levelForXp(0))
        assertEquals(1, Leveling.levelForXp(99))
        assertEquals(2, Leveling.levelForXp(100))
        for (level in 1..49) {
            val threshold = Leveling.totalXpForLevel(level + 1)
            assertEquals(level + 1, Leveling.levelForXp(threshold))
            assertEquals(level, Leveling.levelForXp(threshold - 1))
        }
    }

    @Test
    fun `streak bonus grows then caps`() {
        assertEquals(10, Leveling.xpForAnswer(1))
        assertEquals(12, Leveling.xpForAnswer(2))
        assertEquals(30, Leveling.xpForAnswer(11))
        assertEquals(30, Leveling.xpForAnswer(50))
    }
}

class PassageBuilderTest {

    @Test
    fun `chunks sentences to target length and advances position`() {
        val sentences = List(20) { i -> "Sentence number $i has exactly seven words here." }
        var pos = 0
        val seen = mutableListOf<String>()
        while (pos < sentences.size) {
            val (chunk, next) = PassageBuilder.next(sentences, pos, Difficulty.MEDIUM)
            assertTrue(chunk.isNotEmpty())
            assertTrue(next > pos)
            seen.addAll(chunk)
            pos = next
        }
        assertEquals(sentences, seen)
    }

    @Test
    fun `returns empty at end`() {
        val (chunk, next) = PassageBuilder.next(listOf("One."), 5, Difficulty.EASY)
        assertTrue(chunk.isEmpty())
        assertEquals(5, next)
    }
}

class AchievementsTest {

    @Test
    fun `unlocks match stats and are not re-reported`() {
        val stats = StatsSnapshot(totalCorrect = 100, totalAnswered = 105, bestStreak = 12)
        val newly = Achievements.newlyUnlocked(stats, alreadyUnlocked = setOf("first_correct"))
        val ids = newly.map { it.id }
        assertTrue("correct_100" in ids)
        assertTrue("streak_10" in ids)
        assertTrue("sharpshooter" in ids)
        assertTrue("first_correct" !in ids)
        assertTrue("book_finished" !in ids)
    }
}
