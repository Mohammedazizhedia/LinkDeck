package com.linkdeck.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linkdeck.app.R
import com.linkdeck.core.design.LineIcon
import com.linkdeck.core.design.LineIconView
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.Spacing

data class FeatureMeta(val id: String, val icon: LineIcon, val labelRes: Int)

/** Feature visibility + rail order, persisted so the rail updates instantly. */
object FeatureCatalog {
    val ALL = listOf(
        FeatureMeta("devices", LineIcon.DEVICES, R.string.nav_devices),
        FeatureMeta("mirror", LineIcon.MIRROR, R.string.feature_mirror),
        FeatureMeta("files", LineIcon.FILES, R.string.feature_files),
        FeatureMeta("notifications", LineIcon.NOTIFICATIONS, R.string.feature_notifications),
        FeatureMeta("messages", LineIcon.MESSAGES, R.string.feature_messages),
        FeatureMeta("calls", LineIcon.CALLS, R.string.feature_calls),
        FeatureMeta("apps", LineIcon.APPS, R.string.feature_apps),
        FeatureMeta("settings", LineIcon.SETTINGS, R.string.settings),
    )

    private const val PREFS = "linkdeck_nav"
    private const val KEY_ORDER = "order"
    private const val KEY_HIDDEN = "hidden"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun visible(context: Context): List<FeatureMeta> {
        val order = prefs(context).getString(KEY_ORDER, null)?.split(',')?.filter { it.isNotBlank() }
        val hidden = hidden(context)
        val byId = ALL.associateBy { it.id }
        val ordered = order?.mapNotNull { byId[it] } ?: ALL
        return (ordered + ALL.filter { f -> f.id !in ordered.map { it.id } }).filter { it.id !in hidden }
    }

    fun hidden(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun save(context: Context, orderedIds: List<String>, hidden: Set<String>) {
        prefs(context).edit()
            .putString(KEY_ORDER, orderedIds.joinToString(","))
            .putStringSet(KEY_HIDDEN, hidden)
            .apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationCustomizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state = remember { mutableStateOf(FeatureCatalog.visible(context) to FeatureCatalog.hidden(context)) }

    fun persist(order: List<FeatureMeta>, hidden: Set<String>) {
        state.value = order to hidden
        FeatureCatalog.save(context, order.map { it.id }, hidden)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        LineIconView(icon = LineIcon.HOME, tint = MaterialTheme.colorScheme.onSurface, size = 24.dp)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val (order, hidden) = state.value
            order.forEachIndexed { index, f ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape((12 * LinkDeckTheme.cornerScale).dp)) {
                    Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        LineIconView(icon = f.icon, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 22.dp)
                        Text(
                            stringResource(f.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Spacing.md).weight(1f),
                        )
                        TextButton(onClick = {
                            if (index > 0) persist(order.toMutableList().apply { add(index - 1, removeAt(index)) }, hidden)
                        }, enabled = index > 0) { Text("↑") }
                        TextButton(onClick = {
                            if (index < order.size - 1) persist(order.toMutableList().apply { add(index + 1, removeAt(index)) }, hidden)
                        }, enabled = index < order.size - 1) { Text("↓") }
                        Switch(
                            checked = f.id !in hidden,
                            onCheckedChange = { visibleNow ->
                                persist(order, if (visibleNow) hidden - f.id else hidden + f.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
