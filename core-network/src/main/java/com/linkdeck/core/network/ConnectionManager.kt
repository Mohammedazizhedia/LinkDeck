package com.linkdeck.core.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val device: TrustedDevice) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

/** CONTROLLER-side connection manager: pinned-TLS WebSocket (wss://). */
class ConnectionManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val events: SharedFlow<String> = _events

    private val _beingControlled = MutableStateFlow(false)
    val beingControlled: StateFlow<Boolean> = _beingControlled

    @Volatile private var currentDevice: TrustedDevice? = null
    @Volatile private var session: DefaultClientWebSocketSession? = null
    @Volatile private var userDisconnected = false
    @Volatile private var reconnectAttempts = 0

    private val nextId = AtomicLong(1)
    private val pending = ConcurrentHashMap<Long, CompletableDeferred<String>>()

    fun setControlled(active: Boolean) { _beingControlled.value = active }

    fun connect(device: TrustedDevice) {
        currentDevice = device
        userDisconnected = false
        if (_state.value is ConnectionState.Connected) return
        scope.launch { connectWithBackoff(device) }
    }

    fun disconnect() {
        userDisconnected = true
        pending.values.forEach { it.completeExceptionally(ConnectionClosedException) }
        pending.clear()
        scope.launch { runCatching { session?.close() } }
        session = null
        _state.value = ConnectionState.Disconnected
    }

    fun onNetworkChanged() {
        val device = currentDevice ?: return
        if (_state.value is ConnectionState.Disconnected && !userDisconnected) {
            scope.launch { connectWithBackoff(device) }
        }
    }

    suspend fun send(type: String, payloadJson: String = "{}"): Boolean {
        val msg = ProtoJson.encodeToString(Envelope.serializer(), Envelope(0, type, payloadJson))
        val s = session ?: return false
        return runCatching { s.send(Frame.Text(msg)); true }.getOrDefault(false)
    }

    suspend fun request(type: String, payloadJson: String = "{}"): Result<String> {
        val id = nextId.getAndIncrement()
        val deferred = CompletableDeferred<String>()
        pending[id] = deferred
        val msg = ProtoJson.encodeToString(Envelope.serializer(), Envelope(id, type, payloadJson))
        val s = session ?: run { pending.remove(id); return Result.failure(ConnectionClosedException) }
        return runCatching {
            s.send(Frame.Text(msg))
            deferred.await()
        }.also { if (it.isFailure) pending.remove(id) }
    }

    private suspend fun connectWithBackoff(device: TrustedDevice) {
        while (!userDisconnected && currentDevice?.id == device.id) {
            _state.value = ConnectionState.Connecting
            val ok = runCatching { openSession(device) }.isSuccess
            if (ok) return
            reconnectAttempts++
            if (reconnectAttempts >= 8) {
                DebugLog.log("Conn", "gave up connecting to ${device.ip}:${device.port}")
                _state.value = ConnectionState.Error("unreachable")
                return
            }
            val backoffMs = min(30_000L, (1000L * 2.0.pow(reconnectAttempts)).toLong())
            delay(backoffMs)
        }
    }

    private fun pinnedClient(trustManager: PinnedTrustManager): HttpClient {
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }
        return HttpClient(OkHttp) {
            install(WebSockets)
            engine {
                config {
                    sslSocketFactory(sslContext.socketFactory, trustManager)
                    hostnameVerifier { _, _ -> true }
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }
    }

    private suspend fun openSession(device: TrustedDevice) {
        val trustManager = PinnedTrustManager(setOf(device.fingerprint))
        val client = pinnedClient(trustManager)
        try {
            client.webSocket(urlString = "wss://${device.ip}:${device.port}/ws") {
                session = this
                send(Frame.Text(ProtoJson.encodeToString(AuthMessage.serializer(), AuthMessage(token = device.token))))
                val reply = incoming.receive() as? Frame.Text
                    ?: throw SecurityException("No auth reply")
                val text = reply.readText()
                if (!text.contains("\\"auth_ok\\"")) {
                    DebugLog.log("Conn", "auth rejected: " + text.take(120))
                    throw SecurityException("Auth rejected by host")
                }
                DebugLog.log("Conn", "connected to ${device.ip}:${device.port}")

                _state.value = ConnectionState.Connected(device)
                reconnectAttempts = 0

                val heartbeat = scope.launch {
                    while (isActive) {
                        delay(15_000)
                        runCatching {
                            send(Frame.Text(ProtoJson.encodeToString(Heartbeat.serializer(), Heartbeat())))
                        }
                    }
                }
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> dispatchIncoming(frame.readText())
                            else -> Unit
                        }
                    }
                } finally {
                    heartbeat.cancel()
                    session = null
                    pending.values.forEach { it.completeExceptionally(ConnectionClosedException) }
                    pending.clear()
                    if (!userDisconnected && currentDevice?.id == device.id) {
                        _state.value = ConnectionState.Disconnected
                        connectWithBackoff(device)
                    } else {
                        _state.value = ConnectionState.Disconnected
                    }
                }
            }
        } finally {
            client.close()
        }
    }

    private fun dispatchIncoming(text: String) {
        val env = runCatching { ProtoJson.decodeFromString(Envelope.serializer(), text) }.getOrNull()
        if (env != null && env.id != 0L) {
            pending.remove(env.id)?.complete(env.payload)
        } else {
            _events.tryEmit(text)
        }
    }

    object ConnectionClosedException : Exception("connection closed")
}
