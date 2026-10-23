package com.linkdeck.app.ui.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.linkdeck.app.R
import com.linkdeck.app.devices.TrustedDeviceStore
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.TrustedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    val trustedStore: TrustedDeviceStore,
    val connectionManager: ConnectionManager,
) : ViewModel() {
    fun connect(device: TrustedDevice) = connectionManager.connect(device)
    fun disconnect() = connectionManager.disconnect()
    fun revoke(device: TrustedDevice) {
        connectionManager.disconnect()
        trustedStore.remove(device.id)
    }
}

/**
 * Tablet device list: every paired phone with name, model, and connection
 * state, plus one-tap connect/disconnect and revoke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(onBack: () -> Unit) {
    val vm: DevicesViewModel = hiltViewModel()
    val devices by vm.trustedStore.devices.collectAsState()
    val connectionState by vm.connectionManager.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_devices)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl)) {
            if (devices.isEmpty()) {
                Text(
                    stringResource(R.string.empty_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn {
                items(devices, key = { it.id }) { device ->
                    val connected = (connectionState as? com.linkdeck.core.network.ConnectionState.Connected)?.device?.id == device.id
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    ) {
                        Row(
                            Modifier.padding(Spacing.xl),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LineIconView(icon = LineIcon.DEVICES, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 32.dp)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    device.model,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (connected) {
                                TextButton(onClick = { vm.disconnect() }) { Text(stringResource(R.string.disconnect)) }
                            } else {
                                Button(onClick = { vm.connect(device) }) { Text(stringResource(R.string.connect)) }
                            }
                            TextButton(onClick = { vm.revoke(device) }) {
                                Text(stringResource(R.string.revoke), color = LinkDeckTheme.tints.dangerFg)
                            }
                        }
                    }
                }
            }
        }
    }
}
