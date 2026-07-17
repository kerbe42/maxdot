package com.maxdot.core.game

import com.maxdot.core.model.Challenge
import com.maxdot.core.model.ChallengeType
import com.maxdot.core.model.Difficulty
import com.maxdot.core.model.Passage
import com.maxdot.core.model.PassageSegment
import kotlin.random.Random

/**
 * Turns a run of correct book sentences into a [Passage] with embedded mistakes:
 * removed punctuation, confused homophones, broken subject–verb agreement,
 * mangled apostrophes, and lowercased sentence starts.
 */
class ChallengeGenerator(private val random: Random = Random.Default) {

    private companion object {
        const val PUNCT_CHARS = ",.;:!?"
        val PUNCT_POOL = listOf(",", ".", ";", "!", "?", ":")

        val PUNCT_EXPLANATIONS = mapOf(
            "," to "A comma marks a pause between parts of the sentence.",
            "." to "A complete sentence ends here, so it needs a period.",
            ";" to "A semicolon joins two closely related clauses.",
            "!" to "An exclamation mark shows strong feeling here.",
            "?" to "This is a question, so it ends with a question mark.",
            ":" to "A colon introduces what follows.",
            Challenge.NO_PUNCTUATION to "The sentence flows on — no punctuation is needed here.",
        )
    }

    /** A word token plus any trailing punctuation, e.g. "said," -> word="said", punct=",". */
    private data class Token(val word: String, val punct: String, val sentenceStart: Boolean)

    private sealed class Candidate {
        abstract val tokenIndex: Int

        data class Punctuation(override val tokenIndex: Int, val mark: String) : Candidate()
        data class Grammar(override val tokenIndex: Int, val entry: ConfusionSets.Entry) : Candidate()
        data class Agreement(override val tokenIndex: Int, val wrongForm: String) : Candidate()
        data class Apostrophe(override val tokenIndex: Int) : Candidate()
        data class Capitalization(override val tokenIndex: Int) : Candidate()

        /** Decoy: an empty slot between tokenIndex and the next token; answer is "nothing". */
        data class Decoy(override val tokenIndex: Int) : Candidate()
    }

    fun generate(sentences: List<String>, difficulty: Difficulty): Passage {
        val tokens = tokenize(sentences)
        val candidates = findCandidates(tokens, difficulty)
        val chosen = selectCandidates(candidates, difficulty)

        val challenges = mutableListOf<Challenge>()
        val segments = mutableListOf<PassageSegment>()
        val byIndex = chosen.associateBy { it.tokenIndex }

        for ((i, token) in tokens.withIndex()) {
            val leadingSpace = if (i == 0) "" else " "
            when (val cand = byIndex[i]) {
                null -> appendPlain(segments, leadingSpace + token.word + token.punct)

                is Candidate.Punctuation -> {
                    appendPlain(segments, leadingSpace + token.word)
                    val challenge = punctuationChallenge(challenges.size, cand.mark, difficulty)
                    challenges.add(challenge)
                    segments.add(PassageSegment(Challenge.PUNCTUATION_SLOT, challenge.id))
                    val rest = token.punct.removePrefix(cand.mark)
                    if (rest.isNotEmpty()) appendPlain(segments, rest)
                }

                is Candidate.Decoy -> {
                    appendPlain(segments, leadingSpace + token.word + token.punct)
                    val challenge = decoyChallenge(challenges.size, difficulty)
                    challenges.add(challenge)
                    segments.add(PassageSegment(Challenge.PUNCTUATION_SLOT, challenge.id))
                }

                is Candidate.Grammar -> {
                    val challenge = grammarChallenge(challenges.size, token.word, cand.entry, difficulty)
                    challenges.add(challenge)
                    if (leadingSpace.isNotEmpty()) appendPlain(segments, leadingSpace)
                    segments.add(PassageSegment(challenge.shownText, challenge.id))
                    if (token.punct.isNotEmpty()) appendPlain(segments, token.punct)
                }

                is Candidate.Agreement -> {
                    val challenge = agreementChallenge(challenges.size, token.word, cand.wrongForm)
                    challenges.add(challenge)
                    if (leadingSpace.isNotEmpty()) appendPlain(segments, leadingSpace)
                    segments.add(PassageSegment(challenge.shownText, challenge.id))
                    if (token.punct.isNotEmpty()) appendPlain(segments, token.punct)
                }

                is Candidate.Apostrophe -> {
                    val challenge = apostropheChallenge(challenges.size, token.word)
                    challenges.add(challenge)
                    if (leadingSpace.isNotEmpty()) appendPlain(segments, leadingSpace)
                    segments.add(PassageSegment(challenge.shownText, challenge.id))
                    if (token.punct.isNotEmpty()) appendPlain(segments, token.punct)
                }

                is Candidate.Capitalization -> {
                    val challenge = capitalizationChallenge(challenges.size, token.word)
                    challenges.add(challenge)
                    if (leadingSpace.isNotEmpty()) appendPlain(segments, leadingSpace)
                    segments.add(PassageSegment(challenge.shownText, challenge.id))
                    if (token.punct.isNotEmpty()) appendPlain(segments, token.punct)
                }
            }
        }

        return Passage(segments, challenges)
    }

