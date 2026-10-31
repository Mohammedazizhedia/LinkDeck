package com.linkdeck.core.network
import kotlinx.serialization.Serializable
@Serializable
data class P2pPair(val type: String = "p2p_pair", val code: String)
@Serializable
data class P2pAuthOk(
    val deviceId: String,
    val deviceToken: String,
    val fingerprint: String,
)
const val P2P_PORT = 28765
