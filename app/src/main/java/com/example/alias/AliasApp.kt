package com.example.alias

import android.app.Application
import com.example.alias.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class AliasApp : Application() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val storedRaw = withContext(Dispatchers.IO) { settingsRepository.settings.first().uiLanguage }
            val stored = canonicalizeLocalePreference(storedRaw)
            if (storedRaw != stored) {
                withContext(Dispatchers.IO) { settingsRepository.updateUiLanguage(stored) }
            }

            val initialTag = resolveInitialLocalePreference(stored)
            if (initialTag != stored) {
                withContext(Dispatchers.IO) { settingsRepository.updateUiLanguage(initialTag) }
            }

            applyLocalePreference(initialTag)

            settingsRepository.settings
                .map { it.uiLanguage }
                .distinctUntilChanged()
                .collect { raw ->
                    val canonical = canonicalizeLocalePreference(raw)
                    if (canonical != raw) {
                        withContext(Dispatchers.IO) { settingsRepository.updateUiLanguage(canonical) }
                    } else {
                        applyLocalePreference(canonical)
                    }
                }
        }
    }
}