    /** Merge adjacent plain segments so the segment list stays small. */
    private fun appendPlain(segments: MutableList<PassageSegment>, text: String) {
        val last = segments.lastOrNull()
        if (last != null && last.challengeId == null) {
            segments[segments.size - 1] = last.copy(text = last.text + text)
        } else {
            segments.add(PassageSegment(text))
        }
    }

    private fun tokenize(sentences: List<String>): List<Token> {
        val tokens = mutableListOf<Token>()
        for (sentence in sentences) {
            val words = sentence.split(' ').filter { it.isNotEmpty() }
            for ((w, rawWord) in words.withIndex()) {
                var split = rawWord.length
                while (split > 0 && rawWord[split - 1] in PUNCT_CHARS) split--
                tokens.add(
                    Token(
                        word = rawWord.substring(0, split),
                        punct = rawWord.substring(split),
                        sentenceStart = w == 0,
                    )
                )
            }
        }
        return tokens
    }

    private fun findCandidates(tokens: List<Token>, difficulty: Difficulty): List<Candidate> {
        val candidates = mutableListOf<Candidate>()
        for ((i, token) in tokens.withIndex()) {
            val bare = token.word.trim('"', '\'', '(', ')', '—')
            val lower = bare.lowercase()

            if (token.punct.isNotEmpty() && token.punct[0].toString() in PUNCT_POOL && token.word.isNotEmpty() &&
                token.word.last().isLetterOrDigit()
            ) {
                candidates.add(Candidate.Punctuation(i, token.punct[0].toString()))
            }
            if (bare == token.word) { // skip words wrapped in quotes to keep display simple
                if (difficulty != Difficulty.EASY && ConfusionSets.groupFor(lower) != null && !lower.contains('\'')) {
                    candidates.add(Candidate.Grammar(i, ConfusionSets.groupFor(lower)!!))
                }
                if (difficulty != Difficulty.EASY && ConfusionSets.isAgreementWord(lower)) {
                    candidates.add(Candidate.Agreement(i, ConfusionSets.agreementSwap(lower)!!))
                }
                if (bare.contains('\'') && bare.length >= 4 && bare.first().isLetter() && bare.last().isLetter()) {
                    candidates.add(Candidate.Apostrophe(i))
                }
                if (token.sentenceStart && bare.length > 1 && bare[0].isUpperCase() && bare[1].isLowerCase()) {
                    candidates.add(Candidate.Capitalization(i))
                }
            }
            if (difficulty.decoySlots && token.punct.isEmpty() && i < tokens.size - 1 &&
                !tokens[i + 1].sentenceStart && token.word.isNotEmpty() && token.word.last().isLetter()
            ) {
                candidates.add(Candidate.Decoy(i))
            }
        }
        return candidates
    }

    /**
     * Picks a difficulty-appropriate mix of mistakes, never placing two on the same
     * or adjacent tokens so the passage stays readable.
     */
    private fun selectCandidates(candidates: List<Candidate>, difficulty: Difficulty): List<Candidate> {
        val usedIndices = mutableSetOf<Int>()
        val chosen = mutableListOf<Candidate>()

        fun tryAdd(candidate: Candidate): Boolean {
            val idx = candidate.tokenIndex
            if ((idx - 1..idx + 1).any { it in usedIndices }) return false
            usedIndices.add(idx)
            chosen.add(candidate)
            return true
        }

        val grammarLike = candidates.filter {
            it is Candidate.Grammar || it is Candidate.Agreement || it is Candidate.Apostrophe
        }.shuffled(random)
        val punctuation = candidates.filterIsInstance<Candidate.Punctuation>().shuffled(random)
        val capitalization = candidates.filterIsInstance<Candidate.Capitalization>().shuffled(random)
        val decoys = candidates.filterIsInstance<Candidate.Decoy>().shuffled(random)

        val max = difficulty.maxChallenges
        val grammarQuota = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
        }

