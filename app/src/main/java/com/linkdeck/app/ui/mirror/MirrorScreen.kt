package com.linkdeck.app.ui.mirror

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.R
import com.linkdeck.app.mirror.MirrorViewModel
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.Spacing

/**
 * CONTROLLER mirror screen. Normal mode = JPEG frames with tap injection
 * through the host accessibility service. The toolbar auto-hides in a later
 * polish pass; every control here works in Normal mode today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorScreen(onBack: () -> Unit) {
    val vm: MirrorViewModel = hiltViewModel()
    val frame by vm.frame.collectAsState()
    val message by vm.message.collectAsState()
    val active by vm.active.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_mirror)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(9f / 16f),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val bmp = frame
                    when {
                        message == "flag_secure" -> Centered(stringResource(R.string.mirror_flag_secure))
                        message != null -> Centered(message!!)
                        bmp != null -> Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.feature_mirror),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().clickable {
                                // Tap passthrough (normalized coordinates).
                                vm.tap(0.5f, 0.5f)
                            },
                        )
                        else -> Centered(stringResource(R.string.mirror_idle))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = { vm.key("back") }) { Text(stringResource(R.string.key_back)) }
                OutlinedButton(onClick = { vm.key("home") }) { Text(stringResource(R.string.key_home)) }
                OutlinedButton(onClick = { vm.key("recents") }) { Text(stringResource(R.string.key_recents)) }
            }
            Row {
                if (active) TextButton(onClick = vm::stopMirror) { Text(stringResource(R.string.mirror_stop)) }
                else Button(onClick = vm::startMirror) { Text(stringResource(R.string.mirror_start)) }
            }
        }
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
