package com.maxdot.core

import com.maxdot.core.parse.EpubParser
import com.maxdot.core.parse.MobiParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EpubParserTest {

    private fun buildEpub(vararg files: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val chapterBody = (1..30).joinToString(" ") { "This is sentence number $it of the story, told plainly." }

    @Test
    fun `parses epub with container and spine order`() {
        val epub = buildEpub(
            "META-INF/container.xml" to """
                <?xml version="1.0"?>
                <container><rootfiles>
                  <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                </rootfiles></container>
            """.trimIndent(),
            "OEBPS/content.opf" to """
                <package><metadata>
                  <dc:title>Test Book</dc:title>
                  <dc:creator>Jane Author</dc:creator>
                </metadata>
                <manifest>
                  <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
                  <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
                </manifest>
                <spine><itemref idref="c1"/><itemref idref="c2"/></spine>
                </package>
            """.trimIndent(),
            "OEBPS/ch2.xhtml" to "<html><body><p>SECOND_CHAPTER $chapterBody</p></body></html>",
            "OEBPS/ch1.xhtml" to "<html><body><p>FIRST_CHAPTER $chapterBody</p></body></html>",
        )
        val book = EpubParser.parse(ByteArrayInputStream(epub))
        assertEquals("Test Book", book.title)
        assertEquals("Jane Author", book.author)
        // Spine order (c1 then c2) must win over zip order (ch2 first).
        assertTrue(book.text.indexOf("FIRST_CHAPTER") < book.text.indexOf("SECOND_CHAPTER"))
    }

    @Test
    fun `falls back to html files when no opf`() {
        val epub = buildEpub(
            "a.html" to "<html><body><p>ALPHA $chapterBody</p></body></html>",
            "b.html" to "<html><body><p>BETA $chapterBody</p></body></html>",
        )
        val book = EpubParser.parse(ByteArrayInputStream(epub))
        assertTrue(book.text.contains("ALPHA"))
        assertTrue(book.text.contains("BETA"))
    }

    @Test
    fun `rejects empty archive`() {
        assertFailsWith<EpubParser.EpubException> {
            EpubParser.parse(ByteArrayInputStream(buildEpub()))
        }
    }
}

class MobiParserTest {

    @Test
    fun `palmdoc decompression handles all opcode classes`() {
        // "Hello" + backref(distance=3, length=3) => "Hellollo", then 0xC1 => " A"
        val compressed = byteArrayOf(
            'H'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte(),
            0x80.toByte(), 0x18, // (3 << 3) | (3 - 3)
            0xC1.toByte(),
        )
        assertEquals("Hellollo A", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun `literal run opcode copies bytes verbatim`() {
        val compressed = byteArrayOf(0x02, '<'.code.toByte(), '>'.code.toByte(), 'x'.code.toByte())
        assertEquals("<>x", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun `parses uncompressed palmdoc pdb`() {
        val text = ("<html><body><p>" +
            (1..40).joinToString(" ") { "Uncompressed sentence number $it with several words." } +
            "</p></body></html>").toByteArray(Charsets.UTF_8)
        val pdb = buildPdb(compression = 1, textBytes = text)
        val book = MobiParser.parse(pdb)
        assertTrue(book.text.contains("Uncompressed sentence number 1"))
        assertTrue(book.text.contains("number 40"))
    }

    @Test
    fun `rejects non-mobi files`() {
        assertFailsWith<MobiParser.MobiException> {
            MobiParser.parse(ByteArray(200)) // zeros: bad type/creator
        }
    }

    @Test
    fun `rejects drm-protected books`() {
        val pdb = buildPdb(compression = 1, textBytes = "hello".toByteArray(), encryption = 2)
        assertFailsWith<MobiParser.MobiException> { MobiParser.parse(pdb) }
    }

    /** Builds a minimal 2-record PalmDOC PDB: header record + one text record. */
    private fun buildPdb(compression: Int, textBytes: ByteArray, encryption: Int = 0): ByteArray {
        val header = ByteArray(16)
        header[0] = (compression shr 8).toByte(); header[1] = compression.toByte()
        val len = textBytes.size
        header[4] = (len shr 24).toByte(); header[5] = (len shr 16).toByte()
        header[6] = (len shr 8).toByte(); header[7] = len.toByte()
        header[8] = 0; header[9] = 1 // one text record
        header[10] = 0x10; header[11] = 0 // record size 4096
        header[12] = (encryption shr 8).toByte(); header[13] = encryption.toByte()

        val numRecords = 2
        val headerSize = 78 + numRecords * 8
        val rec0Offset = headerSize
        val rec1Offset = rec0Offset + header.size

        val out = ByteArrayOutputStream()
        val pdbHeader = ByteArray(78)
        "TestBook".toByteArray(Charsets.US_ASCII).copyInto(pdbHeader, 0)
        "TEXtREAd".toByteArray(Charsets.US_ASCII).copyInto(pdbHeader, 60)
        pdbHeader[76] = 0; pdbHeader[77] = numRecords.toByte()
        out.write(pdbHeader)
        for (offset in intArrayOf(rec0Offset, rec1Offset)) {
            out.write(byteArrayOf(
                (offset shr 24).toByte(), (offset shr 16).toByte(),
                (offset shr 8).toByte(), offset.toByte(),
                0, 0, 0, 0,
            ))
        }
        out.write(header)
        out.write(textBytes)
        return out.toByteArray()
    }
}
