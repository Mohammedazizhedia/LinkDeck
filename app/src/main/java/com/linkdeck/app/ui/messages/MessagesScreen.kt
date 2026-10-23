package com.linkdeck.app.ui.messages

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing

/** SMS inbox + contacts from the paired phone, with sending. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(onBack: () -> Unit) {
    val vm: MessagesViewModel = hiltViewModel()
    val sms by vm.sms.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_messages)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text(stringResource(R.string.tab_sms)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text(stringResource(R.string.tab_contacts)) }
            }
            Column(Modifier.fillMaxSize().padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                var to by remember { mutableStateOf("") }
                var body by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = to, onValueChange = { to = it },
                    label = { Text(stringResource(R.string.sms_to)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text(stringResource(R.string.sms_body)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    Button(onClick = { vm.send(to, body); body = "" }, enabled = to.isNotBlank() && body.isNotBlank() && !sending) {
                        Text(stringResource(R.string.send))
                    }
                    if (tab == 1) {
                        Spacer(Modifier.padding(Spacing.sm))
                        TextButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.retry)) }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                    if (tab == 0) {
                        items(sms, key = { it.id }) { m ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp)) {
                                Column(Modifier.padding(Spacing.lg).fillMaxWidth()) {
                                    Text(m.address, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(m.body, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        items(contacts, key = { it.id }) { c ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp),
                                onClick = { to = c.phone.ifBlank { c.name }; tab = 0 },
                            ) {
                                Row(Modifier.padding(Spacing.lg).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(c.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
