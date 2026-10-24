package com.linkdeck.app.host

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.SmsManager
import com.linkdeck.app.service.LinkDeckNotificationListener
import com.linkdeck.core.network.CmdReq
import com.linkdeck.core.network.CmdRes
import com.linkdeck.core.network.HostBridge
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class StatusInfo(
    val battery: Int,
    val charging: Boolean,
    val storageTotal: Long,
    val storageFree: Long,
    val wifiSsid: String,
    val volume: Int,
    val brightness: Int,
)

@Serializable
data class SimpleItem(val id: String, val title: String, val subtitle: String)

/** HOST-side command executor. Impossible-on-stock actions return a clear error. */
object HostCommands {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun execute(context: Context, payload: String): String {
        val req = runCatching { json.decodeFromString(CmdReq.serializer(), payload) }.getOrNull()
            ?: return res(CmdRes(false, error = "bad_request"))
        return res(runCmd(context, req.cmd, req.argsJson))
    }

    private fun res(r: CmdRes) = json.encodeToString(CmdRes.serializer(), r)

    private fun runCmd(context: Context, cmd: String, args: String): CmdRes = when (cmd) {
        "get_status" -> getStatus(context)
        "set_clipboard" -> {
            HostClipboardHolder.set(context, arg(args, "text"))
            CmdRes(true)
        }
        "sms_list" -> smsList(context)
        "sms_send" -> smsSend(context, arg(args, "to"), arg(args, "body"))
        "contacts_list" -> contactsList(context)
        "apps_list" -> appsList(context)
        "launch_app" -> launchApp(context, arg(args, "package"))
        "dial" -> {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + arg(args, "number")))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            CmdRes(true)
        }
        "answer_call" -> answerCall(context)
        "end_call" -> endCall(context)
        "notif_dismiss" -> {
            (HostBridge.notificationListener as? LinkDeckNotificationListener)?.dismiss(arg(args, "key"))
            CmdRes(true)
        }
        "notif_reply" -> {
            val ok = (HostBridge.notificationListener as? LinkDeckNotificationListener)
                ?.reply(arg(args, "key"), arg(args, "text")) == true
            CmdRes(ok, error = if (ok) "" else "reply_unavailable")
        }
        "notif_snapshot" -> {
            val items = (HostBridge.notificationListener as? LinkDeckNotificationListener)?.snapshot().orEmpty()
            CmdRes(true, json.encodeToString(ListSerializer(NotifLite.serializer()), items.map { it.toLite() }))
        }
        "set_volume" -> {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val level = arg(args, "level").toIntOrNull() ?: return CmdRes(false, error = "bad_args")
            am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                level.coerceIn(0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),
                0,
            )
            CmdRes(true)
        }
        "set_brightness" -> {
            val v = arg(args, "level").toIntOrNull() ?: return CmdRes(false, error = "bad_args")
            if (!Settings.System.canWrite(context)) return CmdRes(false, error = "need_write_settings")
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v.coerceIn(0, 255))
            CmdRes(true)
        }
        "wake" -> wake(context)
        "lock" -> lock(context)
        "inject_key" -> injectKey(context, arg(args, "key"))
        "inject_tap" -> injectTap(
            context,
            arg(args, "x").toFloatOrNull() ?: 0f,
            arg(args, "y").toFloatOrNull() ?: 0f,
        )
        "mirror_request" -> {
            com.linkdeck.app.mirror.MirrorConsentActivity.request(context)
            CmdRes(true)
        }
        "mirror_stop" -> {
            com.linkdeck.app.mirror.ScreenCaptureService.stop(context)
            CmdRes(true)
        }
        else -> CmdRes(false, error = "unknown_cmd")
    }

    @Serializable
    private data class NotifLite(val key: String, val packageName: String, val title: String, val text: String, val whenMs: Long, val canReply: Boolean)

    private fun com.linkdeck.core.network.NotifPush.toLite() =
        NotifLite(key, packageName, title, text, whenMs, canReply)

    private fun arg(args: String, key: String): String = runCatching {
        val el = json.parseToJsonElement(args)
        if (el is kotlinx.serialization.json.JsonObject) {
            el[key]?.let { v -> if (v is kotlinx.serialization.json.JsonPrimitive) v.content else "" } ?: ""
        } else ""
    }.getOrDefault("")

    private fun getStatus(context: Context): CmdRes {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra("level", -1) ?: -1
        val status = intent?.getIntExtra("status", -1) ?: -1
        val storage = runCatching {
            val s = android.os.StatFs(Environment.getDataDirectory().path)
            s.totalBytes to s.availableBytes
        }.getOrDefault(0L to 0L)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val brightness = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(-1)
        val wifi = runCatching {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            wm.connectionInfo.ssid?.removePrefix("\"")?.removeSuffix("\"")
        }.getOrDefault("")
        return CmdRes(
            true,
            json.encodeToString(
                StatusInfo.serializer(),
                StatusInfo(
                    battery = level,
                    charging = status == 2 || status == 5,
                    storageTotal = storage.first,
                    storageFree = storage.second,
                    wifiSsid = wifi,
                    volume = am.getStreamVolume(AudioManager.STREAM_MUSIC),
                    brightness = brightness,
                ),
            ),
        )
    }

    private fun smsList(context: Context): CmdRes = runCatching {
        val items = mutableListOf<SimpleItem>()
        context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("_id", "address", "body", "date"),
            null, null, "date DESC LIMIT 100",
        )?.use { c ->
            while (c.moveToNext()) {
                items.add(SimpleItem(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        }
        CmdRes(true, json.encodeToString(ListSerializer(SimpleItem.serializer()), items))
    }.getOrElse { CmdRes(false, error = it.message ?: "sms_denied") }

    private fun smsSend(context: Context, to: String, body: String): CmdRes = runCatching {
        val sm = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION") SmsManager.getDefault()
        }
        sm?.sendTextMessage(to, null, body, null, null)
        CmdRes(true)
    }.getOrElse { CmdRes(false, error = it.message ?: "sms_failed") }

    private fun contactsList(context: Context): CmdRes = runCatching {
        val items = mutableListOf<SimpleItem>()
        context.contentResolver.query(
            Uri.parse("content://com.android.contacts/contacts"),
            arrayOf("_id", "display_name", "display_name"),
            null, null, "display_name ASC LIMIT 200",
        )?.use { c ->
            while (c.moveToNext()) {
                items.add(SimpleItem(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        }
        CmdRes(true, json.encodeToString(ListSerializer(SimpleItem.serializer()), items))
    }.getOrElse { CmdRes(false, error = it.message ?: "contacts_denied") }

    private fun appsList(context: Context): CmdRes = runCatching {
        val pm = context.packageManager
        val items = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { SimpleItem(it.packageName, pm.getApplicationLabel(it).toString(), it.packageName) }
            .sortedBy { it.title.lowercase() }
        CmdRes(true, json.encodeToString(ListSerializer(SimpleItem.serializer()), items))
    }.getOrElse { CmdRes(false, error = it.message ?: "apps_failed") }

    private fun launchApp(context: Context, pkg: String): CmdRes {
        return runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: error("no_launcher")
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (se: SecurityException) {
                // Android 10+ background-start restriction: fall back to a heads-up.
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val pi = android.app.PendingIntent.getActivity(
                    context, pkg.hashCode(), intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                val notif = android.app.Notification.Builder(context, "linkdeck_service")
                    .setSmallIcon(com.linkdeck.app.R.drawable.ic_stat_linkdeck)
                    .setContentTitle(context.getString(com.linkdeck.app.R.string.launch_fallback_title))
                    .setContentText(intent.`package`.let { context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(pkg, 0)).toString() })
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
                nm.notify(pkg.hashCode(), notif)
            }
            CmdRes(true)
        }.getOrElse { CmdRes(false, error = it.message ?: "launch_failed") }
    }

    private fun answerCall(context: Context): CmdRes = runCatching {
        val tm = context.getSystemService(TelecomManager::class.java)
        tm?.acceptRingingCall()
        CmdRes(true)
    }.getOrElse { CmdRes(false, error = it.message ?: "answer_failed") }

    private fun endCall(context: Context): CmdRes = runCatching {
        val tm = context.getSystemService(TelecomManager::class.java)
        tm?.endCall()
        CmdRes(true)
    }.getOrElse { CmdRes(false, error = "end_call_needs_default_dialer") }

    private fun injectKey(context: Context, key: String): CmdRes {
        val action = when (key) {
            "back" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
            "home" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
            "recents" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
            else -> return CmdRes(false, error = "bad_key")
        }
        val svc = HostBridge.accessibilityService as? android.accessibilityservice.AccessibilityService
            ?: return CmdRes(false, error = "accessibility_off")
        return CmdRes(svc.performGlobalAction(action))
    }

    private fun injectTap(context: Context, x: Float, y: Float): CmdRes {
        val svc = HostBridge.accessibilityService as? android.accessibilityservice.AccessibilityService
            ?: return CmdRes(false, error = "accessibility_off")
        val path = android.graphics.Path().apply { moveTo(x, y) }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return CmdRes(svc.dispatchGesture(gesture, null, null))
    }

    private fun wake(context: Context): CmdRes = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (pm.isInteractive) return CmdRes(true)
        CmdRes(false, error = "wake_needs_pro_or_proximity")
    }.getOrElse { CmdRes(false, error = it.message ?: "wake_failed") }

    private fun lock(context: Context): CmdRes = runCatching {
        val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        if (dm.isAdminActive(android.content.ComponentName(context, DeviceAdminStub::class.java))) {
            dm.lockNow()
            CmdRes(true)
        } else {
            CmdRes(false, error = "lock_needs_device_admin")
        }
    }.getOrElse { CmdRes(false, error = it.message ?: "lock_failed") }

    class DeviceAdminStub : android.app.admin.DeviceAdminReceiver()
}

/** Holder so HostCommands can reach a clipboard monitor without Hilt. */
object HostClipboardHolder {
    @Volatile private var monitor: HostClipboardMonitor? = null
    fun set(context: Context, text: String) {
        var m = monitor
        if (m == null) { m = HostClipboardMonitor(context.applicationContext); monitor = m }
        m.setText(text)
    }
}
