package com.linkdeck.app.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.linkdeck.core.network.ProtoJson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class PhoneApp(val id: String, val name: String)

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
) : ViewModel() {
    private val _apps = MutableStateFlow<List<PhoneApp>>(emptyList())
    val apps: StateFlow<List<PhoneApp>> = _apps

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        connectionManager.request("cmd", ProtoJson.encodeToString(CmdReq.serializer(), CmdReq("apps_list")))
            .onSuccess { json ->
                runCatching {
                    ProtoJson.decodeFromString(ListSerializer(WireApp.serializer()), json)
                }.onSuccess { list -> _apps.value = list.map { PhoneApp(it.id, it.title) } }
            }
    }

    fun launch(pkg: String) = viewModelScope.launch {
        connectionManager.send("cmd", ProtoJson.encodeToString(
            CmdReq.serializer(), CmdReq("launch_app", """{"package":"$pkg"}""")))
    }

    @Serializable
    private data class WireApp(val id: String, val title: String, val subtitle: String)
}

/** All phone apps; tapping launches them on the phone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(onBack: () -> Unit) {
    val vm: AppsViewModel = hiltViewModel()
    val apps by vm.apps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        if (apps.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.apps_loading), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(apps, key = { it.id }) { app ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
                        onClick = { vm.launch(app.id) },
                    ) {
                        Row(Modifier.padding(Spacing.lg).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            LineIconView(icon = LineIcon.APPS, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
                            Text(
                                app.name, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = Spacing.sm), maxLines = 1, softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
