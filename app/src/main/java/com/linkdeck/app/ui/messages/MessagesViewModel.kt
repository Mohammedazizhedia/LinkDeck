package com.linkdeck.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.core.network.CmdReq
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class SmsItem(val id: String, val address: String, val body: String)

@Serializable
data class ContactItem(val id: String, val name: String, val phone: String)

/** The host answers with its generic {id, title, subtitle} wire shape. */
@Serializable
private data class WireItem(val id: String, val title: String, val subtitle: String)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
) : ViewModel() {

    private val _sms = MutableStateFlow<List<SmsItem>>(emptyList())
    val sms: StateFlow<List<SmsItem>> = _sms

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            connectionManager.request("cmd", ProtoJson.encodeToString(CmdReq.serializer(), CmdReq("sms_list")))
                .onSuccess { json ->
                    runCatching {
                        _sms.value = ProtoJson.decodeFromString(ListSerializer(WireItem.serializer()), json)
                            .map { SmsItem(it.id, it.title, it.subtitle) }
                    }
                }
                .onFailure { _error.value = it.message }
            connectionManager.request("cmd", ProtoJson.encodeToString(CmdReq.serializer(), CmdReq("contacts_list")))
                .onSuccess { json ->
                    runCatching {
                        _contacts.value = ProtoJson.decodeFromString(ListSerializer(WireItem.serializer()), json)
                            .map { ContactItem(it.id, it.title, it.subtitle) }
                    }
                }
        }
    }

    fun send(to: String, body: String) {
        _sending.value = true
        viewModelScope.launch {
            val args = """{"to":${ProtoJson.encodeToString(kotlinx.serialization.builtins.serializer<String>(), to)},"body":${ProtoJson.encodeToString(kotlinx.serialization.builtins.serializer<String>(), body)}}"""
            connectionManager.request("cmd", ProtoJson.encodeToString(CmdReq.serializer(), CmdReq("sms_send", args)))
                .onSuccess { refresh() }
                .onFailure { _error.value = it.message }
            _sending.value = false
        }
    }
}
