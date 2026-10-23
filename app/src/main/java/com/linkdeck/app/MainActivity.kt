package com.linkdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.linkdeck.app.settings.SettingsStore
import com.linkdeck.core.design.AccentTheme
import com.linkdeck.core.design.ThemeMode
import com.linkdeck.core.design.AppFont
import com.linkdeck.core.design.IconStroke
import com.linkdeck.core.design.LinkDeckTheme
import com.linkdeck.core.design.loadAppFontFamily
import com.linkdeck.app.settings.AppLanguage
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(val settings: SettingsStore) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val accent: StateFlow<AccentTheme> = settings.accent.stateIn(viewModelScope, SharingStarted.Eagerly, AccentTheme.DEFAULT)
    val language: StateFlow<AppLanguage> = settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.AUTO)
    val fontFamily: StateFlow<AppFont> = settings.fontFamily.stateIn(viewModelScope, SharingStarted.Eagerly, AppFont.CAIRO)
    val fontScale: StateFlow<Float> = settings.fontScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val fontWeight: StateFlow<Int> = settings.fontWeight.stateIn(viewModelScope, SharingStarted.Eagerly, 500)
    val cornerScale: StateFlow<Float> = settings.cornerScale.stateIn(viewModelScope, SharingStarted.Eagerly, 1f)
    val iconStroke: StateFlow<IconStroke> = settings.iconStroke.stateIn(viewModelScope, SharingStarted.Eagerly, IconStroke.REGULAR)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val themeMode by vm.themeMode.collectAsState()
            val accent by vm.accent.collectAsState()
            val font by vm.fontFamily.collectAsState()
            val fontScale by vm.fontScale.collectAsState()
            val fontWeight by vm.fontWeight.collectAsState()
            val cornerScale by vm.cornerScale.collectAsState()
            val iconStroke by vm.iconStroke.collectAsState()

            val context = LocalContext.current
            val family = loadAppFontFamily(context, font, fontWeight)

            // Recreate the activity when the language override changes so the new
            // locale is applied through LocaleHelper at attach time.
            val language by vm.language.collectAsState()
            LanguageRestartEffect(language)

            LinkDeckTheme(
                mode = themeMode,
                accent = accent,
                fontFamily = family,
                fontScale = fontScale,
                fontWeight = fontWeight,
                cornerScale = cornerScale,
                iconStroke = iconStroke,
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

/** Recreates the activity when a non-auto language is chosen or switches back. */
@Composable
private fun LanguageRestartEffect(language: AppLanguage) {
    val activity = LocalContext.current as? MainActivity ?: return
    androidx.compose.runtime.LaunchedEffect(language) {
        if (language != AppLanguage.AUTO && activity.resources.configuration.locales[0]?.language != language.tag) {
            activity.recreate()
        } else if (language == AppLanguage.AUTO && activity.resources.configuration.locales[0]?.language == "ar"
            && !java.util.Locale.getDefault().language.startsWith("ar")) {
            activity.recreate()
        }
    }
}
