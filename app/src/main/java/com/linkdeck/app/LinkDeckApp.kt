package com.linkdeck.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.linkdeck.app.host.HostCommands
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
    @Inject lateinit var proManager: com.linkdeck.app.pro.ProModeManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        HostBridge.hostServer = hostServer
        HostBridge.commandHandler = { payload -> HostCommands.execute(this, payload) }
        HostBridge.proManager = proManager
        // React to Wi-Fi changes: drop dead connections and let ConnectionManager
        // re-establish them (paired devices re-resolve over mDNS in later phases).
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = connectionManager.onNetworkChanged()
            override fun onLost(network: Network) = connectionManager.onNetworkChanged()
        })
    }
}
