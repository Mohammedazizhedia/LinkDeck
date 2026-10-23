package com.linkdeck.app.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProWizardViewModel @Inject constructor(
    val pro: ProModeManager,
    val grants: SelfGrantEngine,
) : ViewModel()

/** One-time HOST wizard: Developer options → Wireless Debugging → pairing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProWizardScreen(onBack: () -> Unit) {
    val vm: ProWizardViewModel = hiltViewModel()
    val step by vm.pro.step.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xxl), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            StepItem(stringResource(R.string.pro_step_developer), active = step == ProModeManager.Step.WIZARD_DEVELOPER)
            StepItem(stringResource(R.string.pro_step_wd), active = step == ProModeManager.Step.WIZARD_WIRELESS_DEBUG)
            StepItem(stringResource(R.string.pro_step_pair), active = step == ProModeManager.Step.PAIRING)

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = vm.pro::openDeveloperSettings) { Text(stringResource(R.string.pro_open_dev_settings)) }
                OutlinedButton(onClick = vm.pro::openWirelessDebuggingSettings) { Text(stringResource(R.string.pro_open_wd)) }
            }
            Button(onClick = vm.pro::markWirelessDebuggingEnabled) {
                Text(stringResource(R.string.pro_wd_done))
            }

            val code = vm.pro.lastPairingCode()
            if (code.isNotBlank()) {
                Text(stringResource(R.string.pro_capture_hint, code), style = MaterialTheme.typography.bodyMedium)
            }

            Text(stringResource(R.string.pro_grant_summary), style = MaterialTheme.typography.titleMedium)
            vm.grants.secureSettingsGrants().forEach { g ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp)) {
                    Row(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                        Text(
                            "${g.label}: ${stringResource(if (g.ok) R.string.pro_granted else R.string.pro_failed)} (${g.detail})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (g.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedButton(onClick = vm.pro::useNormalModeFallback) {
                Text(stringResource(R.string.pro_normal_fallback))
            }
        }
    }
}

@Composable
private fun StepItem(text: String, active: Boolean) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
