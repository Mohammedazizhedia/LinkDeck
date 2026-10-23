package com.linkdeck.app.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    /**
     * Applies the persisted language override. Called from Application.attachBaseContext
     * so the whole process (services included) honors the user's choice.
     */
    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences(SettingsStore.PREFS, Context.MODE_PRIVATE)
        val tag = prefs.getString(SettingsStore.KEY_LANGUAGE, null)
        val language = tag?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.AUTO
        val locale = when (language) {
            AppLanguage.ARABIC -> Locale("ar")
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.AUTO -> null
        } ?: return context
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
