package com.linkdeck.app.pairing
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class P2pManager(private val context: Context) {
    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val _devices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val devices: StateFlow<List<WifiP2pDevice>> = _devices
    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo
    private var receiver: BroadcastReceiver? = null
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        stopDiscovery()
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION ->
                        manager.requestPeers(channel) { peers ->
                            _devices.value = peers.deviceList.toList()
                        }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION ->
                        manager.requestConnectionInfo(channel) { info ->
                            _connectionInfo.value = info
                        }
                }
            }
        }
        runCatching { context.registerReceiver(receiver, filter) }
        manager.discoverPeers(channel, silentListener())
    }
    fun stopDiscovery() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
    }
    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager.connect(channel, config, silentListener())
    }
    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager.createGroup(channel, silentListener())
    }
    private fun silentListener() = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {}
        override fun onFailure(reason: Int) {}
    }
}
