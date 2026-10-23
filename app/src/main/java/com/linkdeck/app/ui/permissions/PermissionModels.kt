package com.linkdeck.app.ui.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.linkdeck.app.R

/** Special (settings-screen) permissions vs standard runtime permissions. */
enum class SpecialPermission { NONE, ALL_FILES, NOTIF_LISTENER, ACCESSIBILITY, BATTERY }

data class PermissionDef(
    val key: String,
    val titleRes: Int,
    val whyRes: Int,
    val runtimePermissions: List<String> = emptyList(),
    val special: SpecialPermission = SpecialPermission.NONE,
    /** Min SDK where this permission exists (older devices auto-grant). */
    val minSdk: Int = 1,
)

/** The full consent-first permission catalogue. Nothing is requested silently. */
object PermissionCatalog {
    val ALL = listOf(
        PermissionDef("files", R.string.perm_files, R.string.perm_files_why,
            special = SpecialPermission.ALL_FILES),
        PermissionDef("notifications", R.string.perm_notifications, R.string.perm_notifications_why,
            runtimePermissions = if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()),
        PermissionDef("notif_listener", R.string.perm_notif_listener, R.string.perm_notif_listener_why,
            special = SpecialPermission.NOTIF_LISTENER),
        PermissionDef("accessibility", R.string.perm_accessibility, R.string.perm_accessibility_why,
            special = SpecialPermission.ACCESSIBILITY),
        PermissionDef("bluetooth", R.string.perm_bluetooth, R.string.perm_bluetooth_why,
            runtimePermissions = buildList {
                if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            }),
        PermissionDef("contacts", R.string.perm_contacts, R.string.perm_contacts_why,
            runtimePermissions = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)),
        PermissionDef("sms", R.string.perm_sms, R.string.perm_sms_why,
            runtimePermissions = listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)),
        PermissionDef("phone", R.string.perm_phone, R.string.perm_phone_why,
            runtimePermissions = listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS)),
        PermissionDef("microphone", R.string.perm_microphone, R.string.perm_microphone_why,
            runtimePermissions = listOf(Manifest.permission.RECORD_AUDIO)),
        PermissionDef("camera", R.string.perm_camera, R.string.perm_camera_why,
            runtimePermissions = listOf(Manifest.permission.CAMERA)),
        PermissionDef("battery", R.string.perm_battery, R.string.perm_battery_why,
            special = SpecialPermission.BATTERY),
    )
}

enum class PermState { GRANTED, DENIED, PERMANENTLY_DENIED, NOT_APPLICABLE }

object PermissionChecker {

    fun stateOf(context: Context, def: PermissionDef): PermState {
        if (Build.VERSION.SDK_INT < def.minSdk) return PermState.NOT_APPLICABLE

        if (def.special != SpecialPermission.NONE) {
            return if (isSpecialGranted(context, def.special)) PermState.GRANTED else PermState.DENIED
        }
        if (def.runtimePermissions.isEmpty()) return PermState.GRANTED

        val denied = def.runtimePermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (denied.isEmpty()) return PermState.GRANTED

        // A permanent denial is possible only via the standard dialog, which the
        // center always uses. Any regular denial here may still be re-askable once.
        return PermState.DENIED
    }

    private fun isSpecialGranted(context: Context, special: SpecialPermission): Boolean = when (special) {
        SpecialPermission.ALL_FILES ->
            Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()
        SpecialPermission.NOTIF_LISTENER ->
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        SpecialPermission.ACCESSIBILITY -> isAccessibilityEnabled(context)
        SpecialPermission.BATTERY -> {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        SpecialPermission.NONE -> true
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals("${context.packageName}/com.linkdeck.app.service.LinkDeckAccessibilityService", ignoreCase = true) }
    }

    /** The exact settings screen for a special permission. */
    fun specialIntent(context: Context, special: SpecialPermission): Intent = when (special) {
        SpecialPermission.ALL_FILES -> Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        SpecialPermission.NOTIF_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        SpecialPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        SpecialPermission.BATTERY -> Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        SpecialPermission.NONE -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
