package com.linkdeck.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.NotifDismissPush
import com.linkdeck.core.network.NotifPush
import com.linkdeck.core.network.ProtoJson

/**
 * HOST side: mirrors posted notifications to connected controllers, keeps the
 * latest snapshot for the tablet list, and executes dismiss/reply commands.
 * Enabled explicitly by the user from Settings → Accessibility-style screens.
 */
class LinkDeckNotificationListener : NotificationListenerService() {

    private val latest = LinkedHashMap<String, StatusBarNotification>()

    override fun onListenerConnected() {
        HostBridge.notificationListener = this
    }

    override fun onListenerDisconnected() {
        if (HostBridge.notificationListener === this) HostBridge.notificationListener = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing && sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // Pro pairing: capture the 6-digit code from the wireless-debugging notification.
        if (sbn.packageName == "android") {
            val t = sbn.notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val m = Regex("\\b(\\d{6})\\b").find(t)
            if (m != null && t.contains("pair", ignoreCase = true)) {
                (com.linkdeck.core.network.HostBridge.proManager as? com.linkdeck.app.pro.ProModeManager)
                    ?.onPairingCodeCaptured(m.groupValues[1])
            }
        }
        latest[sbn.key] = sbn
        trim()
        push(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        latest.remove(sbn.key)
        HostBridge.hostServer?.broadcast(
            "notif_removed",
            ProtoJson.encodeToString(NotifDismissPush.serializer(), NotifDismissPush(sbn.key)),
        )
    }

    fun dismiss(key: String) = runCatching { cancelNotification(key) }

    /** Sends a reply through the notification's RemoteInput when available. */
    fun reply(key: String, text: String): Boolean {
        val sbn = latest[key] ?: return false
        val act = sbn.notification.actions?.firstOrNull { it.remoteInputs != null } ?: return false
        return runCatching {
            val fillIn = android.content.Intent()
            val results = Bundle().apply { putCharSequence(act.remoteInputs!![0].resultKey, text) }
            RemoteInput.addResultsToBundle(act.remoteInputs!!, fillIn, results)
            // PendingIntent mutability: addResultsToBundle requires a mutable target.
            act.actionIntent.send(this, 0, fillIn)
            true
        }.getOrDefault(false)
    }

    private fun push(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val push = NotifPush(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
            whenMs = sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime,
            canReply = sbn.notification.actions?.any { it.remoteInputs != null } == true,
        )
        HostBridge.hostServer?.broadcast("notif", ProtoJson.encodeToString(NotifPush.serializer(), push))
    }

    fun snapshot(): List<NotifPush> = latest.values.map { sbn ->
        val extras = sbn.notification.extras
        NotifPush(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
            whenMs = sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime,
            canReply = sbn.notification.actions?.any { it.remoteInputs != null } == true,
        )
    }

    private fun trim() { while (latest.size > 50) latest.remove(latest.keys.first()) }
}
