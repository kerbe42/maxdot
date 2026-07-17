package com.maxdot.core.parse

import com.maxdot.core.model.ParsedBook
import com.maxdot.core.text.HtmlStripper
import com.maxdot.core.text.TextCleaner
import java.nio.charset.Charset

/**
 * Reader for MOBI / PalmDOC (.mobi, .prc, .azw) files: parses the PDB container,
 * decompresses PalmDOC (LZ77) text records, and strips the embedded HTML.
 * DRM-protected and HUFF/CDIC-compressed books are rejected with a clear error.
 */
object MobiParser {

    class MobiException(message: String) : Exception(message)

    private const val COMPRESSION_NONE = 1
    private const val COMPRESSION_PALMDOC = 2
    private const val COMPRESSION_HUFF_CDIC = 17480

    fun parse(bytes: ByteArray): ParsedBook {
        if (bytes.size < 80) throw MobiException("File too small to be a MOBI book")

        val typeCreator = String(bytes, 60, 8, Charsets.US_ASCII)
        if (typeCreator != "BOOKMOBI" && typeCreator != "TEXtREAd") {
            throw MobiException("Not a MOBI/PalmDOC book (unrecognized format)")
        }

        val numRecords = readU16(bytes, 76)
        if (numRecords < 2) throw MobiException("MOBI file has no text records")
        val recordOffsets = IntArray(numRecords) { readU32(bytes, 78 + it * 8).toInt() }

        fun record(i: Int): ByteArray {
            val start = recordOffsets[i]
            val end = if (i + 1 < numRecords) recordOffsets[i + 1] else bytes.size
            if (start < 0 || end > bytes.size || start >= end) throw MobiException("Corrupt MOBI record table")
            return bytes.copyOfRange(start, end)
        }

        val header = record(0)
        if (header.size < 16) throw MobiException("Corrupt PalmDOC header")
        val compression = readU16(header, 0)
        val textLength = readU32(header, 4)
        val textRecordCount = readU16(header, 8)
        val encryption = readU16(header, 12)

        if (encryption != 0) throw MobiException("This book is DRM-protected and cannot be imported")
        if (compression == COMPRESSION_HUFF_CDIC) {
            throw MobiException("HUFF/CDIC-compressed MOBI books are not supported — convert to EPUB and retry")
        }
        if (compression != COMPRESSION_NONE && compression != COMPRESSION_PALMDOC) {
            throw MobiException("Unknown MOBI compression type $compression")
        }

        var encoding: Charset = Charsets.UTF_8
        var title: String? = null
        var trailingFlags = 0
        val hasMobiHeader = header.size >= 24 && String(header, 16, 4, Charsets.US_ASCII) == "MOBI"
        if (hasMobiHeader) {
            val mobiHeaderLength = readU32(header, 20).toInt()
            val encodingValue = readU32(header, 28)
            encoding = when (encodingValue) {
                65001L -> Charsets.UTF_8
                1252L -> charsetOrDefault("windows-1252")
                else -> Charsets.UTF_8
            }
            if (header.size >= 92) {
                val fullNameOffset = readU32(header, 84).toInt()
                val fullNameLength = readU32(header, 88).toInt()
                if (fullNameLength in 1..2048 && fullNameOffset + fullNameLength <= header.size) {
                    title = String(header, fullNameOffset, fullNameLength, encoding).trim().ifEmpty { null }
                }
            }
            if (mobiHeaderLength >= 228 && header.size >= 244) {
                trailingFlags = readU16(header, 242)
            }
        }

        val out = StringBuilder()
        val decoded = ArrayList<ByteArray>(textRecordCount)
        var decodedBytes = 0L
        for (i in 1..textRecordCount.coerceAtMost(numRecords - 1)) {
            var rec = record(i)
            rec = trimTrailingEntries(rec, trailingFlags)
            val data = if (compression == COMPRESSION_PALMDOC) decompressPalmDoc(rec) else rec
            decoded.add(data)
            decodedBytes += data.size
            if (decodedBytes > 40L * 1024 * 1024) throw MobiException("MOBI book too large to import")
        }
        val all = ByteArray(decodedBytes.toInt())
        var pos = 0
        for (d in decoded) {
            d.copyInto(all, pos)
            pos += d.size
        }
        val limit = if (textLength in 1..all.size) textLength.toInt() else all.size
        out.append(String(all, 0, limit, encoding))

        val text = TextCleaner.clean(HtmlStripper.strip(out.toString()))
        if (text.length < 200) throw MobiException("MOBI book contained almost no text")
        return ParsedBook(title, null, text)
    }

    /**
     * Removes per-record trailing entries indicated by the MOBI extra-data flags,
     * which would otherwise corrupt the decompressed text.
     */
    private fun trimTrailingEntries(record: ByteArray, flags: Int): ByteArray {
        var size = record.size
        var testFlags = flags shr 1
        while (testFlags != 0 && size > 0) {
            if (testFlags and 1 != 0) {
                size -= backwardEncodedSize(record, size)
            }
            testFlags = testFlags shr 1
        }
        if (flags and 1 != 0 && size > 0) {
            size -= (record[size - 1].toInt() and 0x3) + 1
        }
        return if (size in 0 until record.size) record.copyOfRange(0, size) else record
    }

    /** Reads a backward-encoded variable-width integer ending at [end]. */
    private fun backwardEncodedSize(record: ByteArray, end: Int): Int {
        var value = 0
        for (i in maxOf(0, end - 4) until end) {
            val b = record[i].toInt() and 0xFF
            if (b and 0x80 != 0) value = 0
            value = (value shl 7) or (b and 0x7F)
        }
        return value
    }

    /** PalmDOC LZ77 decompression. */
    internal fun decompressPalmDoc(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>(data.size * 2)
        var i = 0
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            when {
                b == 0x00 -> {
                    out.add(0)
                    i++
                }
                b in 0x01..0x08 -> {
                    i++
                    var n = b
                    while (n > 0 && i < data.size) {
                        out.add(data[i])
                        i++
                        n--
                    }
                }
                b <= 0x7F -> {
                    out.add(b.toByte())
                    i++
                }
                b in 0x80..0xBF -> {
                    if (i + 1 >= data.size) break
                    val pair = ((b and 0x3F) shl 8) or (data[i + 1].toInt() and 0xFF)
                    val distance = pair shr 3
                    val length = (pair and 0x07) + 3
                    if (distance in 1..out.size) {
                        repeat(length) {
                            out.add(out[out.size - distance])
                        }
                    }
                    i += 2
                }
                else -> { // 0xC0..0xFF: space + ASCII char
                    out.add(' '.code.toByte())
                    out.add((b xor 0x80).toByte())
                    i++
                }
            }
        }
        return out.toByteArray()
    }

    private fun charsetOrDefault(name: String): Charset =
        try {
            Charset.forName(name)
        } catch (_: Exception) {
            Charsets.ISO_8859_1
        }

    private fun readU16(b: ByteArray, offset: Int): Int =
        ((b[offset].toInt() and 0xFF) shl 8) or (b[offset + 1].toInt() and 0xFF)

    private fun readU32(b: ByteArray, offset: Int): Long =
        ((b[offset].toLong() and 0xFF) shl 24) or
            ((b[offset + 1].toLong() and 0xFF) shl 16) or
            ((b[offset + 2].toLong() and 0xFF) shl 8) or
            (b[offset + 3].toLong() and 0xFF)
}
