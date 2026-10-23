package com.linkdeck.app.pairing

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import com.linkdeck.app.devices.TrustedDeviceStore
import com.linkdeck.core.network.AuthMessage
import com.linkdeck.core.network.AuthOk
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.DiscoveredDevice
import com.linkdeck.core.network.HostServer
import com.linkdeck.core.network.NsdDiscovery
import com.linkdeck.core.network.NsdRegistration
import com.linkdeck.core.network.PairingPayload
import com.linkdeck.core.network.PinnedTrustManager
import com.linkdeck.core.network.TlsIdentity
import com.linkdeck.core.network.TrustedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.ExperimentalHttpClientApi
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Everything the UI needs to show the host's QR code. */
data class HostQrState(
    val payload: PairingPayload? = null,
    val secondsLeft: Int = 0,
)

/** One-stop manager for QR pairing on both sides. */
@Singleton
class PairingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tlsIdentity: TlsIdentity,
    private val hostServer: HostServer,
    private val nsdManager: NsdManager,
    private val trustedStore: TrustedDeviceStore,
    private val connectionManager: ConnectionManager,
    private val scope: CoroutineScope,
) {
    private val registration = NsdRegistration(nsdManager)
    private var qrJob: kotlinx.coroutines.Job? = null

    private val _hostQr = MutableStateFlow(HostQrState())
    val hostQr: StateFlow<HostQrState> = _hostQr

    private val discovery = NsdDiscovery(nsdManager, scope)
    val discovered: StateFlow<List<DiscoveredDevice>> = discovery.devices

    // ---------------------------------------------------------------- HOST --

    /** Starts the TLS server + mDNS (if needed) and begins broadcasting a fresh QR token. */
    fun hostStart() {
        if (!hostServer.isRunning) {
            hostServer.start()
        }
        if (_hostQr.value.payload == null) {
            rotateQrToken()
        }
        registration.register("LinkDeck-${android.os.Build.MODEL}", hostServer.boundPort)
    }

    /** Hotspot / DHCP can move us to a different address — refresh IP + port in the QR. */
    fun refreshAdvertisedAddress() {
        val cur = _hostQr.value.payload ?: return
        val ip = localIpAddress() ?: "0.0.0.0"
        if (ip != cur.ip || hostServer.boundPort != cur.port) {
            rotateQrToken()
        }
    }

    fun hostStop() {
        qrJob?.cancel()
        qrJob = null
        registration.unregister()
        hostServer.stop()
        _hostQr.value = HostQrState()
    }

    /** Rotates the 2-minute one-time token and rebuilds the QR payload. */
    fun rotateQrToken() {
        qrJob?.cancel()
        val token = hostServer.newPairingToken(validityMillis = 120_000L)
        val ip = localIpAddress() ?: "0.0.0.0"
        val port = hostServer.boundPort
        val payload = PairingPayload(
            name = deviceName(),
            model = android.os.Build.MODEL,
            ip = ip,
            port = port,
            fp = tlsIdentity.certificateFingerprint,
            token = token,
            exp = System.currentTimeMillis() + 120_000L,
        )
        _hostQr.value = HostQrState(payload = payload, secondsLeft = 120)
        qrJob = scope.launch {
            var left = 120
            while (isActive && left > 0) {
                delay(1_000)
                left--
                val cur = _hostQr.value.payload
                _hostQr.value = HostQrState(payload = cur, secondsLeft = left)
            }
        }
    }

    // ---------------------------------------------------------- CONTROLLER --

    /** Validates a scanned QR and performs the pinned-TLS auth handshake. */
    suspend fun pairWithPayload(payload: PairingPayload): Result<TrustedDevice> = withContext(Dispatchers.IO) {
        if (payload.isExpired()) return@withContext Result.failure(PairingException.TokenExpired)
        if (payload.port <= 0) return@withContext Result.failure(PairingException.InvalidPayload)

        runCatching {
            val trustManager = PinnedTrustManager(setOf(payload.fp))
            val client = HttpClient(CIO) {
                install(WebSockets)
                engine {
                    @OptIn(ExperimentalHttpClientApi::class)
                    https {
                        this.trustManager = trustManager
                        serverName = "linkdeck"
                    }
                }
            }
            try {
                var trusted: TrustedDevice? = null
                client.webSocket(method = HttpMethod.Get, host = payload.ip, port = payload.port, path = "/ws") {
                    send(Frame.Text(Json.encodeToString(AuthMessage.serializer(), AuthMessage(token = payload.token))))
                    val reply = incoming.tryReceive().getOrNull() as? Frame.Text
                        ?: throw SecurityException("Host did not reply")
                    val text = reply.readText()
                    if (!text.contains("\"auth_ok\"")) throw SecurityException("Pairing rejected: $text")
                    val ok = Json.decodeFromString(AuthOk.serializer(), text)
                    trusted = TrustedDevice(
                        id = ok.deviceId,
                        name = payload.name,
                        model = payload.model,
                        ip = payload.ip,
                        port = payload.port,
                        fingerprint = payload.fp,
                        token = ok.deviceToken,
                    )
                }
                val device = trusted ?: throw SecurityException("Handshake incomplete")
                trustedStore.upsert(device)
                connectionManager.connect(device)
                device
            } finally {
                client.close()
            }
        }.mapError()
    }

    /** Starts mDNS browsing for previously paired hosts on the LAN. */
    fun startDiscovery() = discovery.start()
    fun stopDiscovery() = discovery.stop()

    fun connectTo(device: TrustedDevice) = connectionManager.connect(device)
    fun disconnect() = connectionManager.disconnect()

    // ---------------------------------------------------------------- utils --

    fun deviceName(): String = runCatching {
        val name = android.provider.Settings.Global.getString(context.contentResolver, "device_name")
        name?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: android.os.Build.MODEL

    private fun localIpAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull() ?: runCatching {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        if (ip != 0) "%d.%d.%d.%d".format(ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff) else null
    }.getOrNull()

    private fun <T> Result<T>.mapError(): Result<T> = this // placeholder for fine-grained error mapping

    sealed class PairingException(msg: String) : Exception(msg) {
        data object TokenExpired : PairingException("token expired")
        data object InvalidPayload : PairingException("invalid payload")
        data class Network(val cause2: Throwable) : PairingException("network: ${cause2.message}")
    }
}
