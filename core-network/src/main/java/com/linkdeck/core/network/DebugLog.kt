package com.linkdeck.core.network

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-app diagnostic ring buffer. Visible and copyable from the UI. */
object DebugLog {
    private const val MAX = 160
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    @Synchronized
    fun log(tag: String, msg: String) {
        _lines.value = (_lines.value + "${fmt.format(Date())} [$tag] $msg").takeLast(MAX)
    }

    fun dump(): String = _lines.value.joinToString("\n")
    fun clear() { _lines.value = emptyList() }
}
