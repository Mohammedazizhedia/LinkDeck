package com.linkdeck.app.voice
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
@Singleton
class VoiceCommandManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface VoiceState {
        data object Idle : VoiceState
        data object Downloading : VoiceState
        data object Ready : VoiceState
        data object Listening : VoiceState
        data class Error(val message: String) : VoiceState
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state
    private var speechService: SpeechService? = null
    private var model: Model? = null
    companion object {
        private const val AR_MODEL = "https://alphacephei.com/vosk/models/vosk-model-small-ar-0.22.zip"
        private const val EN_MODEL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    }
    private fun modelDir(lang: String): File = File(context.filesDir, "vosk/$lang")
    fun isReady(lang: String): Boolean = modelDir(lang).exists() &&
        (modelDir(lang).listFiles()?.isNotEmpty() == true)
    fun ensureModelAndStart(lang: String, onCommand: (String) -> Unit) {
        if (speechService != null) { stop(); return }
        if (isReady(lang)) { startListening(lang, onCommand); return }
        _state.value = VoiceState.Downloading
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val url = if (lang.startsWith("ar")) AR_MODEL else EN_MODEL
        val request = DownloadManager.Request(Uri.parse(url))
            .setDestinationInExternalFilesDir(context, "vosk", "$lang.zip")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        val id = runCatching { dm.enqueue(request) }.getOrElse {
            _state.value = VoiceState.Error(it.message ?: "download_failed"); return
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == id) {
                    runCatching { ctx.unregisterReceiver(this) }
                    scope.launch {
                        runCatching {
                            unzip(File(context.getExternalFilesDir("vosk"), "$lang.zip"), modelDir(lang))
                        }.onSuccess { startListening(lang, onCommand) }
                            .onFailure { _state.value = VoiceState.Error(it.message ?: "model_failed") }
                    }
                }
            }
        }
        runCatching { context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) }
    }
    private fun unzip(zip: File, target: File) {
        if (!zip.exists()) error("download_missing")
        target.mkdirs()
        ZipInputStream(zip.inputStream()).use { zin ->
            var e = zin.nextEntry
            val rootPrefix = e?.name?.substringBefore('/') ?: ""
            while (e != null) {
                val rel = e.name.removePrefix(rootPrefix).trimStart('/')
                if (rel.isNotBlank()) {
                    val out = File(target, rel)
                    if (e.isDirectory) out.mkdirs()
                    else { out.parentFile?.mkdirs(); out.outputStream().use { zin.copyTo(it) } }
                }
                zin.closeEntry()
                e = zin.nextEntry
            }
        }
        zip.delete()
    }
    private fun startListening(lang: String, onCommand: (String) -> Unit) {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { Model(modelDir(lang).path) } }
                .onSuccess { m ->
                    model = m
                    val grammar = """["[unk]","افتح","الملفات","الشاشة","open","files","mirror","connect","اتصل","الأجهزة","devices"]"""
                    val recognizer = Recognizer(m, 16000.0f, grammar)
                    speechService = SpeechService(recognizer, 16000.0f).apply {
                        startListening(object : RecognitionListener {
                            override fun onPartialResult(hypothesis: String?) {}
                            override fun onError(e: Exception?) {
                                _state.value = VoiceState.Error(e?.message ?: "mic_error")
                            }
                            override fun onTimeout() {}
                            override fun onResult(hypothesis: String?) {
                                hypothesis?.extractText()?.let { mapCommand(it) }?.let(onCommand)
                            }
                            override fun onFinalResult(hypothesis: String?) {
                                hypothesis?.extractText()?.let { mapCommand(it) }?.let(onCommand)
                            }
                        })
                    }
                    _state.value = VoiceState.Ready
                }
                .onFailure { _state.value = VoiceState.Error(it.message ?: "model_load_failed") }
        }
    }
    fun mapCommand(text: String): String? = when {
        text.contains("الملفات") || text.contains("files", ignoreCase = true) -> "files"
        text.contains("الشاشة") || text.contains("mirror", ignoreCase = true) -> "mirror"
        text.contains("اتصل") || text.contains("connect", ignoreCase = true) -> "devices"
        text.contains("الأجهزة") || text.contains("devices", ignoreCase = true) -> "devices"
        else -> null
    }
    fun stop() {
        runCatching { speechService?.stop() }
        runCatching { speechService?.shutdown() }
        speechService = null
        _state.value = VoiceState.Idle
    }
    private fun String.extractText(): String? = runCatching {
        org.json.JSONObject(this).getString("text")
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
