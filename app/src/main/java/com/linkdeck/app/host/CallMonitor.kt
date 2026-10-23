package com.linkdeck.app.host

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.linkdeck.core.network.HostBridge

/** HOST side: pushes call-state changes to controllers as "call_state" events. */
object CallMonitor {

    @Volatile private var registered = false

    private val legacyListener = object : android.telephony.PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) = push(state, phoneNumber)
    }

    private val modernCallback = object : android.telephony.TelephonyCallback(),
        android.telephony.TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = push(state, null)
    }

    fun start(context: Context) {
        if (registered) return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching { tm.registerTelephonyCallback(context.mainExecutor, modernCallback) }
        } else {
            @Suppress("DEPRECATION")
            tm.listen(legacyListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
        registered = true
    }

    fun stop(context: Context) {
        if (!registered) return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching { tm.unregisterTelephonyCallback(modernCallback) }
        } else {
            @Suppress("DEPRECATION")
            tm.listen(legacyListener, android.telephony.PhoneStateListener.LISTEN_NONE)
        }
        registered = false
    }

    private fun push(state: Int, number: String?) {
        val name = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> "ringing"
            TelephonyManager.CALL_STATE_OFFHOOK -> "offhook"
            else -> "idle"
        }
        val num = number?.replace("\"", "")?.let { "\"$it\"" } ?: "null"
        HostBridge.hostServer?.broadcast(
            "call_state",
            """{"state":"$name","number":$num}""",
        )
    }
}
