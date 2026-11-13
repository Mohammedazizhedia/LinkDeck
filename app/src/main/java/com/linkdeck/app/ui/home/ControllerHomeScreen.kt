package com.linkdeck.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ControllerHomeViewModel @Inject constructor(
    val connectionManager: ConnectionManager,
    val pairingManager: PairingManager,
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
        
        Spacer(Modifier.height(Spacing.xxl))

        // ✅ الأزرار ظاهرة دايماً للتجربة
        Text("الوظائف المتاحة", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.md))
        
        val features = listOf(
            "Mirror" to "mirror",
            "Files" to "files",
            "Notifications" to "notifications",
            "Messages" to "messages",
            "Calls" to "calls",
            "Apps" to "apps"
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(features) { (label, route) ->
                OutlinedButton(
                    onClick = { onNavigate(route) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(label)
                }
            }
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
