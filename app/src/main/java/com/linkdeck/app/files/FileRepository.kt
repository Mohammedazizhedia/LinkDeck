package com.linkdeck.app.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.FileChunkReq
import com.linkdeck.core.network.FileChunkRes
import com.linkdeck.core.network.FileEntry
import com.linkdeck.core.network.FileListReq
import com.linkdeck.core.network.FileListRes
import com.linkdeck.core.network.FileStatRes
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** High-level controller-side API over the file protocol. All paths are host-relative. */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: ConnectionManager,
) {
    companion object { const val CHUNK = 256 * 1024 }

    suspend fun list(path: String, query: String = ""): Result<List<FileEntry>> =
        connectionManager.request("file_list", ProtoJson.encodeToString(FileListReq.serializer(), FileListReq(path, query)))
            .mapCatching { ProtoJson.decodeFromString(FileListRes.serializer(), it) }
            .mapCatching { if (it.ok) it.entries else error(it.error) }

    suspend fun stat(path: String): Result<FileStatRes> =
        connectionManager.request("file_stat", path)
            .mapCatching { ProtoJson.decodeFromString(FileStatRes.serializer(), it) }

    suspend fun thumbnail(path: String): Result<Bitmap?> =
        connectionManager.request("file_thumb", path)
            .mapCatching { ProtoJson.decodeFromString(FileChunkRes.serializer(), it) }
            .mapCatching { res ->
                if (!res.ok || res.dataBase64.isEmpty()) null
                else BitmapFactory.decodeByteArray(Base64.decode(res.dataBase64, Base64.DEFAULT), 0, Base64.decode(res.dataBase64, Base64.DEFAULT).size)
            }

    /**
     * Pulls a host file into the tablet cache (resumable: an existing partial
     * file continues from its current length).
     */
    suspend fun pullToCache(remotePath: String, onProgress: (done: Long, total: Long) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            val target = File(context.cacheDir, "shared/" + remotePath.substringAfterLast('/'))
            target.parentFile?.mkdirs()
            val total = stat(remotePath).getOrElse { return@withContext Result.failure(it) }.size
            var offset = if (target.exists()) target.length().coerceAtMost(total) else 0L
            runCatching {
                target.parentFile?.mkdirs()
                java.io.RandomAccessFile(target, "rw").use { raf ->
                    raf.seek(offset)
                    while (offset < total) {
                        val payload = ProtoJson.encodeToString(
                            FileChunkReq.serializer(),
                            FileChunkReq(path = remotePath, offset = offset, length = CHUNK),
                        )
                        val res = connectionManager.request("file_read", payload)
                            .mapCatching { ProtoJson.decodeFromString(FileChunkRes.serializer(), it) }
                            .getOrElse { error(it.message ?: "read failed") }
                        if (!res.ok) error(res.error)
                        if (res.dataBase64.isEmpty()) break
                        val bytes = Base64.decode(res.dataBase64, Base64.DEFAULT)
                        raf.write(bytes)
                        offset += bytes.size
                        onProgress(offset, total)
                    }
                }
                target
            }
        }

    /**
     * Pushes a local tablet file to the host (resumable: resumes from the host
     * partial file length when one exists).
     */
    suspend fun push(local: File, remotePath: String, onProgress: (done: Long, total: Long) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val total = local.length()
            val start = stat(remotePath).getOrNull()?.takeIf { it.ok }?.size?.coerceAtMost(total) ?: 0L
            runCatching {
                java.io.RandomAccessFile(local, "r").use { raf ->
                    raf.seek(start)
                    var offset = start
                    val buf = ByteArray(CHUNK)
                    while (offset < total) {
                        val read = raf.read(buf)
                        if (read < 0) break
                        val payload = ProtoJson.encodeToString(
                            FileChunkReq.serializer(),
                            FileChunkReq(path = remotePath, offset = offset, dataBase64 = Base64.encodeToString(buf, 0, read, Base64.NO_WRAP)),
                        )
                        val res = connectionManager.request("file_write", payload)
                            .mapCatching { ProtoJson.decodeFromString(FileChunkRes.serializer(), it) }
                            .getOrElse { error(it.message ?: "write failed") }
                        if (!res.ok) error(res.error)
                        offset = res.size
                        onProgress(offset, total)
                    }
                }
            }
        }
}
