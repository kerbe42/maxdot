package com.maxdot.app.data

import android.content.Context
import android.net.Uri
import com.maxdot.core.model.ParsedBook
import com.maxdot.core.parse.EpubParser
import com.maxdot.core.parse.MobiParser
import com.maxdot.core.text.SentenceSplitter
import com.maxdot.core.text.TextCleaner
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isBundled: Boolean,
)

data class BookProgress(
    val position: Int = 0,
    val sentenceCount: Int = 0,
    val correct: Int = 0,
    val answered: Int = 0,
    val timesFinished: Int = 0,
) {
    val percent: Int
        get() = if (sentenceCount == 0) 0 else ((position * 100) / sentenceCount).coerceIn(0, 100)
}

/**
 * Owns the book catalog: bundled classics shipped in assets plus user-imported
 * books stored as cleaned plain text in app-private storage.
 */
class BookRepository(private val context: Context) {

    class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private val importedDir: File = File(context.filesDir, "books").apply { mkdirs() }
    private val catalogFile: File = File(importedDir, "catalog.json")
    private val progressPrefs =
        context.getSharedPreferences("maxdot_book_progress", Context.MODE_PRIVATE)

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val sentenceCache = mutableMapOf<String, List<String>>()

    init {
        refresh()
    }

    private fun refresh() {
        _books.value = loadBundledCatalog() + loadImportedCatalog()
    }

    private fun loadBundledCatalog(): List<Book> = try {
        val json = context.assets.open("books/index.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Book(
                id = "bundled_" + obj.getString("id"),
                title = obj.getString("title"),
                author = obj.getString("author"),
                isBundled = true,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun loadImportedCatalog(): List<Book> = try {
        if (!catalogFile.exists()) return emptyList()
        val array = JSONArray(catalogFile.readText())
        (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            if (!textFileFor(id).exists()) return@mapNotNull null
            Book(
                id = id,
                title = obj.getString("title"),
                author = obj.optString("author", "Unknown author"),
                isBundled = false,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    // --- Text access --------------------------------------------------------

    /** Loads (and caches) the sentence list for a book. */
    fun sentences(book: Book): List<String> = sentenceCache.getOrPut(book.id) {
        val raw = if (book.isBundled) {
            val assetId = book.id.removePrefix("bundled_")
            context.assets.open("books/$assetId.txt").bufferedReader().use { it.readText() }
        } else {
            textFileFor(book.id).readText()
        }
        SentenceSplitter.split(TextCleaner.clean(raw))
    }

    // --- Import -------------------------------------------------------------

    /**
     * Imports a user-selected document. Detects EPUB/MOBI/PDF/plain text by magic
     * bytes and file name, converts to cleaned text, and stores it privately.
     */
    fun import(uri: Uri): Book {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(uri) ?: "Imported book"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw ImportException("Could not open the selected file")
        if (bytes.size > 60 * 1024 * 1024) throw ImportException("File is too large to import (60 MB max)")

        val parsed: ParsedBook = try {
            when (detectFormat(bytes, displayName)) {
                Format.EPUB -> EpubParser.parse(bytes.inputStream())
                Format.MOBI -> MobiParser.parse(bytes)
                Format.PDF -> parsePdf(bytes)
                Format.TEXT -> ParsedBook(null, null, TextCleaner.clean(bytes.toString(Charsets.UTF_8)))
            }
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            throw ImportException(e.message ?: "Could not read this book", e)
        }

        if (parsed.text.length < 300) {
            throw ImportException("This file contains too little readable text to play with")
        }

        val id = "imported_" + System.currentTimeMillis()
        val title = parsed.title?.takeIf { it.isNotBlank() }
            ?: displayName.substringBeforeLast('.').replace(Regex("[_-]+"), " ").trim()
                .ifEmpty { "Imported book" }
        textFileFor(id).writeText(parsed.text)

        val entry = JSONObject()
            .put("id", id)
            .put("title", title)
            .put("author", parsed.author ?: "Imported")
        val catalog = if (catalogFile.exists()) JSONArray(catalogFile.readText()) else JSONArray()
        catalog.put(entry)
        catalogFile.writeText(catalog.toString())
        refresh()
        return Book(id, title, parsed.author ?: "Imported", isBundled = false)
    }

    fun deleteImported(book: Book) {
        if (book.isBundled) return
        textFileFor(book.id).delete()
        sentenceCache.remove(book.id)
        if (catalogFile.exists()) {
            val catalog = JSONArray(catalogFile.readText())
            val kept = JSONArray()
            for (i in 0 until catalog.length()) {
                val obj = catalog.getJSONObject(i)
                if (obj.getString("id") != book.id) kept.put(obj)
            }
            catalogFile.writeText(kept.toString())
        }
        clearProgress(book.id)
        refresh()
    }

    private enum class Format { EPUB, MOBI, PDF, TEXT }

    private fun detectFormat(bytes: ByteArray, name: String): Format {
        val lower = name.lowercase()
        if (bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()
        ) return Format.PDF
        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) return Format.EPUB
        if (bytes.size >= 68) {
            val typeCreator = String(bytes, 60, 8, Charsets.US_ASCII)
            if (typeCreator == "BOOKMOBI" || typeCreator == "TEXtREAd") return Format.MOBI
        }
        return when {
            lower.endsWith(".epub") -> Format.EPUB
            lower.endsWith(".mobi") || lower.endsWith(".prc") || lower.endsWith(".azw") -> Format.MOBI
            lower.endsWith(".pdf") -> Format.PDF
            else -> Format.TEXT
        }
    }

    private fun parsePdf(bytes: ByteArray): ParsedBook {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(bytes).use { doc ->
            if (doc.isEncrypted) throw ImportException("This PDF is password-protected and cannot be imported")
            val stripper = PDFTextStripper()
            val text = stripper.getText(doc)
            return ParsedBook(null, null, TextCleaner.clean(text))
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }

    private fun textFileFor(id: String) = File(importedDir, "$id.txt")

    // --- Per-book progress --------------------------------------------------

    fun progress(bookId: String): BookProgress = BookProgress(
        position = progressPrefs.getInt("${bookId}_pos", 0),
        sentenceCount = progressPrefs.getInt("${bookId}_count", 0),
        correct = progressPrefs.getInt("${bookId}_correct", 0),
        answered = progressPrefs.getInt("${bookId}_answered", 0),
        timesFinished = progressPrefs.getInt("${bookId}_finished", 0),
    )

    fun saveProgress(bookId: String, progress: BookProgress) {
        progressPrefs.edit()
            .putInt("${bookId}_pos", progress.position)
            .putInt("${bookId}_count", progress.sentenceCount)
            .putInt("${bookId}_correct", progress.correct)
            .putInt("${bookId}_answered", progress.answered)
            .putInt("${bookId}_finished", progress.timesFinished)
            .apply()
    }

    /** Erases reading positions and per-book stats for every book. */
    fun clearAllProgress() {
        progressPrefs.edit().clear().apply()
    }

    private fun clearProgress(bookId: String) {
        progressPrefs.edit()
            .remove("${bookId}_pos")
            .remove("${bookId}_count")
            .remove("${bookId}_correct")
            .remove("${bookId}_answered")
            .remove("${bookId}_finished")
            .apply()
    }
}
