package com.example.alias

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.alias.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@HiltAndroidApp
class AliasApp : Application() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            val stored = settingsRepository.settings.first().uiLanguage
            val appLocales = AppCompatDelegate.getApplicationLocales()
            val systemTag = if (appLocales.isEmpty) "system" else appLocales.toLanguageTags()
            val tag = if (systemTag != "system") systemTag else stored
            if (stored != tag) {
                settingsRepository.updateUiLanguage(tag)
            }
            val locales = when (tag) {
                "system" -> LocaleListCompat.getEmptyLocaleList()
                else -> LocaleListCompat.forLanguageTags(tag)
            }
            if (appLocales != locales) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
            val defaultLocale = if (locales.isEmpty) {
                LocaleListCompat.getAdjustedDefault()[0]
            } else {
                locales[0]
            }
            defaultLocale?.let { Locale.setDefault(it) }
        }
    }
}

