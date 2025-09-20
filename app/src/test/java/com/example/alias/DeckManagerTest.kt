package com.example.alias

import android.app.Application
import android.content.Context
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.WordBrief
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.pack.ParsedPack
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeckManagerTest {
    private lateinit var context: Context
    private lateinit var deckManager: DeckManager
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordDao: FakeWordDao

    @Before
    fun setUp() {
        context = Application()
        settingsRepository = FakeSettingsRepository()
        wordDao = FakeWordDao()
        deckManager = DeckManager(
            context = context,
            deckRepository = FakeDeckRepository(),
            wordDao = wordDao,
            deckDao = FakeDeckDao(),
            turnHistoryDao = FakeTurnHistoryDao(),
            settingsRepository = settingsRepository,
            downloader = PackDownloader(OkHttpClient(), settingsRepository),
        )
    }

    @Test
    fun canonicalizeWordClassFiltersOrdersKnownValues() {
        val result = deckManager.canonicalizeWordClassFilters(listOf("verb", "unknown", "Adj", "adj"))
        assertEquals(listOf("ADJ", "VERB", "UNKNOWN"), result)
    }

    @Test
    fun parsePrimaryWordClassReturnsFirstNormalizedEntry() {
        val parsed = deckManager.parsePrimaryWordClass(" foo , adj, verb")
        assertEquals("ADJ", parsed)
    }

    @Test
    fun loadAvailableWordClassesCanonicalizesValues() = runBlocking {
        wordDao.availableWordClasses = listOf("verb", "unknown", "Adj")
        val key = DeckManager.WordClassAvailabilityKey(setOf("deck"), language = "en", allowNSFW = false)

        val classes = deckManager.loadAvailableWordClasses(key)

        assertEquals(listOf("ADJ", "VERB", "UNKNOWN"), classes)
    }

    @Test
    fun buildWordQueryFiltersSetsFlagsBasedOnSelections() {
        val settings = Settings(
            enabledDeckIds = setOf("a", "b"),
            selectedCategories = setOf("party"),
            selectedWordClasses = setOf("verb"),
            allowNSFW = true,
            minDifficulty = 2,
            maxDifficulty = 4,
        )

        val filters = deckManager.buildWordQueryFilters(settings)

        assertEquals(listOf("a", "b"), filters.deckIds)
        assertEquals(1, filters.categoryFilterEnabled)
        assertEquals(1, filters.wordClassFilterEnabled)
        assertEquals(listOf("VERB"), filters.wordClasses)
    }

    private class FakeDeckRepository : DeckRepository {
        override fun getDecks(): Flow<List<DeckEntity>> = MutableStateFlow(emptyList())
        override suspend fun getWordCount(deckId: String): Int = throw UnsupportedOperationException()
        override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> = throw UnsupportedOperationException()
        override suspend fun getRecentWords(deckId: String, limit: Int): List<String> = throw UnsupportedOperationException()
        override suspend fun importJson(content: String) = throw UnsupportedOperationException()
        override suspend fun importPack(pack: ParsedPack) = throw UnsupportedOperationException()
        override suspend fun deleteDeck(deckId: String) = throw UnsupportedOperationException()
    }

    private class FakeWordDao : WordDao {
        var availableWordClasses: List<String> = emptyList()

        override suspend fun insertWords(words: List<com.example.alias.data.db.WordEntity>) = throw UnsupportedOperationException()
        override suspend fun insertWordClasses(entries: List<com.example.alias.data.db.WordClassEntity>) = throw UnsupportedOperationException()
        override suspend fun getWordTexts(deckId: String): List<String> = throw UnsupportedOperationException()
        override suspend fun getWordCount(deckId: String): Int = throw UnsupportedOperationException()
        override suspend fun deleteByDeck(deckId: String) = throw UnsupportedOperationException()
        override suspend fun getWordTextsForDecks(
            deckIds: List<String>,
            language: String,
            allowNSFW: Boolean,
            minDifficulty: Int,
            maxDifficulty: Int,
            categories: List<String>,
            hasCategories: Int,
            classes: List<String>,
            hasClasses: Int,
        ): List<String> = throw UnsupportedOperationException()

        override suspend fun getWordBriefsForDecks(
            deckIds: List<String>,
            language: String,
            allowNSFW: Boolean,
            minDifficulty: Int,
            maxDifficulty: Int,
            categories: List<String>,
            hasCategories: Int,
            classes: List<String>,
            hasClasses: Int,
        ): List<WordBrief> = throw UnsupportedOperationException()

        override suspend fun getAvailableCategories(deckIds: List<String>, language: String, allowNSFW: Boolean): List<String> =
            throw UnsupportedOperationException()

        override suspend fun getAvailableWordClasses(deckIds: List<String>, language: String, allowNSFW: Boolean): List<String> =
            availableWordClasses

        override suspend fun getWordClassCounts(deckId: String): List<WordClassCount> = throw UnsupportedOperationException()
        override suspend fun getDeckCategories(deckId: String): List<String> = throw UnsupportedOperationException()
        override suspend fun getRandomWordSamples(deckId: String, limit: Int): List<String> = throw UnsupportedOperationException()
        override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> = throw UnsupportedOperationException()
        override suspend fun getRecentWords(deckId: String, limit: Int): List<String> = throw UnsupportedOperationException()
    }

    private class FakeDeckDao : DeckDao {
        override suspend fun insertDecks(decks: List<DeckEntity>) = throw UnsupportedOperationException()
        override fun getDecks(): Flow<List<DeckEntity>> = MutableStateFlow(emptyList())
        override suspend fun deleteDeck(deckId: String) = throw UnsupportedOperationException()
        override suspend fun deleteAll() = Unit
    }

    private class FakeTurnHistoryDao : TurnHistoryDao {
        override suspend fun insertAll(entries: List<com.example.alias.data.db.TurnHistoryEntity>) = throw UnsupportedOperationException()
        override fun getRecent(limit: Int): Flow<List<com.example.alias.data.db.TurnHistoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun deleteAll() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val flow = MutableStateFlow(Settings())
        override val settings: Flow<Settings> = flow

        override suspend fun updateRoundSeconds(value: Int) {
            flow.value = flow.value.copy(roundSeconds = value)
        }
        override suspend fun updateTargetWords(value: Int) {
            flow.value = flow.value.copy(targetWords = value)
        }
        override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) {
            flow.value = flow.value.copy(maxSkips = maxSkips, penaltyPerSkip = penaltyPerSkip)
        }
        override suspend fun updatePunishSkips(value: Boolean) {
            flow.value = flow.value.copy(punishSkips = value)
        }
        override suspend fun updateLanguagePreference(language: String) {
            flow.value = flow.value.copy(languagePreference = language)
        }
        override suspend fun setEnabledDeckIds(ids: Set<String>) {
            flow.value = flow.value.copy(enabledDeckIds = ids)
        }
        override suspend fun updateAllowNSFW(value: Boolean) {
            flow.value = flow.value.copy(allowNSFW = value)
        }
        override suspend fun updateStemmingEnabled(value: Boolean) {
            flow.value = flow.value.copy(stemmingEnabled = value)
        }
        override suspend fun updateHapticsEnabled(value: Boolean) {
            flow.value = flow.value.copy(hapticsEnabled = value)
        }
        override suspend fun updateSoundEnabled(value: Boolean) {
            flow.value = flow.value.copy(soundEnabled = value)
        }
        override suspend fun updateOneHandedLayout(value: Boolean) {
            flow.value = flow.value.copy(oneHandedLayout = value)
        }
        override suspend fun updateOrientation(value: String) {
            flow.value = flow.value.copy(orientation = value)
        }
        override suspend fun updateUiLanguage(language: String) {
            flow.value = flow.value.copy(uiLanguage = language)
        }
        override suspend fun updateDifficultyFilter(min: Int, max: Int) {
            flow.value = flow.value.copy(minDifficulty = min, maxDifficulty = max)
        }
        override suspend fun setCategoriesFilter(categories: Set<String>) {
            flow.value = flow.value.copy(selectedCategories = categories)
        }
        override suspend fun setWordClassesFilter(classes: Set<String>) {
            flow.value = flow.value.copy(selectedWordClasses = classes)
        }
        override suspend fun setTeams(teams: List<String>) {
            flow.value = flow.value.copy(teams = teams)
        }
        override suspend fun updateVerticalSwipes(value: Boolean) {
            flow.value = flow.value.copy(verticalSwipes = value)
        }
        override suspend fun setTrustedSources(origins: Set<String>) {
            flow.value = flow.value.copy(trustedSources = origins)
        }
        override suspend fun readBundledDeckHashes(): Set<String> = emptySet()
        override suspend fun writeBundledDeckHashes(entries: Set<String>) = Unit
        override suspend fun readDeletedBundledDeckIds(): Set<String> = emptySet()
        override suspend fun addDeletedBundledDeckId(deckId: String) = Unit
        override suspend fun removeDeletedBundledDeckId(deckId: String) = Unit
        override suspend fun updateSeenTutorial(value: Boolean) {
            flow.value = flow.value.copy(seenTutorial = value)
        }
        override suspend fun clearAll() {
            flow.value = Settings()
        }
    }
}
