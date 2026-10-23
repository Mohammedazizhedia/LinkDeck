package com.linkdeck.app.ui.files

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.network.FileEntry
import java.io.File

/**
 * Dual-pane phone file manager (list + preview) with search, streaming open,
 * push/pull transfers with resume, live progress.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun FilesScreen(onBack: () -> Unit) {
    val vm: FilesViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val transfers by vm.transfers.transfers.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val isExpanded = activity?.let { calculateWindowSizeClass(it).widthSizeClass == WindowWidthSizeClass.Expanded } ?: false

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // SAF gives us a stream; stage into cache then push.
        val tmp = File(context.cacheDir, "upload/" + (uri.lastPathSegment ?: "file.bin").substringAfterLast(':'))
        tmp.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        if (tmp.exists()) vm.push(tmp)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_files)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
                actions = {
                    TextButton(onClick = { pickFile.launch("*/*") }) {
                        Text(stringResource(R.string.files_upload))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::search,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.files_search_hint)) },
                singleLine = true,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                state.path,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))

            if (isExpanded) {
                Row(Modifier.weight(1f)) {
                    FileListPane(
                        entries = state.entries,
                        selected = state.selected,
                        loading = state.loading,
                        error = state.error,
                        onEntry = vm::select,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.padding(Spacing.xl))
                    PreviewPane(state, vm, modifier = Modifier.weight(1f))
                }
            } else {
                if (state.selected == null) {
                    FileListPane(state.entries, state.selected, state.loading, state.error, vm::select, Modifier.fillMaxSize())
                } else {
                    PreviewPane(state, vm, Modifier.fillMaxSize())
                }
            }

            if (transfers.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                Text(stringResource(R.string.files_transfers), style = MaterialTheme.typography.titleMedium)
                transfers.takeLast(3).forEach { t ->
                    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${t.name} — ${t.direction}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { vm.transfers.remove(t.id) }) { Text(stringResource(R.string.files_hide)) }
                        }
                        if (t.active) LinearProgressIndicator(
                            progress = { t.progress },
                            modifier = Modifier.fillMaxWidth(),
                        ) else Text(
                            t.error ?: stringResource(R.string.files_done),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (t.error != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListPane(
    entries: List<FileEntry>,
    selected: FileEntry?,
    loading: Boolean,
    error: String?,
    onEntry: (FileEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Box(modifier, contentAlignment = Alignment.Center) { LinearProgressIndicator() }
        error != null -> Box(modifier, contentAlignment = Alignment.Center) { Text(error, color = MaterialTheme.colorScheme.error) }
        entries.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.files_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(entries, key = { it.path }) { e ->
                Surface(
                    color = if (selected?.path == e.path) LinkDeckTheme.tints.accentBg
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEntry(e) },
                ) {
                    Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        LineIconView(
                            icon = if (e.isDir) LineIcon.FILES else LineIcon.INFO,
                            tint = if (selected?.path == e.path) LinkDeckTheme.tints.accentFg
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 24.dp,
                        )
                        Spacer(Modifier.padding(Spacing.sm))
                        Column {
                            Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, softWrap = false)
                            Text(
                                if (e.isDir) stringResource(R.string.files_folder)
                                else formatSize(e.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(state: FilesUiState, vm: FilesViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = state.selected ?: run {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.files_select_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (entry.mime.startsWith("image/") && state.preview != null) {
            Image(
                bitmap = state.preview!!.asImageBitmap(),
                contentDescription = entry.name,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
        }
        Text(entry.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "${entry.mime.ifEmpty { "?" }} • ${formatSize(entry.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = { vm.openOnTablet(context, entry) }) { Text(stringResource(R.string.files_open)) }
            TextButton(onClick = vm::pullSelected) { Text(stringResource(R.string.files_pull)) }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30).toFloat())
    bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20).toFloat())
    bytes >= 1 shl 10 -> "%.1f KB".format(bytes / (1 shl 10).toFloat())
    else -> "$bytes B"
}
