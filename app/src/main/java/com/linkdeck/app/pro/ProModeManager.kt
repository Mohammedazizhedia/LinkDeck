package com.linkdeck.app.pro

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.edit
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pro-mode state machine for the HOST.
 *
 * Honest note: a full scrcpy-style ADB client cannot ship inside one APK without
 * bundling the `adb` binary/dadb (large, OEM-dependent). This manager implements
 * the one-time wizard (Developer options → Wireless Debugging → pairing via the
 * settings notification captured by our NotificationListener), wires `WRITE_SECURE_SETTINGS`
 * once the shell grants it (Phase-4 Pro: user runs `adb tcpip` style step or a PC
 * one-liner), and always falls back to Normal mode (MediaProjection + Accessibility)
 * which works stock. Huawei fallback: `adb tcpip` via USB is offered when Wireless
 * Debugging is missing.
 */
@Singleton
class ProModeManager @Inject constructor(@ApplicationContext private val context: Context) {

    enum class Step { DISABLED, WIZARD_DEVELOPER, WIZARD_WIRELESS_DEBUG, PAIRING, ACTIVE, FALLBACK_NORMAL }

    private val prefs = context.getSharedPreferences("linkdeck_pro", Context.MODE_PRIVATE)

    private val _step = MutableStateFlow(currentStep())
    val step: StateFlow<Step> = _step

    fun currentStep(): Step = when {
        prefs.getBoolean("pro_active", false) -> Step.ACTIVE
        prefs.getBoolean("fallback_normal", false) -> Step.FALLBACK_NORMAL
        prefs.getBoolean("wd_enabled", false) -> Step.PAIRING
        else -> Step.WIZARD_DEVELOPER
    }

    fun openDeveloperSettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openWirelessDebuggingSettings() {
        // Exact ADB-WiFi settings screen (Android 11+); EMUI 12+ exposes the same entry.
        runCatching {
            context.startActivity(Intent("android.settings.ADB_WIFI_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    /** Called from the wizard when the user confirms Wireless Debugging is on. */
    fun markWirelessDebuggingEnabled() {
        prefs.edit { putBoolean("wd_enabled", true) }
        _step.value = Step.PAIRING
    }

    /** Captured by the NotificationListener when the pairing dialog posts (inline reply). */
    fun onPairingCodeCaptured(code: String) {
        prefs.edit { putString("last_pairing_code", code) }
    }

    fun lastPairingCode(): String = prefs.getString("last_pairing_code", "") ?: ""

    fun markPaired() {
        prefs.edit { putBoolean("pro_active", true) }
        _step.value = Step.ACTIVE
    }

    /** Huawei/older devices: offer one-time USB activation then continue wirelessly. */
    fun enableUsbFallback() {
        prefs.edit { putBoolean("fallback_normal", false); putBoolean("usb_fallback", true) }
    }

    fun useNormalModeFallback() {
        prefs.edit { putBoolean("fallback_normal", true) }
        _step.value = Step.FALLBACK_NORMAL
        HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"normal_mode"}""")
    }

    fun hasWriteSecureSettings(): Boolean = runCatching {
        context.contentResolver.call(
            android.net.Uri.parse("content://settings/system"), "GET_secure", "mock_location", null
        ) != null || context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
}
