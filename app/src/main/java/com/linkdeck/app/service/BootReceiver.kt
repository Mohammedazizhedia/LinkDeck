package com.linkdeck.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.linkdeck.app.settings.AppMode
import com.linkdeck.app.settings.SettingsStore

/** Restart the connection service after reboot so reconnects work unattended. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(SettingsStore.PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(SettingsStore.KEY_APP_MODE, null)?.let {
            runCatching { AppMode.valueOf(it) }.getOrNull()
        } ?: return // not configured yet — nothing to restart
        val service = Intent(context, LinkDeckService::class.java)
            .setAction(LinkDeckService.ACTION_START)
        runCatching { context.startForegroundService(service) }
    }
}
