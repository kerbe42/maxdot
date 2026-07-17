package com.maxdot.core.text

/**
 * Heuristic sentence splitter tuned for 19th-century prose: aware of common
 * abbreviations and titles (Mr., Mrs., Dr., St. ...), single-letter initials,
 * decimal numbers, ellipses, and closing quotation marks.
 */
object SentenceSplitter {

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "st", "prof", "sr", "jr", "vs", "etc",
        "capt", "col", "gen", "lt", "rev", "hon", "no", "vol", "ch", "esq",
        "e.g", "i.e", "viz", "cf",
    )

    private const val CLOSERS = "\"')”’"

    /**
     * Splits cleaned text (as produced by [TextCleaner.clean]) into sentences.
     * Paragraph breaks always end a sentence.
     */
    fun split(cleanedText: String): List<String> =
        TextCleaner.paragraphs(cleanedText).flatMap { splitParagraph(it) }

    fun splitParagraph(paragraph: String): List<String> {
        val sentences = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < paragraph.length) {
            val c = paragraph[i]
            if (c == '.' || c == '!' || c == '?') {
                var end = i
                // Swallow runs of terminal punctuation ("?!", "...").
                while (end + 1 < paragraph.length && paragraph[end + 1] in ".!?") end++
                // Swallow closing quotes/brackets.
                while (end + 1 < paragraph.length && paragraph[end + 1] in CLOSERS) end++

                val isBoundary = when {
                    end + 1 >= paragraph.length -> true
                    paragraph[end + 1] != ' ' -> false
                    c == '.' && end == i && isNonTerminalPeriod(paragraph, i) -> false
                    else -> {
                        val next = nextNonSpace(paragraph, end + 1)
                        next == null || next.isUpperCase() || next.isDigit() || next in "\"'“‘—("
                    }
                }
                if (isBoundary) {
                    val sentence = paragraph.substring(start, end + 1).trim()
                    if (sentence.isNotEmpty()) sentences.add(sentence)
                    start = end + 1
                    i = end
                }
            }
            i++
        }
        val tail = paragraph.substring(start).trim()
        if (tail.isNotEmpty()) sentences.add(tail)
        return sentences
    }

    /** True when the period at [index] is part of an abbreviation, initial, or number. */
    private fun isNonTerminalPeriod(text: String, index: Int): Boolean {
        // Decimal number: digit on both sides.
        if (index > 0 && index + 1 < text.length &&
            text[index - 1].isDigit() && text[index + 1].isDigit()
        ) return true

        // Word immediately before the period.
        var wordStart = index
        while (wordStart > 0 && (text[wordStart - 1].isLetter() || text[wordStart - 1] == '.')) wordStart--
        val word = text.substring(wordStart, index).lowercase().trimStart('.')
        if (word in ABBREVIATIONS) return true
        // Single-letter initial such as "J." in "J. Watson".
        if (word.length == 1 && text[wordStart].isUpperCase()) return true
        return false
    }

    private fun nextNonSpace(text: String, from: Int): Char? {
        var j = from
        while (j < text.length && text[j] == ' ') j++
        return if (j < text.length) text[j] else null
    }
}
