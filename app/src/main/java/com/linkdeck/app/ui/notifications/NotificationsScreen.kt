package com.linkdeck.app.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.NotifPush

/** Notification mirror + two-way clipboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val vm: NotificationsViewModel = hiltViewModel()
    val items by vm.items.collectAsState()
    val hostClip by vm.hostClip.collectAsState()
    var replyTarget by remember { mutableStateOf<NotifPush?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_notifications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            // Two-way clipboard card
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp)) {
                Column(Modifier.padding(Spacing.xl).fillMaxWidth()) {
                    Text(stringResource(R.string.clipboard_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        if (hostClip.isBlank()) stringResource(R.string.clipboard_empty)
                        else hostClip.take(120),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    var outgoing by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = outgoing, onValueChange = { outgoing = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.clipboard_send_hint)) },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Button(onClick = { vm.sendClipboardToHost(outgoing); outgoing = "" }, enabled = outgoing.isNotBlank()) {
                        Text(stringResource(R.string.clipboard_send))
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.weight(1f)) {
                items(items, key = { it.key }) { n ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp)) {
                        Column(Modifier.padding(Spacing.xl).fillMaxWidth()) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(n.title.ifBlank { n.packageName }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.dismiss(n.key) }) { Text(stringResource(R.string.dismiss)) }
                            }
                            Text(n.text, style = MaterialTheme.typography.bodyMedium)
                            if (n.canReply) {
                                TextButton(onClick = { replyTarget = n }) { Text(stringResource(R.string.reply)) }
                            }
                        }
                    }
                }
            }
        }
    }

    replyTarget?.let { target ->
        var text by remember(target.key) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text(stringResource(R.string.reply_to, target.title)) },
            text = {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { vm.reply(target.key, text); replyTarget = null }) { Text(stringResource(R.string.send)) }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
