package com.linkdeck.app.files

import android.content.Context
import com.linkdeck.core.network.ConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Transfer(
    val id: String,
    val name: String,
    val direction: String, // "pull" | "push"
    val remotePath: String,
    val done: Long,
    val total: Long,
    val active: Boolean,
    val error: String? = null,
) { val progress: Float get() = if (total <= 0) 0f else done.toFloat() / total }

/** Tracks all file transfers with live progress and resume on reconnect. */
@Singleton
class TransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepository: FileRepository,
    private val connectionManager: ConnectionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    val transfers: StateFlow<List<Transfer>> = _transfers

    fun pull(remotePath: String) {
        val id = "pull:$remotePath"
        upsert(Transfer(id, remotePath.substringAfterLast('/'), "pull", remotePath, 0, 0, true))
        scope.launch {
            fileRepository.pullToCache(remotePath) { done, total ->
                upsert(Transfer(id, remotePath.substringAfterLast('/'), "pull", remotePath, done, total, true))
            }.onSuccess { upsert(Transfer(id, remotePath.substringAfterLast('/'), "pull", remotePath, 1, 1, false)) }
                .onFailure { e -> upsert(Transfer(id, remotePath.substringAfterLast('/'), "pull", remotePath, 0, 0, false, error = e.message)) }
        }
    }

    fun push(local: File, remoteDir: String) {
        val remotePath = remoteDir.trimEnd('/') + "/" + local.name
        val id = "push:$remotePath"
        upsert(Transfer(id, local.name, "push", remotePath, 0, 0, true))
        scope.launch {
            fileRepository.push(local, remotePath) { done, total ->
                upsert(Transfer(id, local.name, "push", remotePath, done, total, true))
            }.onSuccess { upsert(Transfer(id, local.name, "push", remotePath, 1, 1, false)) }
                .onFailure { e -> upsert(Transfer(id, local.name, "push", remotePath, 0, 0, false, error = e.message)) }
        }
    }

    fun remove(id: String) = _transfers.update { list -> list.filterNot { it.id == id } }

    private fun upsert(t: Transfer) = _transfers.update { list ->
        (list.filterNot { it.id == t.id } + t).sortedBy { it.id }
    }
}
