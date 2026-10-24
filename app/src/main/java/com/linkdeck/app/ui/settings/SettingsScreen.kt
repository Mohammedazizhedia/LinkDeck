package com.linkdeck.app.ui.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkdeck.app.MainViewModel
import com.linkdeck.app.R
import com.linkdeck.app.settings.AppLanguage
import com.linkdeck.app.settings.IconStyle
import com.linkdeck.core.design.AccentTheme
import com.linkdeck.core.design.AppFont
import com.linkdeck.core.design.IconStroke
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing
import com.linkdeck.core.design.ThemeMode

private val ICON_ALIASES = mapOf(
    IconStyle.DEFAULT to "com.linkdeck.app.alias.IconDefault",
    IconStyle.TEAL to "com.linkdeck.app.alias.IconTeal",
    IconStyle.INDIGO to "com.linkdeck.app.alias.IconIndigo",
    IconStyle.DARK to "com.linkdeck.app.alias.IconDark",
)

/** Appearance + launcher-icon customization. Every change applies live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenGuide: () -> Unit = {}, onOpenCustomize: () -> Unit = {}) {
    val vm: MainViewModel = hiltViewModel()
    val context = LocalContext.current
    var showHideWarning by remember { mutableStateOf(false) }

    val themeMode by vm.themeMode.collectAsState()
    val accent by vm.accent.collectAsState()
    val language by vm.language.collectAsState()
    val font by vm.fontFamily.collectAsState()
    val fontScale by vm.fontScale.collectAsState()
    val fontWeight by vm.fontWeight.collectAsState()
    val cornerScale by vm.cornerScale.collectAsState()
    val iconStroke by vm.iconStroke.collectAsState()
    val iconStyle by vm.settings.iconStyle.collectAsState()

    fun applyIconStyle(style: IconStyle) {
        val pm = context.packageManager
        ICON_ALIASES.forEach { (s, name) ->
            val state = if (style == s) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, name), state, PackageManager.DONT_KILL_APP,
            )
        }
        vm.settings.setIconStyle(style)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionTitle(stringResource(R.string.settings_theme))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = themeMode == m,
                        onClick = { vm.settings.setThemeMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = ThemeMode.entries.size),
                    ) { Text(stringResource(when (m) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    })) }
                }
            }

            SectionTitle(stringResource(R.string.settings_accent))
            AccentTheme.entries.forEach { a ->
                SelectRow(
                    label = stringResource(when (a) {
                        AccentTheme.DEFAULT -> R.string.accent_default
                        AccentTheme.TEAL -> R.string.accent_teal
                        AccentTheme.INDIGO -> R.string.accent_indigo
                        AccentTheme.AMBER -> R.string.accent_amber
                        AccentTheme.ROSE -> R.string.accent_rose
                        AccentTheme.DYNAMIC -> R.string.accent_dynamic
                    }),
                    selected = accent == a,
                    onClick = { vm.settings.setAccent(a) },
                )
            }

            SectionTitle(stringResource(R.string.settings_language))
            AppLanguage.entries.forEach { l ->
                SelectRow(
                    label = stringResource(when (l) {
                        AppLanguage.AUTO -> R.string.lang_auto
                        AppLanguage.ARABIC -> R.string.lang_ar
                        AppLanguage.ENGLISH -> R.string.lang_en
                    }),
                    selected = language == l,
                    onClick = { vm.settings.setLanguage(l) },
                )
            }

                        SectionTitle(stringResource(R.string.settings_font))
            AppFont.entries.forEach { f ->
                SelectRow(
                    label = stringResource(when (f) {
                        AppFont.CAIRO -> R.string.font_cairo
                        AppFont.IBM_PLEX -> R.string.font_ibm
                        AppFont.MADIKA -> R.string.font_madika
                        AppFont.BASSEET -> R.string.font_basseet
                        AppFont.CAMEL -> R.string.font_camel
                        AppFont.SYSTEM -> R.string.font_system
                    }),
                    selected = font == f,
                    onClick = { vm.settings.setFontFamily(f) },
                )
            }
            Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleMedium)
            Slider(value = fontScale, onValueChange = { vm.settings.setFontScale(it) }, valueRange = 0.85f..1.3f)
            Text(stringResource(R.string.settings_font_weight), style = MaterialTheme.typography.titleMedium)
            Slider(value = fontWeight.toFloat(), onValueChange = { vm.settings.setFontWeight(it.toInt()) }, valueRange = 300f..800f)

            SectionTitle(stringResource(R.string.settings_corners))
            Slider(value = cornerScale, onValueChange = { vm.settings.setCornerScale(it) }, valueRange = 0.5f..1.5f)

            SectionTitle(stringResource(R.string.settings_icon_stroke))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                IconStroke.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = iconStroke == s,
                        onClick = { vm.settings.setIconStroke(s) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = IconStroke.entries.size),
                    ) { Text(stringResource(when (s) {
                        IconStroke.THIN -> R.string.stroke_thin
                        IconStroke.REGULAR -> R.string.stroke_regular
                        IconStroke.BOLD -> R.string.stroke_bold
                    })) }
                }
            }

            SectionTitle(stringResource(R.string.settings_icon_style))
            IconStyle.entries.forEach { s ->
                SelectRow(
                    label = stringResource(when (s) {
                        IconStyle.DEFAULT -> R.string.icon_style_default
                        IconStyle.TEAL -> R.string.icon_style_teal
                        IconStyle.INDIGO -> R.string.icon_style_indigo
                        IconStyle.DARK -> R.string.icon_style_dark
                        IconStyle.HIDDEN -> R.string.icon_style_hidden
                    }),
                    selected = iconStyle == s,
                    onClick = { if (s == IconStyle.HIDDEN) showHideWarning = true else applyIconStyle(s) },
                )
            }

            SelectRow(label = stringResource(R.string.nav_customize_title), selected = false, onClick = onOpenCustomize)
            SelectRow(label = stringResource(R.string.guide_title), selected = false, onClick = onOpenGuide)

            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = {
                    vm.settings.resetToDefaults()
                    applyIconStyle(IconStyle.DEFAULT)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.reset_default)) }
        }
    }

    if (showHideWarning) {
        AlertDialog(
            onDismissRequest = { showHideWarning = false },
            title = { Text(stringResource(R.string.hide_icon_warning_title)) },
            text = { Text(stringResource(R.string.hide_icon_warning_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val pm = context.packageManager
                    ICON_ALIASES.values.forEach { name ->
                        pm.setComponentEnabledSetting(
                            ComponentName(context.packageName, name),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                        )
                    }
                    vm.settings.setIconStyle(IconStyle.HIDDEN)
                    showHideWarning = false
                }) { Text(stringResource(R.string.hide_icon_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showHideWarning = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) LinkDeckTheme.tints.accentBg else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.xl)) {
            Text(
                label, style = MaterialTheme.typography.bodyLarge,
                color = if (selected) LinkDeckTheme.tints.accentFg else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
