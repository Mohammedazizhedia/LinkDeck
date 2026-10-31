package com.linkdeck.app.host
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
class HostHotspotManager(private val context: Context) {
    interface Callback {
        fun onStarted(info: String)
        fun onError(reason: String)
    }
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private var callback: Callback? = null
    fun start(callback: Callback) {
        this.callback = callback
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        runCatching {
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res
                    val info = runCatching {
                        if (Build.VERSION.SDK_INT >= 30) {
                            val cfg = res.softApConfiguration
                            val ssid = cfg?.ssid?.toString().orEmpty()
                            val pass = cfg?.passphrase.orEmpty()
                            if (ssid.isNotBlank()) "Hotspot: $ssid  |  Password: $pass"
                            else "Hotspot started — see the system notification for its name and password."
                        } else {
                            "Hotspot started — see the system notification for its name and password."
                        }
                    }.getOrDefault("Hotspot started.")
                    callback.onStarted(info)
                }
                override fun onStopped() {
                    reservation = null
                }
                override fun onFailed(reason: Int) {
                    callback.onError("hotspot failed (code $reason)")
                }
            }, Handler(Looper.getMainLooper()))
        }.onFailure { callback.onError(it.message ?: "hotspot unsupported on this device") }
    }
    fun stop() {
        runCatching { reservation?.close() }
        reservation = null
    }
}
