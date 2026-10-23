package com.linkdeck.core.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Base64
import java.io.File
import java.io.RandomAccessFile
import java.net.URLConnection
import kotlin.math.max

/**
 * HOST-side filesystem operations executed when a controller requests them.
 * Every path is canonicalized and confined to shared storage.
 */
object HostFileOps {

    private const val CHUNK_CAP = 256 * 1024
    private const val SEARCH_CAP = 500
    private const val SEARCH_MAX_DEPTH = 12

    private fun root(): File = Environment.getExternalStorageDirectory()

    /** Canonical path inside external storage, or null if the path escapes. */
    private fun resolve(path: String): File? = runCatching {
        val r = root().canonicalFile
        val f = File(r, path.removePrefix("/")).canonicalFile
        if (f.path == r.path || f.path.startsWith(r.path + File.separator)) f else null
    }.getOrNull()

    fun mimeOf(file: File): String =
        URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"

    private fun toEntry(f: File): FileEntry = FileEntry(
        name = f.name,
        path = f.path.removePrefix(root().path).ifEmpty { "/" },
        isDir = f.isDirectory,
        size = if (f.isFile) f.length() else 0L,
        modified = f.lastModified(),
        mime = if (f.isFile) mimeOf(f) else "",
    )

    fun list(path: String, query: String): FileListRes {
        val dir = resolve(path) ?: return FileListRes(false, error = "access_denied")
        if (!dir.isDirectory) return FileListRes(false, error = "not_a_directory")
        return runCatching {
            val trimmed = query.trim()
            val entries = if (trimmed.isEmpty()) {
                dir.listFiles()?.map(::toEntry)
                    ?.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
            } else {
                search(dir, trimmed.lowercase(), 0, mutableListOf())
            }
            FileListRes(true, entries = entries ?: emptyList())
        }.getOrElse { FileListRes(false, error = it.message ?: "io_error") }
    }

    private fun search(dir: File, needle: String, depth: Int, out: MutableList<FileEntry>): List<FileEntry> {
        if (depth > SEARCH_MAX_DEPTH || out.size >= SEARCH_CAP) return out
        dir.listFiles()?.forEach { f ->
            if (out.size >= SEARCH_CAP) return out
            if (f.name.startsWith(".")) return@forEach
            if (f.name.lowercase().contains(needle)) out.add(toEntry(f))
            if (f.isDirectory) search(f, needle, depth + 1, out)
        }
        return out
    }

    fun stat(path: String): FileStatRes {
        val f = resolve(path) ?: return FileStatRes(false, error = "access_denied")
        return runCatching {
            FileStatRes(
                ok = true,
                size = if (f.isFile) f.length() else 0L,
                mime = if (f.isFile) mimeOf(f) else "",
                modified = f.lastModified(),
                isDir = f.isDirectory,
            )
        }.getOrElse { FileStatRes(false, error = it.message ?: "io_error") }
    }

    fun readChunk(path: String, offset: Long, length: Int): FileChunkRes {
        val f = resolve(path) ?: return FileChunkRes(false, error = "access_denied")
        if (!f.isFile) return FileChunkRes(false, error = "not_a_file")
        return runCatching {
            RandomAccessFile(f, "r").use { raf ->
                raf.seek(offset)
                val buf = ByteArray(length.coerceIn(1, CHUNK_CAP))
                val read = raf.read(buf)
                if (read < 0) FileChunkRes(true, size = f.length(), dataBase64 = "")
                else FileChunkRes(true, size = f.length(), dataBase64 = Base64.encodeToString(buf, 0, read, Base64.NO_WRAP))
            }
        }.getOrElse { FileChunkRes(false, error = it.message ?: "io_error") }
    }

    fun writeChunk(path: String, offset: Long, dataBase64: String): FileChunkRes {
        val f = resolve(path) ?: return FileChunkRes(false, error = "access_denied")
        return runCatching {
            f.parentFile?.mkdirs()
            val data = Base64.decode(dataBase64, Base64.DEFAULT)
            RandomAccessFile(f, "rw").use { raf ->
                raf.seek(offset)
                raf.write(data)
                FileChunkRes(true, size = raf.length())
            }
        }.getOrElse { FileChunkRes(false, error = it.message ?: "io_error") }
    }

    fun thumbnail(path: String, maxDim: Int = 256): FileChunkRes {
        val f = resolve(path) ?: return FileChunkRes(false, error = "access_denied")
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, opts)
            var sample = 1
            while (max(opts.outWidth, opts.outHeight) / (sample * 2) > maxDim) sample *= 2
            val bmp: Bitmap = BitmapFactory.decodeFile(
                f.path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return FileChunkRes(false, error = "decode_failed")
            val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                val ratio = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
            } else bmp
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            if (scaled != bmp) bmp.recycle()
            scaled.recycle()
            FileChunkRes(true, size = f.length(), dataBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
        }.getOrElse { FileChunkRes(false, error = it.message ?: "io_error") }
    }
}
