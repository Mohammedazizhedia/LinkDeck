package com.linkdeck.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON protocol over the pinned-TLS WebSocket (single channel, id-routed). */
val ProtoJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class Envelope(val id: Long = 0, val type: String, val payload: String = "{}")

// ---- files ----
@Serializable
data class FileEntry(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    val mime: String = "",
)

@Serializable
data class FileListReq(val path: String, val query: String = "")

@Serializable
data class FileListRes(val ok: Boolean, val entries: List<FileEntry> = emptyList(), val error: String = "")

@Serializable
data class FileChunkReq(val path: String, val offset: Long = 0, val length: Int = 0, val dataBase64: String = "")

@Serializable
data class FileChunkRes(
    val ok: Boolean,
    val size: Long = -1,
    val dataBase64: String = "",
    val error: String = "",
)

@Serializable
data class FileStatRes(
    val ok: Boolean,
    val size: Long = -1,
    val mime: String = "",
    val modified: Long = 0,
    val isDir: Boolean = false,
    val error: String = "",
)

// ---- file transfer (Task 2.1) ----
@Serializable
data class FileTransferInitReq(
    val transferId: String,
    val path: String,
    val totalSize: Long,
    val sha256: String,
)

@Serializable
data class FileTransferInitRes(
    val ok: Boolean,
    val resumeOffset: Long = 0L,
    val error: String = "",
)

@Serializable
data class FileTransferChunkReq(
    val transferId: String,
    val path: String,
    val offset: Long,
    val dataBase64: String,
    val chunkSha256: String,
)

@Serializable
data class FileTransferChunkRes(
    val ok: Boolean,
    val received: Long,
    val error: String = "",
)

@Serializable
data class FileTransferCancelReq(val transferId: String, val reason: String = "")

@Serializable
data class FileTransferCancelRes(val ok: Boolean, val error: String = "")

// ---- generic host command (status, sms, contacts, apps, calls, clipboard...) ----
@Serializable
data class CmdReq(val cmd: String, val argsJson: String = "{}")

@Serializable
data class CmdRes(val ok: Boolean, val resultJson: String = "", val error: String = "")

// ---- pushes (host -> controllers) ----
@Serializable
data class NotifPush(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val whenMs: Long,
    val canReply: Boolean = false,
)

@Serializable
data class NotifDismissPush(val key: String)

@Serializable
data class ClipPush(val text: String)

/** Normal-mode mirror: JPEG frames over the control channel (fallback for Pro mode). */
@Serializable
data class FramePush(val width: Int, val height: Int, val dataBase64: String)

/** Generic host event (call state, battery, etc.). */
@Serializable
data class EventPush(val kind: String, val dataJson: String = "{}")

/** Static bridge so non-Hilt components (listener/accessibility/widget) reach the host server. */
object HostBridge {
    @Volatile var hostServer: HostServer? = null
    @Volatile var commandHandler: ((String) -> String?)? = null
    @Volatile var notificationListener: Any? = null
    @Volatile var accessibilityService: Any? = null
    @Volatile var proManager: Any? = null
}
