package com.maxdot.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxdot.app.data.Book
import com.maxdot.app.data.BookRepository
import com.maxdot.app.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportStatus {
    data object Idle : ImportStatus()
    data object Working : ImportStatus()
    data class Done(val book: Book) : ImportStatus()
    data class Failed(val message: String) : ImportStatus()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val profiles = ProfileRepository(application)
    val books = BookRepository(application)

    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus

    fun importBook(uri: Uri) {
        _importStatus.value = ImportStatus.Working
        viewModelScope.launch {
            try {
                val book = withContext(Dispatchers.IO) { books.import(uri) }
                profiles.recordBookImported()
                _importStatus.value = ImportStatus.Done(book)
            } catch (e: Exception) {
                _importStatus.value = ImportStatus.Failed(e.message ?: "Import failed")
            }
        }
    }

    fun clearImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }
}
