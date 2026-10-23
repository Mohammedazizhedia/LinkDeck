package com.linkdeck.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.HostServer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HostHomeViewModel @Inject constructor(
    val pairingManager: PairingManager,
    val hostServer: HostServer,
    val connectionManager: ConnectionManager,
) : ViewModel()

/**
 * HOST (phone) home: big connection circle, Show QR, trusted controllers,
 * Pause/Disconnect, and the always-visible "being controlled" pill.
 */
@Composable
fun HostHomeScreen(
    onShowQr: () -> Unit,
    onOpenProWizard: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HostHomeViewModel = hiltViewModel()
    val hostEvent by vm.hostServer.events.collectAsState()
    val beingControlled = vm.hostServer.connectedCount > 0

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            // Connection circle
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = if (beingControlled) LinkDeckTheme.tints.dangerBg else LinkDeckTheme.tints.accentBg,
                    modifier = Modifier.size(160.dp),
                ) {}
                LineIconView(
                    icon = LineIcon.POWER,
                    tint = if (beingControlled) LinkDeckTheme.tints.dangerFg else LinkDeckTheme.tints.accentFg,
                    size = 64.dp,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (beingControlled) stringResource(R.string.host_sharing_active)
                else stringResource(R.string.host_waiting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.xxl))
            Button(onClick = {
                vm.pairingManager.hostStart()
                onShowQr()
            }) { Text(stringResource(R.string.show_qr)) }

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onOpenProWizard) { Text(stringResource(R.string.pro_title)) }

            Spacer(Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }
                OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
            }

            Spacer(Modifier.height(Spacing.xxl))
            Text(
                stringResource(R.string.trusted_devices),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            LazyColumn(Modifier.fillMaxWidth()) {
                // Host-side paired controller list is surfaced in Phase 3;
                // the count badge below reflects live sessions instead.
                item {
                    Text(
                        text = stringResource(R.string.host_controllers_hint, vm.hostServer.pairedDeviceIds().size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Floating "being controlled" pill with red dot + emergency disconnect.
        if (beingControlled) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(50),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.xl)
                    .fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(LinkDeckTheme.tints.dangerFg, CircleShape)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        stringResource(R.string.being_controlled),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        vm.hostServer.pairedDeviceIds().forEach { vm.hostServer.revokeDevice(it) }
                    }) { Text(stringResource(R.string.emergency_disconnect), color = LinkDeckTheme.tints.dangerFg) }
                }
            }
        }
    }
}
