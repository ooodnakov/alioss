package com.example.alias

import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsControllerTest {
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var controller: SettingsController

    @Before
    fun setUp() {
        settingsRepository = FakeSettingsRepository()
        controller = SettingsController(settingsRepository)
    }

    @Test
    fun addTrustedSourceRejectsInvalidScheme() = runBlocking {
        val result = controller.addTrustedSource("http://example.com")
        assertTrue(result is SettingsController.TrustedSourceResult.Invalid)
    }

    @Test
    fun applySettingsUpdatePersistsChanges() = runBlocking {
        val initialSettings = Settings(
            allowNSFW = false,
        )
        settingsRepository.state.value = initialSettings

        val request = SettingsUpdateRequest.from(initialSettings).copy(
            allowNSFW = true,
            uiLanguage = "ru",
        )

        controller.applySettingsUpdate(request)

        val updated = settingsRepository.state.value
        assertTrue(updated.allowNSFW)
        assertEquals("ru", updated.uiLanguage)
    }

    @Test
    fun updateDeckLanguagesFilterPersistsSelection() = runBlocking {
        controller.updateDeckLanguagesFilter(setOf("en", "ru"))

        assertEquals(setOf("en", "ru"), settingsRepository.state.value.selectedDeckLanguages)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(Settings())
        override val settings: Flow<Settings> = state

        override suspend fun updateRoundSeconds(value: Int) {
            state.value = state.value.copy(roundSeconds = value)
        }
        override suspend fun updateTargetWords(value: Int) {
            state.value = state.value.copy(targetWords = value)
        }
        override suspend fun updateTargetScore(value: Int) {
            state.value = state.value.copy(targetScore = value)
        }
        override suspend fun updateScoreTargetEnabled(value: Boolean) {
            state.value = state.value.copy(scoreTargetEnabled = value)
        }
        override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) {
            state.value = state.value.copy(maxSkips = maxSkips, penaltyPerSkip = penaltyPerSkip)
        }
        override suspend fun updatePunishSkips(value: Boolean) {
            state.value = state.value.copy(punishSkips = value)
        }
        override suspend fun setEnabledDeckIds(ids: Set<String>) {
            state.value = state.value.copy(enabledDeckIds = ids)
        }
        override suspend fun removeEnabledDeckId(deckId: String) {
            state.value = state.value.copy(enabledDeckIds = state.value.enabledDeckIds - deckId)
        }
        override suspend fun setDeckLanguagesFilter(languages: Set<String>) {
            state.value = state.value.copy(selectedDeckLanguages = languages)
        }
        override suspend fun updateAllowNSFW(value: Boolean) {
            state.value = state.value.copy(allowNSFW = value)
        }
        override suspend fun updateStemmingEnabled(value: Boolean) {
            state.value = state.value.copy(stemmingEnabled = value)
        }
        override suspend fun updateHapticsEnabled(value: Boolean) {
            state.value = state.value.copy(hapticsEnabled = value)
        }
        override suspend fun updateSoundEnabled(value: Boolean) {
            state.value = state.value.copy(soundEnabled = value)
        }
        override suspend fun updateOneHandedLayout(value: Boolean) {
            state.value = state.value.copy(oneHandedLayout = value)
        }
        override suspend fun updateOrientation(value: String) {
            state.value = state.value.copy(orientation = value)
        }
        override suspend fun updateUiLanguage(language: String) {
            state.value = state.value.copy(uiLanguage = language)
        }
        override suspend fun updateDifficultyFilter(min: Int, max: Int) {
            state.value = state.value.copy(minDifficulty = min, maxDifficulty = max)
        }
        override suspend fun setCategoriesFilter(categories: Set<String>) {
            state.value = state.value.copy(selectedCategories = categories)
        }
        override suspend fun setWordClassesFilter(classes: Set<String>) {
            state.value = state.value.copy(selectedWordClasses = classes)
        }
        override suspend fun setTeams(teams: List<String>) {
            state.value = state.value.copy(teams = teams)
        }
        override suspend fun updateVerticalSwipes(value: Boolean) {
            state.value = state.value.copy(verticalSwipes = value)
        }
        override suspend fun setTrustedSources(origins: Set<String>) {
            state.value = state.value.copy(trustedSources = origins)
        }
        override suspend fun readBundledDeckHashes(): Set<String> = emptySet()
        override suspend fun writeBundledDeckHashes(entries: Set<String>) = Unit
        override suspend fun readDeletedBundledDeckIds(): Set<String> = emptySet()
        override suspend fun addDeletedBundledDeckId(deckId: String) = Unit
        override suspend fun removeDeletedBundledDeckId(deckId: String) = Unit
        override suspend fun readDeletedImportedDeckIds(): Set<String> = emptySet()
        override suspend fun addDeletedImportedDeckId(deckId: String) = Unit
        override suspend fun removeDeletedImportedDeckId(deckId: String) = Unit
        override suspend fun updateSeenTutorial(value: Boolean) {
            state.value = state.value.copy(seenTutorial = value)
        }
        override suspend fun clearAll() {
            state.value = Settings()
        }
    }
}
