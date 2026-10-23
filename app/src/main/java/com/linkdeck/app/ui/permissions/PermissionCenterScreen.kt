package com.linkdeck.app.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.R
import com.linkdeck.app.settings.SettingsStore
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.design.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Consent-first permission center. One screen lists every permission with a
 * one-line reason; the user approves explicitly via "Grant all" (runtime dialog
 * sequence first, then each special settings screen in order with progress),
 * or grants/denies individually. Nothing is requested silently, and nothing
 * is re-asked unless the user revokes it from system settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var granting by remember { mutableStateOf(false) }
    var grantIndex by remember { mutableIntStateOf(0) }

    val all = PermissionCatalog.ALL.filter { it.runtimePermissions.isNotEmpty() || it.special != SpecialPermission.NONE }
    val specials = all.filter { it.special != SpecialPermission.NONE }
    val runtimePerms = all.flatMap { it.runtimePermissions }.distinct()

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshTick++ }

    val specialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        grantIndex++
        refreshTick++
    }

    // Sequential special-permission flow: open each exact settings screen in order.
    LaunchedEffect(grantIndex, granting) {
        if (granting && grantIndex < specials.size) {
            val intent = PermissionChecker.specialIntent(context, specials[grantIndex].special)
            specialLauncher.launch(intent)
        } else if (granting && grantIndex >= specials.size) {
            granting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permission_center_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl)) {

            if (granting) {
                LinearProgressIndicator(
                    progress = { grantIndex.toFloat() / specials.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(R.string.permission_progress, grantIndex + 1, specials.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(all, key = { it.key }) { def ->
                    key(refreshTick) {
                        val state = PermissionChecker.stateOf(context, def)
                        PermissionRow(
                            def = def,
                            state = state,
                            onGrant = {
                                when {
                                    def.special != SpecialPermission.NONE ->
                                        specialLauncher.launch(PermissionChecker.specialIntent(context, def.special))
                                    def.runtimePermissions.isNotEmpty() ->
                                        runtimeLauncher.launch(def.runtimePermissions.toTypedArray())
                                }
                                refreshTick++
                            },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    grantIndex = 0
                    granting = true
                    if (runtimePerms.isNotEmpty()) runtimeLauncher.launch(runtimePerms.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.permission_grant_all)) }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.permission_never_silent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRow(def: PermissionDef, state: PermState, onGrant: () -> Unit) {
    val (bg, fg) = when (state) {
        PermState.GRANTED -> LinkDeckTheme.tints.successBg to LinkDeckTheme.tints.successFg
        PermState.DENIED -> LinkDeckTheme.tints.warningBg to LinkDeckTheme.tints.warningFg
        PermState.PERMANENTLY_DENIED -> LinkDeckTheme.tints.dangerBg to LinkDeckTheme.tints.dangerFg
        PermState.NOT_APPLICABLE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape((14 * LinkDeckTheme.cornerScale).dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(def.titleRes), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.padding(Spacing.xs))
                    StatusChip(
                        text = when (state) {
                            PermState.GRANTED -> stringResource(R.string.perm_state_granted)
                            PermState.DENIED -> stringResource(R.string.perm_state_denied)
                            PermState.PERMANENTLY_DENIED -> stringResource(R.string.perm_state_denied)
                            PermState.NOT_APPLICABLE -> ""
                        },
                        background = bg,
                        foreground = fg,
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    stringResource(def.whyRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state != PermState.GRANTED && state != PermState.NOT_APPLICABLE) {
                OutlinedButton(onClick = onGrant) { Text(stringResource(R.string.perm_grant)) }
            }
        }
    }
}
