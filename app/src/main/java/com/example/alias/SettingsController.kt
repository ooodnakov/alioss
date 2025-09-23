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
        settingsRepository.setTrustedSources(current)
        achievementsManager.onSettingsAdjusted()
        return TrustedSourceResult.Added(normalized)
    }

    suspend fun removeTrustedSource(entry: String) {
        val current = settingsRepository.settings.first().trustedSources.toMutableSet()
        current -= entry
        settingsRepository.setTrustedSources(current)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun updateDifficultyFilter(min: Int, max: Int) {
        settingsRepository.updateDifficultyFilter(min, max)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun updateCategoriesFilter(categories: Set<String>) {
        settingsRepository.setCategoriesFilter(categories)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun updateWordClassesFilter(classes: Set<String>) {
        settingsRepository.setWordClassesFilter(classes)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun updateSeenTutorial(value: Boolean) {
        settingsRepository.updateSeenTutorial(value)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun applySettingsUpdate(request: SettingsUpdateRequest) {
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
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun setEnabledDeckIds(ids: Set<String>) {
        settingsRepository.setEnabledDeckIds(ids)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun updateDeckLanguagesFilter(languages: Set<String>) {
        settingsRepository.setDeckLanguagesFilter(languages)
        achievementsManager.onSettingsAdjusted()
    }

    suspend fun setOrientation(value: String) {
        settingsRepository.updateOrientation(value)
        achievementsManager.onSettingsAdjusted()
    }

    sealed interface TrustedSourceResult {
        data object Invalid : TrustedSourceResult
        data class Added(val normalized: String) : TrustedSourceResult
        data class Unchanged(val normalized: String) : TrustedSourceResult
    }
}
