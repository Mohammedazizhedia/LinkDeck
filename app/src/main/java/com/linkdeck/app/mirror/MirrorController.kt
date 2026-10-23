package com.linkdeck.app.mirror

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.Envelope
import com.linkdeck.core.network.EventPush
import com.linkdeck.core.network.FramePush
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** CONTROLLER side of Normal-mode mirroring. */
@HiltViewModel
class MirrorViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
) : ViewModel() {

    private val _frame = MutableStateFlow<Bitmap?>(null)
    val frame: StateFlow<Bitmap?> = _frame

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    init {
        viewModelScope.launch {
            connectionManager.events.collect { raw ->
                val env = runCatching { ProtoJson.decodeFromString(Envelope.serializer(), raw) }.getOrNull() ?: return@collect
                when (env.type) {
                    "frame" -> runCatching {
                        ProtoJson.decodeFromString(FramePush.serializer(), env.payload)
                    }.onSuccess { push ->
                        val bytes = Base64.decode(push.dataBase64, Base64.DEFAULT)
                        _frame.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    "mirror_event" -> runCatching {
                        ProtoJson.decodeFromString(EventPush.serializer(), env.payload)
                    }.onSuccess { ev ->
                        when (ev.kind) {
                            "started" -> { _active.value = true; _message.value = null }
                            "stopped" -> { _active.value = false; _frame.value = null }
                            "flag_secure" -> _message.value = "flag_secure"
                            "normal_mode" -> _message.value = "normal_mode"
                            "error" -> _message.value = ev.dataJson
                        }
                    }
                }
            }
        }
    }

    fun startMirror() = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("mirror_request"),
        ))
    }

    fun key(key: String) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("inject_key", """{"key":"$key"}"""),
        ))
    }

    fun tap(nx: Float, ny: Float) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("inject_tap", """{"x":$nx,"y":$ny}"""),
        ))
    }

    fun stopMirror() = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            com.linkdeck.core.network.CmdReq.serializer(),
            com.linkdeck.core.network.CmdReq("mirror_stop"),
        ))
    }
}
