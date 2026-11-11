package com.linkdeck.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
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
) {
    val vm: ControllerHomeViewModel = hiltViewModel()
    val connectionState by vm.connectionManager.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(Spacing.xxl)) {
        Text(stringResource(R.string.controller_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Spacing.sm))
        ConnectionStatusLine(state = connectionState)
        Spacer(Modifier.height(Spacing.xl))

        Button(onClick = onScanQr) { Text(stringResource(R.string.scan_qr)) }
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }
        
        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun ConnectionStatusLine(state: ConnectionState) {
    val (text, fg) = when (state) {
        ConnectionState.Disconnected -> stringResource(R.string.state_disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionState.Connecting -> stringResource(R.string.state_connecting) to LinkDeckTheme.tints.warningFg
        is ConnectionState.Connected -> stringResource(R.string.state_connected) to LinkDeckTheme.tints.successFg
        is ConnectionState.Error -> stringResource(R.string.state_error) to LinkDeckTheme.tints.dangerFg
    }
    when (state) {
        is ConnectionState.Connected -> StatusChips.Success(text)
        ConnectionState.Connecting -> StatusChips.Warning(text)
        is ConnectionState.Error -> StatusChips.Danger(text)
        ConnectionState.Disconnected -> StatusChip(text, MaterialTheme.colorScheme.surfaceVariant, fg)
    }
}
