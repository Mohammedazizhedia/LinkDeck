package com.linkdeck.app.host
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.linkdeck.core.network.ClipPush
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.ProtoJson
class HostClipboardMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var last: String? = null
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val text = cm.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(appContext)
            ?.toString()
        if (text != null && text != last) {
            last = text
            HostBridge.hostServer?.broadcast(
                "clip",
                ProtoJson.encodeToString(ClipPush.serializer(), ClipPush(text)),
            )
        }
    }
    fun start() { last = null; cm.addPrimaryClipChangedListener(listener) }
    fun stop() { cm.removePrimaryClipChangedListener(listener) }
    fun setText(text: String) {
        last = text
        cm.setPrimaryClip(ClipData.newPlainText("linkdeck", text))
    }
}
