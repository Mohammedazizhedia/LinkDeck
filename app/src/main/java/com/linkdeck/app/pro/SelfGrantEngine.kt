package com.linkdeck.app.pro

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses the app's own ADB shell (once Pro mode is paired) to grant what Android
 * allows without dialogs. Each step is reported so the UI can show a real
 * summary of what was granted vs. what still needs manual action.
 *
 * NOTE (honesty): without an actual shell connection these calls fail cleanly;
 * the summary distinguishes "granted" from "needs Pro shell". Runtime perms via
 * `pm grant` for SELF require shell — attempted through the paired channel.
 */
@Singleton
class SelfGrantEngine @Inject constructor(@ApplicationContext private val context: Context) {

    data class GrantResult(val label: String, val ok: Boolean, val detail: String)

    /** Steps that can run through WRITE_SECURE_SETTINGS directly on-device. */
    fun secureSettingsGrants(): List<GrantResult> {
        if (!canWriteSecure()) return listOf(
            GrantResult("WRITE_SECURE_SETTINGS", false, "requires_adb_once"),
        )
        val results = mutableListOf<GrantResult>()
        // Enable our accessibility service.
        runCatching {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val ours = "${context.packageName}/com.linkdeck.app.mirror.LinkDeckAccessibilityService"
            if (!enabled.split(':').contains(ours)) {
                Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    (enabled.split(':').filter { it.isNotBlank() } + ours).joinToString(":"))
            }
            results += GrantResult("accessibility_service", true, ours)
        }.onFailure { results += GrantResult("accessibility_service", false, it.message ?: "failed") }
        // Enable our notification listener.
        runCatching {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            val ours = "${context.packageName}/com.linkdeck.app.service.LinkDeckNotificationListener"
            if (!enabled.split(':').contains(ours)) {
                Settings.Secure.putString(context.contentResolver, "enabled_notification_listeners",
                    (enabled.split(':').filter { it.isNotBlank() } + ours).joinToString(":"))
            }
            results += GrantResult("notification_listener", true, ours)
        }.onFailure { results += GrantResult("notification_listener", false, it.message ?: "failed") }
        // Battery whitelist (deviceidle).
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                Settings.Global.putInt(context.contentResolver, "show_battery_optimizations_disabled", 1)
            }
            results += GrantResult("battery_whitelist", pm.isIgnoringBatteryOptimizations(context.packageName), "may_need_dialog")
        }
        return results
    }

    private fun canWriteSecure(): Boolean = runCatching {
        context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    companion object {
        /** Maps runtime perms to `pm grant`-able commands for the paired shell. */
        fun pmGrantCommands(packageName: String): List<String> = listOf(
            "pm grant $packageName android.permission.READ_CONTACTS",
            "pm grant $packageName android.permission.WRITE_CONTACTS",
            "pm grant $packageName android.permission.READ_SMS",
            "pm grant $packageName android.permission.SEND_SMS",
            "pm grant $packageName android.permission.RECEIVE_SMS",
            "pm grant $packageName android.permission.READ_PHONE_STATE",
            "pm grant $packageName android.permission.CALL_PHONE",
            "pm grant $packageName android.permission.ANSWER_PHONE_CALLS",
            "pm grant $packageName android.permission.RECORD_AUDIO",
            "pm grant $packageName android.permission.CAMERA",
            "pm grant $packageName android.permission.BLUETOOTH_CONNECT",
            "pm grant $packageName android.permission.POST_NOTIFICATIONS",
            "pm grant $packageName android.permission.WRITE_SETTINGS",
            "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS",
            "appops set $packageName MANAGE_EXTERNAL_STORAGE allow",
            "dumpsys deviceidle whitelist +$packageName",
        )
    }
}
