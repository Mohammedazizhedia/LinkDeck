package com.linkdeck.app.ui.mirror

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ✅ أزرار التحكم (بدء/إيقاف) تم نقلها للأعلى
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (active) {
                    Button(onClick = vm::stopMirror, modifier = Modifier.weight(1f)) { 
                        Text(stringResource(R.string.mirror_stop)) 
                    }
                } else {
                    Button(onClick = vm::startMirror, modifier = Modifier.weight(1f)) { 
                        Text(stringResource(R.string.mirror_start)) 
                    }
                }
            }

            // شاشة العرض
            Box(
                Modifier.fillMaxWidth().weight(1f),
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
                                vm.tap(0.5f, 0.5f)
                            },
                        )
                        else -> Centered(stringResource(R.string.mirror_idle))
                    }
                }
            }

            // أزرار أندرويد (رجوع، هوم، إلخ)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = { vm.key("back") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.key_back)) }
                OutlinedButton(onClick = { vm.key("home") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.key_home)) }
                OutlinedButton(onClick = { vm.key("recents") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.key_recents)) }
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
