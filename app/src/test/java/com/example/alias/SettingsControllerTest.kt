package com.example.alias

import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.pack.ParsedPack
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
    private lateinit var deckRepository: FakeDeckRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var controller: SettingsController

    @Before
    fun setUp() {
        deckRepository = FakeDeckRepository()
        settingsRepository = FakeSettingsRepository()
        controller = SettingsController(settingsRepository, deckRepository)
    }

    @Test
    fun addTrustedSourceRejectsInvalidScheme() = runBlocking {
        val result = controller.addTrustedSource("http://example.com")
        assertTrue(result is SettingsController.TrustedSourceResult.Invalid)
    }

    @Test
    fun applySettingsUpdateChangesLanguageDecks() = runBlocking {
        val initialSettings = Settings(
            languagePreference = "en",
            enabledDeckIds = setOf("en-1"),
        )
        settingsRepository.state.value = initialSettings
        deckRepository.state.value = listOf(
            DeckEntity("es-1", "Spanish", "es", isOfficial = true, isNSFW = false, version = 1, updatedAt = 0L),
        )

        val request = SettingsUpdateRequest.from(initialSettings).copy(language = "es")
        val result = controller.applySettingsUpdate(request)

        assertEquals("es", settingsRepository.state.value.languagePreference)
        val change = result.languageChange
        assertEquals(setOf("es-1"), change?.newDeckIds)
        assertEquals(setOf("en-1"), change?.previousDeckIds)
        assertEquals(setOf("es-1"), settingsRepository.state.value.enabledDeckIds)
    }

    private class FakeDeckRepository : DeckRepository {
        val state = MutableStateFlow<List<DeckEntity>>(emptyList())
        override fun getDecks(): Flow<List<DeckEntity>> = state
        override suspend fun getWordCount(deckId: String): Int = throw UnsupportedOperationException()
        override suspend fun getDifficultyHistogram(deckId: String): List<com.example.alias.data.db.DifficultyBucket> =
            throw UnsupportedOperationException()
        override suspend fun getRecentWords(
            deckId: String,
            limit: Int,
        ): List<String> = throw UnsupportedOperationException()
        override suspend fun importJson(content: String) = throw UnsupportedOperationException()
        override suspend fun importPack(pack: ParsedPack) = throw UnsupportedOperationException()
        override suspend fun deleteDeck(deckId: String) = throw UnsupportedOperationException()
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
        override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) {
            state.value = state.value.copy(maxSkips = maxSkips, penaltyPerSkip = penaltyPerSkip)
        }
        override suspend fun updatePunishSkips(value: Boolean) {
            state.value = state.value.copy(punishSkips = value)
        }
        override suspend fun updateLanguagePreference(language: String) {
            require(language.matches(Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")))
            state.value = state.value.copy(languagePreference = language)
        }
        override suspend fun setEnabledDeckIds(ids: Set<String>) {
            state.value = state.value.copy(enabledDeckIds = ids)
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
        override suspend fun updateSeenTutorial(value: Boolean) {
            state.value = state.value.copy(seenTutorial = value)
        }
        override suspend fun clearAll() {
            state.value = Settings()
        }
    }
}
