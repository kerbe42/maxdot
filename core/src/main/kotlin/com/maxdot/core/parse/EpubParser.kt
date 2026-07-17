package com.maxdot.core.parse

import com.maxdot.core.model.ParsedBook
import com.maxdot.core.text.HtmlStripper
import com.maxdot.core.text.TextCleaner
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Minimal EPUB reader: locates the OPF package file via META-INF/container.xml,
 * reads the spine to get chapter order, and strips the XHTML down to plain text.
 * Falls back to "all HTML files in zip order" for malformed books.
 */
object EpubParser {

    private const val MAX_TOTAL_BYTES = 40L * 1024 * 1024

    class EpubException(message: String) : Exception(message)

    fun parse(input: InputStream): ParsedBook {
        val entries = readZip(input)
        if (entries.isEmpty()) throw EpubException("Not a valid EPUB (empty or unreadable archive)")

        val containerXml = entries.entries
            .firstOrNull { it.key.equals("META-INF/container.xml", ignoreCase = true) }
            ?.value?.toString(Charsets.UTF_8)

        val opfPath = containerXml?.let {
            Regex("""full-path\s*=\s*["']([^"']+)["']""").find(it)?.groupValues?.get(1)
        }
        val opfBytes = opfPath?.let { path -> entries.entries.firstOrNull { it.key == path }?.value }

        var title: String? = null
        var author: String? = null
        val orderedDocs: List<ByteArray>

        if (opfBytes != null) {
            val opf = opfBytes.toString(Charsets.UTF_8)
            title = Regex("""<dc:title[^>]*>(.*?)</dc:title>""", RegexOption.DOT_MATCHES_ALL)
                .find(opf)?.groupValues?.get(1)?.trim()?.let { HtmlStripper.strip(it).trim() }
            author = Regex("""<dc:creator[^>]*>(.*?)</dc:creator>""", RegexOption.DOT_MATCHES_ALL)
                .find(opf)?.groupValues?.get(1)?.trim()?.let { HtmlStripper.strip(it).trim() }

            val opfDir = opfPath.substringBeforeLast('/', "")
            val manifest = mutableMapOf<String, String>()
            Regex("""<item\b[^>]*>""").findAll(opf).forEach { m ->
                val tag = m.value
                val id = Regex("""\bid\s*=\s*["']([^"']+)["']""").find(tag)?.groupValues?.get(1)
                val href = Regex("""\bhref\s*=\s*["']([^"']+)["']""").find(tag)?.groupValues?.get(1)
                if (id != null && href != null) manifest[id] = href
            }
            val spineIds = Regex("""<itemref\b[^>]*\bidref\s*=\s*["']([^"']+)["']""")
                .findAll(opf).map { it.groupValues[1] }.toList()

            orderedDocs = spineIds.mapNotNull { idref ->
                val href = manifest[idref] ?: return@mapNotNull null
                val resolved = resolvePath(opfDir, href)
                entries.entries.firstOrNull { it.key == resolved }?.value
            }.ifEmpty { htmlEntriesInOrder(entries) }
        } else {
            orderedDocs = htmlEntriesInOrder(entries)
        }

        if (orderedDocs.isEmpty()) throw EpubException("No readable chapters found in EPUB")

        val text = buildString {
            for (doc in orderedDocs) {
                append(HtmlStripper.strip(doc.toString(Charsets.UTF_8)))
                append("\n\n")
            }
        }
        val cleaned = TextCleaner.clean(text)
        if (cleaned.length < 200) throw EpubException("EPUB contained almost no text")
        return ParsedBook(title, author, cleaned)
    }

    private fun htmlEntriesInOrder(entries: LinkedHashMap<String, ByteArray>): List<ByteArray> =
        entries.filterKeys { name ->
            val lower = name.lowercase()
            (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) &&
                !lower.contains("toc") && !lower.contains("nav") && !lower.contains("cover")
        }.values.toList()

    private fun resolvePath(baseDir: String, href: String): String {
        val raw = href.substringBefore('#')
        val combined = if (baseDir.isEmpty()) raw else "$baseDir/$raw"
        val parts = mutableListOf<String>()
        for (part in combined.split('/')) {
            when (part) {
                "", "." -> {}
                ".." -> parts.removeLastOrNull()
                else -> parts.add(part)
            }
        }
        return parts.joinToString("/")
    }

    private fun readZip(input: InputStream): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name.lowercase()
                val wanted = name.endsWith(".xhtml") || name.endsWith(".html") ||
                    name.endsWith(".htm") || name.endsWith(".opf") || name.endsWith(".xml") ||
                    name.endsWith(".ncx")
                if (!wanted) continue
                val bytes = zip.readBytes()
                total += bytes.size
                if (total > MAX_TOTAL_BYTES) throw EpubException("EPUB too large to import")
                entries[entry.name] = bytes
            }
        }
        return entries
    }
}
