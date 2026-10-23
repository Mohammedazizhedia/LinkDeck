package com.linkdeck.app.ui.mode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.R
import com.linkdeck.app.settings.AppMode
import com.linkdeck.app.settings.ModeManager
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ModeViewModel @Inject constructor(private val modeManager: ModeManager) : ViewModel() {
    fun choose(mode: AppMode) = viewModelScope.launch { modeManager.choose(mode) }
}

/**
 * First-run mode selection: this same APK becomes a HOST (phone) or a
 * CONTROLLER (tablet) depending on the user's choice.
 */
@Composable
fun ModeSelectionScreen(onModeChosen: () -> Unit) {
    val vm: ModeViewModel = hiltViewModel()

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LineIconView(icon = LineIcon.DEVICES, tint = MaterialTheme.colorScheme.primary, size = 72.dp)
        Spacer(Modifier.height(Spacing.xl))
        Text(stringResource(R.string.mode_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.mode_choose_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xxl))

        ModeCard(
            icon = LineIcon.CONNECTION,
            title = stringResource(R.string.mode_host),
            description = stringResource(R.string.mode_host_desc),
            tint = LinkDeckTheme.tints.successBg,
            tintFg = LinkDeckTheme.tints.successFg,
            onClick = { vm.choose(AppMode.HOST); onModeChosen() },
        )
        Spacer(Modifier.height(Spacing.xl))
        ModeCard(
            icon = LineIcon.MIRROR,
            title = stringResource(R.string.mode_controller),
            description = stringResource(R.string.mode_controller_desc),
            tint = LinkDeckTheme.tints.accentBg,
            tintFg = LinkDeckTheme.tints.accentFg,
            onClick = { vm.choose(AppMode.CONTROLLER); onModeChosen() },
        )
    }
}

@Composable
private fun ModeCard(
    icon: LineIcon,
    title: String,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    tintFg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        color = tint,
        shape = RoundedCornerShape((18 * LinkDeckTheme.cornerScale).dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            LineIconView(icon = icon, tint = tintFg, size = 40.dp)
            Spacer(Modifier.width(Spacing.xl))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = tintFg)
                Spacer(Modifier.height(Spacing.xxs))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = tintFg)
            }
        }
    }
}
