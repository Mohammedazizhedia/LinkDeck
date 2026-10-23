package com.linkdeck.core.network

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** mDNS service type for LinkDeck devices on the local network. */
const val SERVICE_TYPE = "_linkdeck._tcp."

data class DiscoveredDevice(val name: String, val host: String, val port: Int)

/** Registers this host on mDNS so paired tablets can find it after the first pairing. */
class NsdRegistration(private val nsdManager: NsdManager) {
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun register(serviceName: String, port: Int) {
        unregister()
        val info = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        runCatching { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    fun unregister() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
    }
}

/**
 * mDNS browser used by the CONTROLLER. Emits discovered (already-paired) hosts
 * and keeps the list fresh as devices appear/disappear from the LAN.
 */
class NsdDiscovery(
    private val nsdManager: NsdManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices

    private val found = ConcurrentHashMap<String, DiscoveredDevice>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val host = serviceInfo.host?.hostAddress ?: return
            found[serviceInfo.serviceName] = DiscoveredDevice(serviceInfo.serviceName, host, serviceInfo.port)
            _devices.value = found.values.sortedBy { it.name }
        }
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    fun start() {
        stop()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                runCatching { nsdManager.resolveService(service, resolveListener) }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                found.remove(service.serviceName)
                _devices.value = found.values.sortedBy { it.name }
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { stop() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        discoveryListener = listener
        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    fun stop() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        found.clear()
        _devices.value = emptyList()
    }
}
