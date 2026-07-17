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

        return paragraphs.joinToString("\n\n")
    }

    /** Splits cleaned text into its paragraphs. */
    fun paragraphs(cleaned: String): List<String> =
        cleaned.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
}
