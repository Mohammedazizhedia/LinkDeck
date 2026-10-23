package com.linkdeck.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.linkdeck.core.design.AccentTheme
import com.linkdeck.core.design.AppFont
import com.linkdeck.core.design.IconStroke
import com.linkdeck.core.design.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which of the two app personalities this installation runs as. */
enum class AppMode { HOST, CONTROLLER }

enum class AppLanguage(val tag: String) { AUTO(""), ARABIC("ar"), ENGLISH("en") }

/**
 * Central appearance/persistence store over SharedPreferences. Every knob is a
 * StateFlow so Compose recomposes instantly on change — no restarts.
 */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        const val PREFS = "linkdeck_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_LANGUAGE = "language"
        const val KEY_FONT = "font_family"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_FONT_WEIGHT = "font_weight"
        const val KEY_CORNER = "corner_scale"
        const val KEY_ICON_STROKE = "icon_stroke"
        const val KEY_APP_MODE = "app_mode"
        const val KEY_ICON_STYLE = "icon_style"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- theme ----
    private val _themeMode = MutableStateFlow(readEnum(KEY_THEME, ThemeMode.SYSTEM))
    val themeMode: StateFlow<ThemeMode> = _themeMode
    fun setThemeMode(v: ThemeMode) { _themeMode.value = v; prefs.edit { putString(KEY_THEME, v.name) } }

    // ---- accent ----
    private val _accent = MutableStateFlow(readEnum(KEY_ACCENT, AccentTheme.DEFAULT))
    val accent: StateFlow<AccentTheme> = _accent
    fun setAccent(v: AccentTheme) { _accent.value = v; prefs.edit { putString(KEY_ACCENT, v.name) } }

    // ---- language ----
    private val _language = MutableStateFlow(readEnum(KEY_LANGUAGE, AppLanguage.AUTO))
    val language: StateFlow<AppLanguage> = _language
    fun setLanguage(v: AppLanguage) { _language.value = v; prefs.edit { putString(KEY_LANGUAGE, v.name) } }

    // ---- fonts ----
    private val _fontFamily = MutableStateFlow(readEnum(KEY_FONT, AppFont.CAIRO))
    val fontFamily: StateFlow<AppFont> = _fontFamily
    fun setFontFamily(v: AppFont) { _fontFamily.value = v; prefs.edit { putString(KEY_FONT, v.name) } }

    private val _fontScale = MutableStateFlow(prefs.getFloat(KEY_FONT_SCALE, 1f))
    val fontScale: StateFlow<Float> = _fontScale
    fun setFontScale(v: Float) { _fontScale.value = v; prefs.edit { putFloat(KEY_FONT_SCALE, v) } }

    private val _fontWeight = MutableStateFlow(prefs.getInt(KEY_FONT_WEIGHT, 500))
    val fontWeight: StateFlow<Int> = _fontWeight
    fun setFontWeight(v: Int) { _fontWeight.value = v; prefs.edit { putInt(KEY_FONT_WEIGHT, v) } }

    // ---- shape / icon strokes ----
    private val _cornerScale = MutableStateFlow(prefs.getFloat(KEY_CORNER, 1f))
    val cornerScale: StateFlow<Float> = _cornerScale
    fun setCornerScale(v: Float) { _cornerScale.value = v; prefs.edit { putFloat(KEY_CORNER, v) } }

    private val _iconStroke = MutableStateFlow(readEnum(KEY_ICON_STROKE, IconStroke.REGULAR))
    val iconStroke: StateFlow<IconStroke> = _iconStroke
    fun setIconStroke(v: IconStroke) { _iconStroke.value = v; prefs.edit { putString(KEY_ICON_STROKE, v.name) } }

    // ---- app mode ----
    private val _appMode = MutableStateFlow(prefs.getString(KEY_APP_MODE, null)?.let { runCatching { AppMode.valueOf(it) }.getOrNull() })
    val appMode: StateFlow<AppMode?> = _appMode
    fun setAppMode(v: AppMode) { _appMode.value = v; prefs.edit { putString(KEY_APP_MODE, v.name) } }

    // ---- launcher icon style ----
    private val _iconStyle = MutableStateFlow(readEnum(KEY_ICON_STYLE, IconStyle.DEFAULT))
    val iconStyle: StateFlow<IconStyle> = _iconStyle
    fun setIconStyle(v: IconStyle) { _iconStyle.value = v; prefs.edit { putString(KEY_ICON_STYLE, v.name) } }

    /** Clears every appearance key and re-seeds defaults (called by "Reset to default"). */
    fun resetToDefaults() {
        prefs.edit { clear() }
        _themeMode.value = ThemeMode.SYSTEM
        _accent.value = AccentTheme.DEFAULT
        _language.value = AppLanguage.AUTO
        _fontFamily.value = AppFont.CAIRO
        _fontScale.value = 1f
        _fontWeight.value = 500
        _cornerScale.value = 1f
        _iconStroke.value = IconStroke.REGULAR
        _iconStyle.value = IconStyle.DEFAULT
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}

enum class IconStyle { DEFAULT, TEAL, INDIGO, DARK, HIDDEN }

/** Chooses HOST vs CONTROLLER; persisted after first selection. */
@Singleton
class ModeManager @Inject constructor(private val store: SettingsStore) {
    val mode = store.appMode
    fun choose(mode: AppMode) = store.setAppMode(mode)
}
