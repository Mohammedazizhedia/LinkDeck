package com.linkdeck.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.R
import com.linkdeck.app.devices.TrustedDeviceStore
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.ConnectionState

private data class RailEntry(val route: String, val icon: LineIcon, val labelRes: Int)

private val RAIL = listOf(
    RailEntry("devices", LineIcon.DEVICES, R.string.nav_devices),
    RailEntry("mirror", LineIcon.MIRROR, R.string.feature_mirror),
    RailEntry("files", LineIcon.FILES, R.string.feature_files),
    RailEntry("notifications", LineIcon.NOTIFICATIONS, R.string.feature_notifications),
    RailEntry("messages", LineIcon.MESSAGES, R.string.feature_messages),
    RailEntry("calls", LineIcon.CALLS, R.string.feature_calls),
    RailEntry("apps", LineIcon.APPS, R.string.feature_apps),
    RailEntry("settings", LineIcon.SETTINGS, R.string.settings),
)

/**
 * CONTROLLER (tablet) shell: navigation rail (right side under RTL), adaptive
 * layout, connection summary, and quick access to pairing.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ControllerHomeScreen(
    onScanQr: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
        ?: return Box(Modifier.fillMaxSize())
    val windowSize = calculateWindowSizeClass(activity)
    val isExpanded = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded

    Row(Modifier.fillMaxSize()) {
        // Under RTL layout direction the rail is laid out from the right edge.
        val context = LocalContext.current
        NavigationRail {
            FeatureCatalog.visible(context).map { RailEntry(it.id, it.icon, it.labelRes) }.forEach { entry ->
                NavigationRailItem(
                    selected = false,
                    onClick = { onNavigate(entry.route) },
                    icon = { LineIconView(icon = entry.icon, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp) },
                    label = { Text(stringResource(entry.labelRes), style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        Column(Modifier.weight(1f)) {
            ControllerHomeContent(onScanQr = onScanQr, onOpenPermissions = onOpenPermissions)
            VoiceFab(onRoute = onNavigate)
        }
    }
}

@Composable
private fun ControllerHomeContent(
    onScanQr: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: ControllerHomeViewModel = hiltViewModel()
    val devices by vm.trustedStore.devices.collectAsState()
    val connectionState by vm.connectionManager.state.collectAsState()

    Column(modifier.fillMaxSize().padding(Spacing.xxl)) {
        Text(stringResource(R.string.controller_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Spacing.sm))
        ConnectionStatusLine(state = connectionState)
        Spacer(Modifier.height(Spacing.xl))

        Button(onClick = onScanQr) { Text(stringResource(R.string.scan_qr)) }
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions)) }

        Spacer(Modifier.height(Spacing.xxl))
        Text(stringResource(R.string.trusted_devices), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        if (devices.isEmpty()) {
            Text(
                stringResource(R.string.empty_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(devices, key = { it.id }) { device ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    ) {
                        Row(
                            Modifier.padding(Spacing.xl),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LineIconView(icon = LineIcon.DEVICES, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(device.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = Spacing.sm)) {
            when (state) {
                is ConnectionState.Connected -> com.linkdeck.core.design.components.StatusChips.Success(text)
                ConnectionState.Connecting -> com.linkdeck.core.design.components.StatusChips.Warning(text)
                is ConnectionState.Error -> com.linkdeck.core.design.components.StatusChips.Danger(text)
                ConnectionState.Disconnected -> com.linkdeck.core.design.components.StatusChip(
                    text, MaterialTheme.colorScheme.surfaceVariant, fg,
                )
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ControllerHomeViewModel @javax.inject.Inject constructor(
    val trustedStore: TrustedDeviceStore,
    val connectionManager: ConnectionManager,
    val pairingManager: PairingManager,
    val voice: com.linkdeck.app.voice.VoiceCommandManager,
) : androidx.lifecycle.ViewModel()


@Composable
private fun VoiceFab(onRoute: (String) -> Unit) {
    val vm: ControllerHomeViewModel = hiltViewModel()
    val state by vm.voice.state.collectAsState()
    Box(Modifier.padding(Spacing.xl)) {
        Button(onClick = {
            val lang = java.util.Locale.getDefault().language.take(2).ifBlank { "en" }
            vm.voice.ensureModelAndStart(lang, onRoute)
        }) {
            Text(
                when (state) {
                    is com.linkdeck.app.voice.VoiceCommandManager.VoiceState.Downloading ->
                        stringResource(R.string.voice_download)
                    com.linkdeck.app.voice.VoiceCommandManager.VoiceState.Listening ->
                        stringResource(R.string.voice_listening)
                    else -> stringResource(R.string.voice_start)
                }
            )
        }
    }
}
