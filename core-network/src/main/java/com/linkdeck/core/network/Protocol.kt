package com.linkdeck.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Payload encoded into the pairing QR code. Token expires 2 minutes after creation. */
@Serializable
data class PairingPayload(
    val v: Int = 1,
    val name: String,          // host device name shown on the tablet
    val model: String,         // host model (e.g. SM-S911B)
    val ip: String,
    val port: Int,
    val fp: String,            // SHA-256 fingerprint of the host TLS certificate
    val token: String,         // one-time pairing token (2 min validity)
    val exp: Long,             // epoch millis when the token expires
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now >= exp

    fun toQrContent(): String = Json.encodeToString(PairingPayload.serializer(), this)

    companion object {
        fun fromQrContent(content: String): PairingPayload? = runCatching {
            Json.decodeFromString(PairingPayload.serializer(), content)
        }.getOrNull()
    }
}

@Serializable
data class AuthMessage(val type: String = "auth", val token: String)

@Serializable
data class AuthOk(val type: String = "auth_ok", val deviceId: String, val deviceToken: String)

@Serializable
data class AuthError(val type: String = "auth_error", val reason: String)

@Serializable
data class Heartbeat(val type: String = "ping", val t: Long = System.currentTimeMillis())

/** A paired device persisted on the controller side. */
@Serializable
data class TrustedDevice(
    val id: String,
    val name: String,
    val model: String,
    val ip: String,
    val port: Int,
    val fingerprint: String,   // pinned cert fingerprint
    val token: String,         // long-lived reconnect token issued by the host
)
