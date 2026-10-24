package com.linkdeck.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.linkdeck.app.host.HostCommands
import com.linkdeck.app.pro.ProModeManager
import com.linkdeck.app.settings.LocaleHelper
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.HostServer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LinkDeckApp : Application() {

    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var hostServer: HostServer
    @Inject lateinit var proManager: ProModeManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        installCrashTrap()
        HostBridge.hostServer = hostServer
        HostBridge.commandHandler = { payload -> HostCommands.execute(this, payload) }
        HostBridge.proManager = proManager
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = connectionManager.onNetworkChanged()
            override fun onLost(network: Network) = connectionManager.onNetworkChanged()
        })
    }

    /** Saves any crash stacktrace to crash.log so it can be reported. */
    private fun installCrashTrap() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                openFileOutput("crash.log", Context.MODE_PRIVATE).bufferedWriter().use {
                    it.write(sw.toString())
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun readCrashLog(context: Context): String? = runCatching {
            val f = java.io.File(context.filesDir, "crash.log")
            if (f.exists() && f.length() > 0) f.readText() else null
        }.getOrNull()

        fun clearCrashLog(context: Context) = runCatching {
            java.io.File(context.filesDir, "crash.log").delete()
        }
    }
}
