package com.maxdot.core.text

/**
 * Normalizes raw book text into clean paragraphs suitable for the game:
 * - strips Project Gutenberg header/footer boilerplate when present
 * - unwraps hard-wrapped lines into flowing paragraphs
 * - normalizes curly quotes, dashes and whitespace
 */
object TextCleaner {

    private val GUTENBERG_START = Regex("""\*{3}\s*START OF (THE|THIS) PROJECT GUTENBERG[^\n]*""", RegexOption.IGNORE_CASE)
    private val GUTENBERG_END = Regex("""\*{3}\s*END OF (THE|THIS) PROJECT GUTENBERG[^\n]*""", RegexOption.IGNORE_CASE)

    /** Runs of 4+ dots — table-of-contents dot leaders (e.g. "Chapter I ...... 12"). */
    private val DOT_LEADERS = Regex("""\.{4,}""")

    /** Phrases that mark a title/copyright page, so the leading block can be skipped. */
    private val FRONT_MATTER_KEYWORDS = Regex(
        "(?i)\\b(copyright|all rights reserved|isbn|table of contents|library of congress|" +
            "cataloging|first published|printed in|no part of this|work of fiction)\\b",
    )

    private val WHITESPACE = Regex("\\s+")

    /**
     * Whether a paragraph reads like real narrative prose (as opposed to a
     * heading, table-of-contents line, page number, or bare roman numeral).
     * Prose has sentence-ending punctuation, at least a few words, and is mostly
     * letters; it never contains dot-leader runs.
     */
    fun looksLikeProse(paragraph: String): Boolean {
        val t = paragraph.trim()
        if (t.isEmpty()) return false
        if (DOT_LEADERS.containsMatchIn(t)) return false
        if (t.none { it == '.' || it == '!' || it == '?' }) return false
        if (t.split(WHITESPACE).size < 3) return false
        return t.count { it.isLetter() } >= t.length * 0.5
    }

    /** Leading paragraphs to skip: copyright/TOC keywords, or anything non-prose. */
    private fun isFrontMatter(paragraph: String): Boolean =
        FRONT_MATTER_KEYWORDS.containsMatchIn(paragraph) || !looksLikeProse(paragraph)

    fun clean(raw: String): String {
        var text = raw
            .removePrefix("﻿")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        GUTENBERG_START.find(text)?.let { m -> text = text.substring(m.range.last + 1) }
        GUTENBERG_END.find(text)?.let { m -> text = text.substring(0, m.range.first) }

        text = text
            .replace('“', '"').replace('”', '"')
            .replace('‘', '\'').replace('’', '\'')
            .replace("--", "—")
            .replace('\t', ' ')
            .replace(' ', ' ')

        // Split into paragraphs on blank lines, unwrap single newlines within a paragraph.
        val paragraphs = text.split(Regex("\n\\s*\n"))
            .map { para ->
                para.replace('\n', ' ')
                    .replace(Regex(" {2,}"), " ")
                    .trim()
            }
            .filter { it.isNotEmpty() }

        // Skip the leading front-matter block (title/copyright/TOC), then drop any
        // remaining non-prose lines (chapter headings, TOC entries, page numbers).
        val prose = paragraphs
            .dropWhile { isFrontMatter(it) }
            .filter { looksLikeProse(it) }

        // Safety net: never let filtering wipe out a whole document.
        val kept = if (prose.isEmpty()) paragraphs else prose
        return kept.joinToString("\n\n")
    }

    /** Splits cleaned text into its paragraphs. */
    fun paragraphs(cleaned: String): List<String> =
        cleaned.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
}
