package com.linkdeck.app.ui.pairing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.R
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.PairingPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object NeedsCameraPermission : ScanUiState
    data object Scanning : ScanUiState
    data object Pairing : ScanUiState
    data class Done(val deviceName: String) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

@HiltViewModel
class ScanQrViewModel @Inject constructor(val pairingManager: PairingManager) : ViewModel() {
    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.NeedsCameraPermission)
    val state: StateFlow<ScanUiState> = _state

    fun onPermissionResult(granted: Boolean) {
        _state.value = if (granted) ScanUiState.Scanning else ScanUiState.NeedsCameraPermission
    }

    fun onScanned(content: String) {
        if (_state.value != ScanUiState.Scanning) return
        // Silently ignore anything that is not a LinkDeck payload — keep scanning.
        if (!content.contains("\"token\"")) return
        val payload = PairingPayload.fromQrContent(content)
        if (payload == null) {
            _state.value = ScanUiState.Error("scanned: ${content.take(120)}")
            return
        }
        if (payload.isExpired()) {
            _state.value = ScanUiState.Error("token_expired")
            return
        }
        _state.value = ScanUiState.Pairing
        viewModelScope.launch {
            pairingManager.pairWithPayload(payload)
                .onSuccess { device ->
                    _state.value = ScanUiState.Done(device.name)
                }
                .onFailure { e ->
                    _state.value = ScanUiState.Error(e.message ?: "pairing_failed")
                }
        }
    }

    fun retry() { _state.value = ScanUiState.Scanning }
}

/** CONTROLLER side: live camera preview scanning the host's QR, then pairing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(onBack: () -> Unit) {
    val vm: ScanQrViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> vm.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title_scan)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ScanUiState.NeedsCameraPermission -> {
                    Column(
                        Modifier.fillMaxSize().padding(Spacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.perm_camera_why), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.xl))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.perm_grant))
                        }
                    }
                }
                ScanUiState.Scanning -> {
                    Text(
                        stringResource(R.string.qr_scan_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Spacing.xl),
                    )
                    CameraQrView(onCode = vm::onScanned, modifier = Modifier.weight(1f))
                }
                ScanUiState.Pairing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                            Spacer(Modifier.height(Spacing.lg))
                            Text(stringResource(R.string.pairing_in_progress), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is ScanUiState.Done -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.pairing_success, (state as ScanUiState.Done).deviceName),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            Button(onClick = onBack) { Text(stringResource(R.string.ok)) }
                        }
                    }
                }
                is ScanUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.pairing_failed),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                (state as ScanUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            Button(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraQrView(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var bound by remember { mutableStateOf(false) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
    ) { previewView ->
        if (bound) return@AndroidView
        bound = true
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, QrAnalyzer(onCode)) }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, executor)
    }
}
