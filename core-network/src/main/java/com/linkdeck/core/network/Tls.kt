package com.linkdeck.core.network

import android.content.Context
import io.ktor.network.tls.certificates.buildKeyStore
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Host-side TLS identity: self-signed cert generated once, pinned via SHA-256 during QR pairing. */
class TlsIdentity(
    val keyStore: KeyStore,
    val password: String,
    val certificateFingerprint: String,
) {
    companion object {
        const val KEY_ALIAS = "linkdeck"

        private fun fingerprintOf(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
            return digest.joinToString(":") { "%02X".format(it) }
        }

        suspend fun loadOrCreate(context: Context): TlsIdentity = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "tls").apply { mkdirs() }
            val ksFile = File(dir, "linkdeck.jks")
            val storePass = "linkdeck-selfsigned"
            if (ksFile.exists()) {
                val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    ksFile.inputStream().use { load(it, storePass.toCharArray()) }
                }
                val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
                return@withContext TlsIdentity(ks, storePass, fingerprintOf(cert))
            }
            val ks = buildKeyStore {
                certificate(KEY_ALIAS) {
                    password = storePass
                    domains = listOf("linkdeck", "localhost", "127.0.0.1")
                    daysValid = 3650
                    keySizeInBits = 2048
                }
            }
            ks.store(ksFile.outputStream(), storePass.toCharArray())
            val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
            TlsIdentity(ks, storePass, fingerprintOf(cert))
        }
    }
}

class PinnedTrustManager(private val pinnedFingerprints: Set<String>) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        val fp = leaf.encoded.let { digest ->
            MessageDigest.getInstance("SHA-256").digest(digest).joinToString(":") { "%02X".format(it) }
        }
        if (fp !in pinnedFingerprints) {
            throw CertificateException("Certificate pin mismatch - possible MITM, connection aborted")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        throw CertificateException("Client authentication is not used in this protocol")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
