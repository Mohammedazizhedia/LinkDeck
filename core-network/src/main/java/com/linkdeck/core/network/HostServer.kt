package com.linkdeck.core.network

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HostEvent {
    data class ControllerConnected(val deviceId: String) : HostEvent
    data class ControllerDisconnected(val deviceId: String) : HostEvent
}

/** TLS WebSocket server on the HOST (Netty engine). */
class HostServer(
    private val identity: TlsIdentity,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var server: ApplicationEngine? = null

    private val _events = MutableStateFlow<HostEvent?>(null)
    val events: StateFlow<HostEvent?> = _events

    @Volatile private var qrToken: String? = null
    @Volatile private var qrTokenExpiresAt: Long = 0L

    private val deviceTokens = ConcurrentHashMap<String, String>()
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    @Volatile
    var commandHandler: ((String) -> String?)? = null

    @Volatile
    var boundPort: Int = -1
        private set

    val connectedCount: Int get() = sessions.size
    val isRunning: Boolean get() = server != null

    fun newPairingToken(validityMillis: Long = 120_000L): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        qrToken = token
        qrTokenExpiresAt = System.currentTimeMillis() + validityMillis
        return token
    }

    fun isQrTokenValid(token: String): Boolean {
        val t = qrToken ?: return false
        return t == token && System.currentTimeMillis() < qrTokenExpiresAt
    }

    fun pairedDeviceIds(): Set<String> = deviceTokens.keys.toSet()

    fun revokeDevice(deviceId: String) {
        deviceTokens.remove(deviceId)
        sessions.remove(deviceId)?.let { s -> scope.launch { runCatching { s.close() } } }
    }

    fun start(): Int {
        check(server == null) { "HostServer already started" }
        val port = findFreePort()

        val environment = applicationEngineEnvironment {
            sslConnector(
                keyStore = identity.keyStore,
                keyAlias = TlsIdentity.KEY_ALIAS,
                keyStorePassword = { identity.password.toCharArray() },
                privateKeyPassword = { identity.password.toCharArray() },
            ) {
                host = "0.0.0.0"
                this.port = port
            }
            module { appModule() }
        }

        val engine = embeddedServer(Netty, environment)
        server = engine
        engine.start(wait = false)
        boundPort = port
        DebugLog.log("Host", "server started on port $boundPort")
        return boundPort
    }

    private fun Application.appModule() {
        install(ContentNegotiation) { json() }
        install(WebSockets)
        routing {
            get("/ping") { call.respondText("linkdeck-ok") }
            webSocket("/ws") {
                try {
                    val deviceId = authenticate() ?: return@webSocket
                    sessions[deviceId] = this
                    _events.value = HostEvent.ControllerConnected(deviceId)
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> handleText(deviceId, frame.readText())
                                else -> Unit
                            }
                        }
                    } finally {
                        sessions.remove(deviceId)
                        _events.value = HostEvent.ControllerDisconnected(deviceId)
                    }
                } catch (t: Throwable) {
                    DebugLog.log("Host", "ws error: ${t.javaClass.simpleName}: ${t.message}")
                    runCatching {
                        send(Frame.Text("{\\"type\\":\\"auth_error\\",\\"reason\\":\\"server:" + t.javaClass.simpleName + ":" + (t.message ?: "").take(120) + "\\"}"))
                    }
                    runCatching { close() }
                }
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.authenticate(): String? {
        val first = incoming.receive() as? Frame.Text ?: return reject("no auth frame")
        val auth = runCatching {
            kotlinx.serialization.json.Json.decodeFromString<AuthMessage>(first.readText())
        }.getOrNull() ?: return reject("malformed auth")
        DebugLog.log("Host", "auth attempt token=${auth.token.take(10)} valid=${isQrTokenValid(auth.token)}")
        val deviceId = UUID.randomUUID().toString().replace("-", "")

        return when {
            isQrTokenValid(auth.token) -> {
                qrToken = null
                val deviceToken = UUID.randomUUID().toString().replace("-", "")
                deviceTokens[deviceId] = deviceToken
                send(Frame.Text(kotlinx.serialization.json.Json.encodeToString(AuthOk.serializer(), AuthOk(deviceId = deviceId, deviceToken = deviceToken))))
                DebugLog.log("Host", "pairing OK device=$deviceId")
                deviceId
            }
            deviceTokens.entries.firstOrNull { it.value == auth.token } != null -> {
                val knownId = deviceTokens.entries.first { it.value == auth.token }.key
                send(Frame.Text(kotlinx.serialization.json.Json.encodeToString(AuthOk.serializer(), AuthOk(deviceId = knownId, deviceToken = auth.token))))
                knownId
            }
            else -> reject("invalid or expired token")
        }
    }

    private suspend fun DefaultWebSocketServerSession.reject(reason: String): String? {
        send(Frame.Text(kotlinx.serialization.json.Json.encodeToString(AuthError.serializer(), AuthError(reason = reason))))
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, reason))
        return null
    }

    private fun handleText(deviceId: String, text: String) {
        if (text.contains("\\"ping\\"")) {
            scope.launch { runCatching { sessions[deviceId]?.send(Frame.Text(text)) } }
            return
        }
        val env = runCatching {
            kotlinx.serialization.json.Json.decodeFromString(Envelope.serializer(), text)
        }.getOrNull() ?: return

        when (env.type) {
            "file_list" -> {
                val req = runCatching {
                    kotlinx.serialization.json.Json.decodeFromString(FileListReq.serializer(), env.payload)
                }.getOrNull() ?: return
                reply(deviceId, env.id, "file_list_res",
                    kotlinx.serialization.json.Json.encodeToString(FileListRes.serializer(), HostFileOps.list(req.path, req.query)))
            }
            "file_stat" -> reply(deviceId, env.id, "file_stat_res",
                kotlinx.serialization.json.Json.encodeToString(FileStatRes.serializer(), HostFileOps.stat(env.payload)))
            "file_read" -> {
                val req = runCatching {
                    kotlinx.serialization.json.Json.decodeFromString(FileChunkReq.serializer(), env.payload)
                }.getOrNull() ?: return
                reply(deviceId, env.id, "file_read_res",
                    kotlinx.serialization.json.Json.encodeToString(FileChunkRes.serializer(), HostFileOps.readChunk(req.path, req.offset, req.length)))
            }
            "file_write" -> {
                val req = runCatching {
                    kotlinx.serialization.json.Json.decodeFromString(FileChunkReq.serializer(), env.payload)
                }.getOrNull() ?: return
                reply(deviceId, env.id, "file_write_res",
                    kotlinx.serialization.json.Json.encodeToString(FileChunkRes.serializer(), HostFileOps.writeChunk(req.path, req.offset, req.dataBase64)))
            }
            "file_thumb" -> reply(deviceId, env.id, "file_thumb_res",
                kotlinx.serialization.json.Json.encodeToString(FileChunkRes.serializer(), HostFileOps.thumbnail(env.payload)))
            "cmd" -> {
                val payload = HostBridge.commandHandler?.invoke(env.payload)
                    ?: kotlinx.serialization.json.Json.encodeToString(CmdRes.serializer(), CmdRes(false, error = "host_busy"))
                reply(deviceId, env.id, "cmd_res", payload)
            }
            else -> Unit
        }
    }

    private fun reply(deviceId: String, id: Long, type: String, payload: String) {
        val msg = runCatching {
            kotlinx.serialization.json.Json.encodeToString(Envelope.serializer(), Envelope(id, type, payload))
        }.getOrNull() ?: return
        scope.launch { runCatching { sessions[deviceId]?.send(Frame.Text(msg)) } }
    }

    fun broadcast(type: String, payloadJson: String) {
        val msg = runCatching {
            kotlinx.serialization.json.Json.encodeToString(Envelope.serializer(), Envelope(0, type, payloadJson))
        }.getOrNull() ?: return
        scope.launch {
            sessions.values.forEach { runCatching { it.send(Frame.Text(msg)) } }
        }
    }

    fun stop() {
        server?.let { runCatching { it.stop(500L, 1500L) } }
        server = null
        boundPort = -1
        sessions.clear()
    }

    private fun findFreePort(): Int {
        for (attempt in 0 until 20) {
            val candidate = 28000 + Random.nextInt(0, 20000)
            runCatching { ServerSocket(candidate).use { return candidate } }
        }
        return ServerSocket(0).use { it.localPort }
    }
}
