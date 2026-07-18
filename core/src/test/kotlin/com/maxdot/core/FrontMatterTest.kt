package com.maxdot.core

import com.maxdot.core.text.TextCleaner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FrontMatterTest {

    @Test
    fun `drops leading title copyright and table of contents`() {
        val raw = """
            The Great Novel

            A Story in Three Parts

            by Jane Author

            Copyright (c) 2021 Jane Author. All rights reserved.

            CONTENTS

            Chapter I. The Beginning .......... 1
            Chapter II. The Middle .......... 45
            Chapter III. The End .......... 98

            CHAPTER I

            It was a bright cold day in April, and the clocks were striking thirteen. Winston Smith slipped quickly through the glass doors of Victory Mansions.
        """.trimIndent()
        val cleaned = TextCleaner.clean(raw)
        assertFalse(cleaned.contains("CONTENTS"), "TOC header leaked: $cleaned")
        assertFalse(cleaned.contains(".........."), "dot leaders leaked")
        assertFalse(cleaned.contains("All rights reserved"), "copyright leaked")
        assertFalse(cleaned.contains("CHAPTER"), "chapter heading leaked")
        assertTrue(
            cleaned.startsWith("It was a bright cold day"),
            "should start at real prose; started with: ${cleaned.take(50)}",
        )
    }

    @Test
    fun `drops chapter headings between prose`() {
        val raw = "First real paragraph of the story goes on for a while here.\n\n" +
            "CHAPTER II\n\nSecond real paragraph continues the tale quite nicely indeed."
        val paras = TextCleaner.paragraphs(TextCleaner.clean(raw))
        assertTrue(paras.none { it.equals("CHAPTER II", ignoreCase = true) }, "heading kept: $paras")
        assertEquals(2, paras.size)
    }

    @Test
    fun `keeps short dialogue paragraphs`() {
        val raw = "\"I won't,\" she said.\n\n\"You must,\" he replied firmly."
        val paras = TextCleaner.paragraphs(TextCleaner.clean(raw))
        assertEquals(2, paras.size, "dialogue was dropped: $paras")
    }

    @Test
    fun `falls back to unfiltered when nothing looks like prose`() {
        val raw = "CONTENTS\n\nChapter 1\n\nChapter 2\n\nIndex"
        val cleaned = TextCleaner.clean(raw)
        assertTrue(cleaned.isNotBlank(), "over-filtered a whole document to nothing")
    }

    @Test
    fun `looksLikeProse classifies prose vs front matter`() {
        assertTrue(TextCleaner.looksLikeProse("It was the best of times, it was the worst of times."))
        assertTrue(TextCleaner.looksLikeProse("\"I won't,\" she said."))
        assertTrue(TextCleaner.looksLikeProse("Second paragraph here."))
        assertFalse(TextCleaner.looksLikeProse("CHAPTER XIV"))
        assertFalse(TextCleaner.looksLikeProse("The Speckled Band .......... 145"))
        assertFalse(TextCleaner.looksLikeProse("CONTENTS"))
        assertFalse(TextCleaner.looksLikeProse("147"))
        assertFalse(TextCleaner.looksLikeProse("The Adventure of the Speckled Band"))
    }
}
