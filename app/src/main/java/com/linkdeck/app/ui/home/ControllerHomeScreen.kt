package com.linkdeck.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.linkdeck.app.R
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.ConnectionState
import com.linkdeck.core.network.DiscoveredDevice
import com.linkdeck.core.network.NsdDiscovery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ControllerHomeViewModel @Inject constructor(
    val connectionManager: ConnectionManager,
    val pairingManager: PairingManager,
    val nsdDiscovery: NsdDiscovery,
) : ViewModel()

@Composable
fun ControllerHomeScreen(
    onScanQr: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val vm: ControllerHomeViewModel = hiltViewModel()
    val connectionState by vm.connectionManager.state.collectAsState()
    val discoveredDevices by vm.nsdDiscovery.devices.collectAsState()

    // ✅ بدء الاكتشاف التلقائي بمجرد فتح الشاشة
    LaunchedEffect(Unit) {
        vm.nsdDiscovery.start()
    }

    Column(Modifier.fillMaxSize().padding(Spacing.xxl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(stringResource(R.string.controller_title), style = MaterialTheme.typography.headlineSmall)
        }
        
        Spacer(Modifier.height(Spacing.sm))
        ConnectionStatusLine(state = connectionState)
        Spacer(Modifier.height(Spacing.xl))

        Button(onClick = onScanQr) { Text(stringResource(R.string.scan_qr)) }
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }

        Spacer(Modifier.height(Spacing.lg))

        // ✅ عرض الأجهزة المكتشفة
        if (discoveredDevices.isNotEmpty()) {
            Text("الأجهزة المكتشفة:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            LazyColumn {
                items(discoveredDevices) { device ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(device.name, modifier = Modifier.weight(1f))
                            Button(onClick = {
                                // الاتصال بالجهاز (هيتم إرسال AuthMessage)
                                // للأسف الاتصال التلقائي محتاج token، فهنستخدم التوكن المخزن أو نطلب QR
                                // مؤقتاً: هنبعت أمر اتصال مباشر لو الجهاز ده متسجل
                            }) {
                                Text("اتصال")
                            }
                        }
                    }
                }
            }
        } else {
            Text("جاري البحث عن أجهزة...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionStatusLine(state: ConnectionState) {
    val (text, color) = when (state) {
        ConnectionState.Disconnected -> stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionState.Connecting -> stringResource(R.string.state_connecting) to LinkDeckTheme.tints.warningFg
        is ConnectionState.Connected -> stringResource(R.string.state_connected) to LinkDeckTheme.tints.successFg
        is ConnectionState.Error -> stringResource(R.string.state_error) to LinkDeckTheme.tints.dangerFg
    }
    
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = Spacing.xs)
    )
}