        var added = 0
        for (c in grammarLike) {
            if (added >= grammarQuota || chosen.size >= max) break
            if (tryAdd(c)) added++
        }
        if (difficulty.decoySlots) {
            for (c in decoys) {
                if (chosen.size >= max || chosen.count { it is Candidate.Decoy } >= 1) break
                tryAdd(c)
            }
        }
        for (c in punctuation) {
            if (chosen.size >= max) break
            tryAdd(c)
        }
        if (chosen.size < max) {
            for (c in capitalization) {
                if (chosen.size >= max || chosen.count { it is Candidate.Capitalization } >= 1) break
                tryAdd(c)
            }
        }
        return chosen.sortedBy { it.tokenIndex }
    }

    // --- Challenge builders -------------------------------------------------

    private fun punctuationChallenge(id: Int, mark: String, difficulty: Difficulty): Challenge {
        val distractors = (PUNCT_POOL - mark).shuffled(random).take(difficulty.optionCount - 2).toMutableList()
        if (difficulty != Difficulty.EASY) distractors.add(Challenge.NO_PUNCTUATION)
        val options = (distractors + mark).shuffled(random)
        return Challenge(
            id = id,
            type = ChallengeType.PUNCTUATION,
            shownText = Challenge.PUNCTUATION_SLOT,
            options = options,
            correctIndex = options.indexOf(mark),
            resolvedText = mark,
            explanation = PUNCT_EXPLANATIONS[mark] ?: "This punctuation mark belongs here.",
        )
    }

    private fun decoyChallenge(id: Int, difficulty: Difficulty): Challenge {
        val distractors = PUNCT_POOL.shuffled(random).take(difficulty.optionCount - 1)
        val options = (distractors + Challenge.NO_PUNCTUATION).shuffled(random)
        return Challenge(
            id = id,
            type = ChallengeType.PUNCTUATION,
            shownText = Challenge.PUNCTUATION_SLOT,
            options = options,
            correctIndex = options.indexOf(Challenge.NO_PUNCTUATION),
            resolvedText = "",
            explanation = PUNCT_EXPLANATIONS.getValue(Challenge.NO_PUNCTUATION),
        )
    }

    private fun grammarChallenge(
        id: Int,
        original: String,
        entry: ConfusionSets.Entry,
        difficulty: Difficulty,
    ): Challenge {
        val lower = original.lowercase()
        val wrong = entry.group.filter { it != lower }.random(random)
        val options = entry.group.map { matchCase(it, original) }.shuffled(random)
        val correct = matchCase(lower, original)
        val note = ConfusionSets.usageNote(lower)
        return Challenge(
            id = id,
            type = ChallengeType.GRAMMAR,
            shownText = matchCase(wrong, original),
            options = options,
            correctIndex = options.indexOf(correct),
            resolvedText = correct,
            explanation = note?.let { "In the original text it is \"$correct\" — $it." }
                ?: "The original text uses \"$correct\" here.",
        )
    }

    private fun agreementChallenge(id: Int, original: String, wrongForm: String): Challenge {
        val correct = original
        val shown = matchCase(wrongForm, original)
        val options = listOf(correct, shown).shuffled(random)
        return Challenge(
            id = id,
            type = ChallengeType.GRAMMAR,
            shownText = shown,
            options = options,
            correctIndex = options.indexOf(correct),
            resolvedText = correct,
            explanation = "The verb must agree with its subject — the original text uses \"$correct\".",
        )
    }

    private fun apostropheChallenge(id: Int, original: String): Challenge {
        val apostropheAt = original.indexOf('\'')
        val noApostrophe = original.replace("'", "")
        val shifted = buildString {
            append(noApostrophe)
            val newPos = if (apostropheAt + 1 < noApostrophe.length) apostropheAt + 1 else apostropheAt - 1
            insert(newPos.coerceIn(1, noApostrophe.length - 1), '\'')
        }
        val options = listOf(original, noApostrophe, shifted).distinct().shuffled(random)
        return Challenge(
            id = id,
            type = ChallengeType.APOSTROPHE,
            shownText = noApostrophe,
            options = options,
            correctIndex = options.indexOf(original),
            resolvedText = original,
            explanation = "\"$original\" needs its apostrophe to mark the missing letters.",
        )
    }

    private fun capitalizationChallenge(id: Int, original: String): Challenge {
        val lower = original.replaceFirstChar { it.lowercase() }
        val options = listOf(original, lower).shuffled(random)
        return Challenge(
            id = id,
            type = ChallengeType.CAPITALIZATION,
            shownText = lower,
            options = options,
            correctIndex = options.indexOf(original),
            resolvedText = original,
            explanation = "A sentence begins with a capital letter.",
        )
    }

    /** Applies the capitalization pattern of [pattern]'s first letter to [word]. */
    private fun matchCase(word: String, pattern: String): String =
        if (pattern.firstOrNull()?.isUpperCase() == true) {
            word.replaceFirstChar { it.uppercase() }
        } else {
            word
        }
}
