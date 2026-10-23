package com.linkdeck.app.ui.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.linkdeck.app.R
import com.linkdeck.app.pairing.HostQrState
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.app.pairing.rememberQrBitmap
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShowQrViewModel @Inject constructor(val pairingManager: PairingManager) : ViewModel()

/**
 * HOST side of first pairing: shows the QR carrying IP, TLS fingerprint and a
 * 2-minute one-time token. Auto-refreshes the token on expiry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrScreen(onBack: () -> Unit) {
    val vm: ShowQrViewModel = hiltViewModel()
    val state by vm.pairingManager.hostQr.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state.secondsLeft) {
        if (state.secondsLeft <= 0) vm.pairingManager.rotateQrToken()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title_host)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val payload = state.payload
            if (payload == null) {
                CircularProgressIndicator()
                return@Column
            }
            val bitmap = rememberQrBitmap(payload.toQrContent())
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(280.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.qr_title_host),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.qr_expires_in, state.secondsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.secondsLeft < 20) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = payload.ip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            Button(onClick = { vm.pairingManager.rotateQrToken() }) {
                Text(stringResource(R.string.qr_regenerate))
            }
        }
    }
}
