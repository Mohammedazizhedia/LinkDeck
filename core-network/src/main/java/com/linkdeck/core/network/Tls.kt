package com.linkdeck.core.network

import android.content.Context
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Host-side TLS identity: a self-signed certificate generated once on the phone,
 * pinned on the tablet during QR pairing via its SHA-256 fingerprint.
 */
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

        /** Load an existing identity or generate a fresh self-signed certificate. */
        suspend fun loadOrCreate(context: Context): TlsIdentity = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "tls").apply { mkdirs() }
            val ksFile = File(dir, "linkdeck.jks")
            val password = "linkdeck-selfsigned" // local-only keystore passphrase
            if (ksFile.exists()) {
                val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    ksFile.inputStream().use { load(it, password.toCharArray()) }
                }
                val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
                return@withContext TlsIdentity(ks, password, fingerprintOf(cert))
            }

            // Generate a long-lived self-signed cert (valid 10 years).
            val ks: KeyStore = io.ktor.network.tls.certificates.generateCertificate(
                file = ksFile,
                algorithm = "SHA256withRSA",
                keyAlias = KEY_ALIAS,
                keyPassword = password,
                jksPassword = password,
                keySizeInBits = 2048,
                subject = X500Principal("CN=LinkDeck"),
                validDays = 3650,
            )
            val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
            TlsIdentity(ks, password, fingerprintOf(cert))
        }
    }
}

/**
 * Client-side trust manager: only certificates whose SHA-256 fingerprint was
 * pinned during pairing are accepted. Everything else is rejected.
 */
class PinnedTrustManager(private val pinnedFingerprints: Set<String>) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        val fp = leaf.encoded.let { digest ->
            MessageDigest.getInstance("SHA-256").digest(digest).joinToString(":") { "%02X".format(it) }
        }
        if (fp !in pinnedFingerprints) {
            throw CertificateException("Certificate pin mismatch — possible MITM, connection aborted")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        throw CertificateException("Client authentication is not used in this protocol")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
