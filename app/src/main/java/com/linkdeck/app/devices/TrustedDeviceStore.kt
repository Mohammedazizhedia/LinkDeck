package com.linkdeck.app.devices

import android.content.Context
import androidx.core.content.edit
import com.linkdeck.core.network.TrustedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Persistence for paired devices (controller side) and paired controllers (host side). */
@Singleton
class TrustedDeviceStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("linkdeck_devices", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<TrustedDevice>> = _devices

    private fun load(): List<TrustedDevice> = runCatching {
        val raw = prefs.getString("devices", null) ?: return emptyList()
        json.decodeFromString(ListSerializer(TrustedDevice.serializer()), raw)
    }.getOrDefault(emptyList())

    private fun save(list: List<TrustedDevice>) {
        prefs.edit { putString("devices", json.encodeToString(ListSerializer(TrustedDevice.serializer()), list)) }
        _devices.value = list
    }

    fun upsert(device: TrustedDevice) {
        val next = (_devices.value.filterNot { it.id == device.id } + device).sortedBy { it.name.lowercase() }
        save(next)
    }

    fun remove(deviceId: String) = save(_devices.value.filterNot { it.id == deviceId })

    fun byId(id: String): TrustedDevice? = _devices.value.firstOrNull { it.id == id }
}
