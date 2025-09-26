package com.example.alioss.ui.settings

import com.example.alioss.SettingsUpdateRequest
import com.example.alioss.data.settings.Settings
import kotlinx.coroutines.flow.StateFlow

/** Minimal contract consumed by [settingsScreen]. */
interface SettingsScreenViewModel {
    val settings: StateFlow<Settings>

    fun updateSeenTutorial(value: Boolean)
    suspend fun updateSettings(request: SettingsUpdateRequest)
    fun resetLocalData(onDone: (() -> Unit)? = null)
    fun restartMatch()
}
