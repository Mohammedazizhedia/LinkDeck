package com.linkdeck.app.ui.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.CmdReq
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.Envelope
import com.linkdeck.core.network.EventPush
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CallUi(val state: String, val number: String?)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    private val _call = MutableStateFlow(CallUi("idle", null))
    val call: StateFlow<CallUi> = _call

    init {
        viewModelScope.launch {
            connectionManager.events.collect { raw ->
                val env = runCatching { ProtoJson.decodeFromString(Envelope.serializer(), raw) }.getOrNull() ?: return@collect
                if (env.type == "call_state") {
                    runCatching { ProtoJson.decodeFromString(EventPush.serializer(), env.payload) }
                        .onSuccess { ev ->
                            val num = ev.dataJson.removeSurrounding("\"").takeIf { it.isNotBlank() && it != "{}" }
                            _call.value = CallUi(ev.kind, num)
                        }
                }
            }
        }
    }

    fun answer() = cmd("answer_call")
    fun reject() = cmd("end_call")
    fun dial(number: String) = cmd("dial", """{"number":"$number"}""")

    private fun cmd(name: String, args: String = "{}") = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(CmdReq.serializer(), CmdReq(name, args)))
    }
}

/** Incoming-call UI on the tablet + dialer. Audio stays on the phone (Bluetooth HFP). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(onBack: () -> Unit) {
    val vm: CallsViewModel = hiltViewModel()
    val call by vm.call.collectAsState()
    var number by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_calls)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xxl), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            if (call.state == "ringing") {
                Text(
                    stringResource(R.string.call_incoming, call.number ?: ""),
                    style = MaterialTheme.typography.headlineSmall,
                    color = LinkDeckTheme.tints.successFg,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    Button(onClick = vm::answer) { Text(stringResource(R.string.call_answer)) }
                    OutlinedButton(onClick = vm::reject) { Text(stringResource(R.string.call_reject)) }
                }
                Text(
                    stringResource(R.string.call_reject_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    if (call.state == "offhook") stringResource(R.string.call_active)
                    else stringResource(R.string.call_idle),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = number, onValueChange = { number = it },
                label = { Text(stringResource(R.string.call_dial_hint)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Button(onClick = { vm.dial(number) }, enabled = number.isNotBlank()) {
                Text(stringResource(R.string.call_dial))
            }
        }
    }
}
