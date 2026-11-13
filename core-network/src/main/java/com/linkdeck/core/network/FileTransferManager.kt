package com.linkdeck.core.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class FileTransferManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    data class TransferProgress(
        val transferId: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val status: Status
    )

    enum class Status { RUNNING, COMPLETED, CANCELLED, FAILED }

    private val _progress = MutableSharedFlow<TransferProgress>()
    val progress: SharedFlow<TransferProgress> = _progress.asSharedFlow()

    private val activeTransfers = ConcurrentHashMap<String, Job>()

    fun startUpload(
        transferId: String,
        path: String,
        totalSize: Long,
        sha256: String,
        onChunkRequested: suspend (offset: Long, chunkSize: Int) -> ByteArray?
    ) {
        if (activeTransfers.containsKey(transferId)) return

        activeTransfers[transferId] = scope.launch {
            try {
                var offset = 0L
                val bufferSize = 256 * 1024 // 256KB

                while (offset < totalSize) {
                    val bytesToRead = minOf(bufferSize.toLong(), totalSize - offset).toInt()
                    val chunk = onChunkRequested(offset, bytesToRead)
                        ?: throw IllegalStateException("Chunk request failed")

                    if (chunk.isEmpty()) break

                    offset += chunk.size
                    _progress.emit(TransferProgress(transferId, offset, totalSize, Status.RUNNING))
                }

                _progress.emit(TransferProgress(transferId, offset, totalSize, Status.COMPLETED))
            } catch (e: CancellationException) {
                _progress.emit(TransferProgress(transferId, 0, totalSize, Status.CANCELLED))
            } catch (e: Exception) {
                _progress.emit(TransferProgress(transferId, 0, totalSize, Status.FAILED))
            } finally {
                activeTransfers.remove(transferId)
            }
        }
    }

    fun cancelTransfer(transferId: String) {
        activeTransfers[transferId]?.cancel()
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
