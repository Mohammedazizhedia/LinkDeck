package com.linkdeck.app.ui.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.files.FileRepository
import com.linkdeck.app.files.TransferManager
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.FileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilesUiState(
    val path: String = "/",
    val entries: List<FileEntry> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val selected: FileEntry? = null,
    val preview: android.graphics.Bitmap? = null,
    val error: String? = null,
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    val repository: FileRepository,
    val transfers: TransferManager,
    val connectionManager: ConnectionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FilesUiState())
    val state: StateFlow<FilesUiState> = _state

    fun open(path: String) {
        _state.value = _state.value.copy(path = path, loading = true, selected = null, preview = null)
        refresh()
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query)
        refresh()
    }

    fun refresh() {
        val s = _state.value
        viewModelScope.launch {
            repository.list(s.path, s.query)
                .onSuccess { _state.value = _state.value.copy(entries = it, loading = false, error = null) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun select(entry: FileEntry) {
        if (entry.isDir) { open(entry.path); return }
        _state.value = _state.value.copy(selected = entry, preview = null)
        viewModelScope.launch {
            repository.thumbnail(entry.path).onSuccess { bmp ->
                if (_state.value.selected?.path == entry.path) _state.value = _state.value.copy(preview = bmp)
            }
        }
    }

    fun pullSelected() = _state.value.selected?.let { transfers.pull(it.path) }
    fun push(local: File) = transfers.push(local, _state.value.path)

    /** Pulls to cache and opens with the tablet's own viewer apps. */
    fun openOnTablet(context: Context, entry: FileEntry) {
        viewModelScope.launch {
            repository.pullToCache(entry.path) { _, _ -> }.onSuccess { file ->
                withContext(Dispatchers.Main) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context, context.packageName + ".fileprovider", file,
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, entry.mime.ifEmpty { "*/*" })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, null)) }
                }
            }
        }
    }
}
