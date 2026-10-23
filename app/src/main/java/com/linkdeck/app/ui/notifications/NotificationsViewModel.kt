package com.linkdeck.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.Envelope
import com.linkdeck.core.network.NotifDismissPush
import com.linkdeck.core.network.NotifPush
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Controller-side mirror: live list of host notifications with reply/dismiss. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
) : ViewModel() {

    private val _items = MutableStateFlow<List<NotifPush>>(emptyList())
    val items: StateFlow<List<NotifPush>> = _items

    private val _hostClip = MutableStateFlow("")
    val hostClip: StateFlow<String> = _hostClip

    init {
        viewModelScope.launch {
            // Snapshot of what is already on the host.
            connectionManager.request("cmd", ProtoJson.encodeToString(
                com.linkdeck.core.network.CmdReq.serializer(),
                com.linkdeck.core.network.CmdReq("notif_snapshot"),
            )).onSuccess { json ->
                runCatching {
                    val arr = ProtoJson.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(NotifPush.serializer()), json)
                    _items.value = arr.reversed()
                }
            }
            connectionManager.events.collect { raw ->
                val env = runCatching { ProtoJson.decodeFromString(Envelope.serializer(), raw) }.getOrNull() ?: return@collect
                when (env.type) {
                    "notif" -> runCatching {
                        ProtoJson.decodeFromString(NotifPush.serializer(), env.payload)
                    }.onSuccess { push ->
                        _items.update { list -> (listOf(push) + list.filterNot { it.key == push.key }).take(50) }
                    }
                    "notif_removed" -> runCatching {
                        ProtoJson.decodeFromString(NotifDismissPush.serializer(), env.payload)
                    }.onSuccess { d -> _items.update { list -> list.filterNot { it.key == d.key } } }
                    "clip" -> runCatching {
                        ProtoJson.decodeFromString(com.linkdeck.core.network.ClipPush.serializer(), env.payload)
                    }.onSuccess { c -> _hostClip.value = c.text }
                }
            }
        }
    }

    fun dismiss(key: String) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("notif_dismiss", """{"key":"$key"}"""),
        ))
    }

    fun reply(key: String, text: String) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("notif_reply", """{"key":"$key","text":${ProtoJson.encodeToString(kotlinx.serialization.builtins.serializer<String>(), text)}}"""),
        ))
    }

    fun sendClipboardToHost(text: String) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("set_clipboard", """{"text":${ProtoJson.encodeToString(kotlinx.serialization.builtins.serializer<String>(), text)}}"""),
        ))
    }
}
