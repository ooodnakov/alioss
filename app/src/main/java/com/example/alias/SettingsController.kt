package com.example.alias

import com.example.alias.achievements.AchievementsManager
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsController
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val achievementsManager: AchievementsManager,
) {
    val settings: Flow<Settings> = settingsRepository.settings

    val enabledDeckIds: Flow<Set<String>> = settings.map { it.enabledDeckIds }

    val trustedSources: Flow<Set<String>> = settings.map { it.trustedSources }

    suspend fun addTrustedSource(originOrHost: String): TrustedSourceResult {
        val input = originOrHost.trim().lowercase()
        val normalized = when {
            input.startsWith("https://") -> input.removeSuffix("/")
            "://" in input -> null
            else -> input
        }
        if (normalized.isNullOrBlank()) {
            return TrustedSourceResult.Invalid
        }
        val current = settingsRepository.settings.first().trustedSources.toMutableSet()
        val changed = current.add(normalized)
        if (!changed) {
            return TrustedSourceResult.Unchanged(normalized)
        }
        trackSettingsUpdate { settingsRepository.setTrustedSources(current) }
        return TrustedSourceResult.Added(normalized)
    }

    suspend fun removeTrustedSource(entry: String) {
        trackSettingsUpdate {
            val current = settingsRepository.settings.first().trustedSources.toMutableSet()
            current -= entry
            settingsRepository.setTrustedSources(current)
        }
    }

    suspend fun updateDifficultyFilter(min: Int, max: Int) {
        trackSettingsUpdate { settingsRepository.updateDifficultyFilter(min, max) }
    }

    suspend fun updateCategoriesFilter(categories: Set<String>) {
        trackSettingsUpdate { settingsRepository.setCategoriesFilter(categories) }
    }

    suspend fun updateWordClassesFilter(classes: Set<String>) {
        trackSettingsUpdate { settingsRepository.setWordClassesFilter(classes) }
    }

    suspend fun updateSeenTutorial(value: Boolean) {
        trackSettingsUpdate { settingsRepository.updateSeenTutorial(value) }
    }

    suspend fun applySettingsUpdate(request: SettingsUpdateRequest) {
        trackSettingsUpdate {
            settingsRepository.updateRoundSeconds(request.roundSeconds)
            settingsRepository.updateTargetWords(request.targetWords)
            settingsRepository.updateTargetScore(request.targetScore)
            settingsRepository.updateScoreTargetEnabled(request.scoreTargetEnabled)
            settingsRepository.updateSkipPolicy(request.maxSkips, request.penaltyPerSkip)
            settingsRepository.updatePunishSkips(request.punishSkips)
            settingsRepository.updateAllowNSFW(request.allowNSFW)
            settingsRepository.updateHapticsEnabled(request.haptics)
            settingsRepository.updateSoundEnabled(request.sound)
            settingsRepository.updateOneHandedLayout(request.oneHanded)
            settingsRepository.updateVerticalSwipes(request.verticalSwipes)
            settingsRepository.updateOrientation(request.orientation)
            settingsRepository.updateUiLanguage(canonicalizeLocalePreference(request.uiLanguage))
            settingsRepository.setTeams(request.teams)
        }
    }

    suspend fun setEnabledDeckIds(ids: Set<String>) {
        trackSettingsUpdate { settingsRepository.setEnabledDeckIds(ids) }
    }

    suspend fun updateDeckLanguagesFilter(languages: Set<String>) {
        trackSettingsUpdate { settingsRepository.setDeckLanguagesFilter(languages) }
    }

    suspend fun setOrientation(value: String) {
        trackSettingsUpdate { settingsRepository.updateOrientation(value) }
    }

    private suspend fun trackSettingsUpdate(block: suspend () -> Unit) {
        block()
        achievementsManager.onSettingsAdjusted()
    }

    sealed interface TrustedSourceResult {
        data object Invalid : TrustedSourceResult
        data class Added(val normalized: String) : TrustedSourceResult
        data class Unchanged(val normalized: String) : TrustedSourceResult
    }
}
