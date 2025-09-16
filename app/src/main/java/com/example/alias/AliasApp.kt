package com.example.alias

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.alias.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class AliasApp : Application() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val storedRaw = withContext(Dispatchers.IO) { settingsRepository.settings.first().uiLanguage }
            val stored = canonicalizeLanguageSetting(storedRaw)
            if (storedRaw != stored) {
                settingsRepository.updateUiLanguage(stored)
            }

            val appLocales = AppCompatDelegate.getApplicationLocales()
            val defaultLocales = LocaleListCompat.getAdjustedDefault()
            val defaultTag = canonicalizeLanguageSetting(defaultLocales.toLanguageTags())
            val canonicalAppTag = canonicalizeLanguageSetting(appLocales.toLanguageTags())
            val appTag = when {
                appLocales.isEmpty -> "system"
                canonicalAppTag == defaultTag -> "system"
                else -> canonicalAppTag
            }

            val tag = if (appTag != "system") appTag else stored
            if (stored != tag) {
                settingsRepository.updateUiLanguage(tag)
            }

            val locales = if (tag == "system") {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            if (appLocales != locales) {
                AppCompatDelegate.setApplicationLocales(locales)
            }

            val defaultLocale = if (locales.isEmpty) {
                defaultLocales.get(0)
            } else {
                locales.get(0)
            }
            defaultLocale?.let(Locale::setDefault)
        }
    }

    private fun canonicalizeLanguageSetting(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.equals("system", ignoreCase = true) || trimmed.isEmpty()) {
            return "system"
        }
        val locales = LocaleListCompat.forLanguageTags(trimmed)
        if (locales.isEmpty) {
            return "system"
        }
        return locales.toLanguageTags()
    }
}

