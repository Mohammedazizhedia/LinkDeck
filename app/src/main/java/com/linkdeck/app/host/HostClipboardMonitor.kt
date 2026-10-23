package com.linkdeck.app.host

import android.content.ClipboardManager
import android.content.Context
import com.linkdeck.core.network.ClipPush
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.ProtoJson

/** HOST side: forwards clipboard text changes to controllers (text only; images Phase 3+). */
class HostClipboardMonitor(context: Context) {
    private val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var last: String? = null
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(cm)?.toString()
        if (text != null && text != last) {
            last = text
            HostBridge.hostServer?.broadcast("clip", ProtoJson.encodeToString(ClipPush.serializer(), ClipPush(text)))
        }
    }
    fun start() { last = null; cm.addPrimaryClipChangedListener(listener) }
    fun stop() { cm.removePrimaryClipChangedListener(listener) }
    fun setText(text: String) {
        last = text
        cm.setPrimaryClip(android.content.ClipData.newPlainText("linkdeck", text))
    }
}
