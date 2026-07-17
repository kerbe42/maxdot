package com.maxdot.core

import com.maxdot.core.text.HtmlStripper
import com.maxdot.core.text.SentenceSplitter
import com.maxdot.core.text.TextCleaner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextCleanerTest {

    @Test
    fun `unwraps hard-wrapped paragraphs and keeps paragraph breaks`() {
        val raw = "It is a truth universally\nacknowledged, that a single man\nin possession of a good fortune.\n\nSecond paragraph here."
        val cleaned = TextCleaner.clean(raw)
        val paragraphs = TextCleaner.paragraphs(cleaned)
        assertEquals(2, paragraphs.size)
        assertTrue(paragraphs[0].contains("universally acknowledged, that"))
    }

    @Test
    fun `strips gutenberg boilerplate`() {
        val raw = """
            Some legal header text.
            *** START OF THE PROJECT GUTENBERG EBOOK PRIDE AND PREJUDICE ***

            The real book text.

            *** END OF THE PROJECT GUTENBERG EBOOK PRIDE AND PREJUDICE ***
            License stuff.
        """.trimIndent()
        val cleaned = TextCleaner.clean(raw)
        assertEquals("The real book text.", cleaned)
    }

    @Test
    fun `normalizes curly quotes and double dashes`() {
        val cleaned = TextCleaner.clean("“Hello,” she said — no, wait--yes.")
        assertEquals("\"Hello,\" she said — no, wait—yes.", cleaned)
    }
}

class SentenceSplitterTest {

    @Test
    fun `splits simple sentences`() {
        val sentences = SentenceSplitter.split("The dog ran. The cat slept! Did the bird sing?")
        assertEquals(3, sentences.size)
        assertEquals("The cat slept!", sentences[1])
    }

    @Test
    fun `does not split on titles and abbreviations`() {
        val sentences = SentenceSplitter.split("Mr. Bennet replied that he had not. Mrs. Long has just been here.")
        assertEquals(2, sentences.size)
        assertTrue(sentences[0].startsWith("Mr. Bennet"))
        assertTrue(sentences[1].startsWith("Mrs. Long"))
    }

    @Test
    fun `does not split on initials or decimals`() {
        val sentences = SentenceSplitter.split("Dr. J. Watson paid 3.50 pounds. He was pleased.")
        assertEquals(2, sentences.size)
    }

    @Test
    fun `keeps closing quotes with the sentence`() {
        val sentences = SentenceSplitter.split("\"Come here!\" she cried. \"I am ready.\" He nodded.")
        assertEquals(3, sentences.size)
        assertEquals("\"Come here!\" she cried.", sentences[0])
        assertEquals("\"I am ready.\"", sentences[1])
    }

    @Test
    fun `paragraph break always ends a sentence`() {
        val sentences = SentenceSplitter.split("A line with no period\n\nNext paragraph starts.")
        assertEquals(2, sentences.size)
    }
}

class HtmlStripperTest {

    @Test
    fun `strips tags and decodes entities`() {
        val html = "<html><head><title>x</title></head><body><p>It&rsquo;s a &ldquo;test&rdquo; &amp; more.</p><p>Two</p></body></html>"
        val text = HtmlStripper.strip(html)
        assertTrue(text.contains("It's a \"test\" & more."))
        assertFalse(text.contains("<"))
        assertFalse(text.contains("title"))
    }

    @Test
    fun `decodes numeric entities`() {
        assertEquals("A B", HtmlStripper.strip("A&#32;B"))
        assertTrue(HtmlStripper.strip("caf&#233;").contains("café"))
    }
}
