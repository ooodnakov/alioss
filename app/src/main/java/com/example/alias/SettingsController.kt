package com.example.alias

import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsController
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val deckRepository: DeckRepository,
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
            return TrustedSourceResult.Added(normalized)
        }

        suspend fun removeTrustedSource(entry: String) {
            val current = settingsRepository.settings.first().trustedSources.toMutableSet()
            current -= entry
            settingsRepository.setTrustedSources(current)
        }

        suspend fun updateDifficultyFilter(min: Int, max: Int) {
            settingsRepository.updateDifficultyFilter(min, max)
        }

        suspend fun updateCategoriesFilter(categories: Set<String>) {
            settingsRepository.setCategoriesFilter(categories)
        }

        suspend fun updateWordClassesFilter(classes: Set<String>) {
            settingsRepository.setWordClassesFilter(classes)
        }

        suspend fun updateSeenTutorial(value: Boolean) {
            settingsRepository.updateSeenTutorial(value)
        }

        suspend fun applySettingsUpdate(request: SettingsUpdateRequest): UpdateResult {
            val before = settingsRepository.settings.first()
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

            val languageResult = runCatching { settingsRepository.updateLanguagePreference(request.language) }
            var languageError: String? = null
            var languageChange: LanguageChange? = null
            languageResult.onFailure { error ->
                languageError = error.message ?: "Invalid language"
            }
            languageResult.onSuccess {
                val newLang = request.language.trim().lowercase()
                val languageUnchanged = newLang.equals(before.languagePreference, ignoreCase = true)
                if (!languageUnchanged) {
                    val preferredDeckIds = withContext(Dispatchers.IO) {
                        deckRepository
                            .getDecks()
                            .first()
                            .filter { it.language.equals(newLang, ignoreCase = true) }
                            .map(DeckEntity::id)
                            .toSet()
                    }
                    val preferredDecksChanged = preferredDeckIds.isNotEmpty() && preferredDeckIds != before.enabledDeckIds
                    if (preferredDecksChanged) {
                        settingsRepository.setEnabledDeckIds(preferredDeckIds)
                        languageChange = LanguageChange(newLang, preferredDeckIds, before.enabledDeckIds)
                    }
                }
            }

            settingsRepository.setTeams(request.teams)
            return UpdateResult(languageErrorMessage = languageError, languageChange = languageChange)
        }

        suspend fun setEnabledDeckIds(ids: Set<String>) {
            settingsRepository.setEnabledDeckIds(ids)
        }

        suspend fun setOrientation(value: String) {
            settingsRepository.updateOrientation(value)
        }

        sealed interface TrustedSourceResult {
            data object Invalid : TrustedSourceResult
            data class Added(val normalized: String) : TrustedSourceResult
            data class Unchanged(val normalized: String) : TrustedSourceResult
        }

        data class UpdateResult(
            val languageErrorMessage: String? = null,
            val languageChange: LanguageChange? = null,
        )

        data class LanguageChange(
            val language: String,
            val newDeckIds: Set<String>,
            val previousDeckIds: Set<String>,
        )
    }
