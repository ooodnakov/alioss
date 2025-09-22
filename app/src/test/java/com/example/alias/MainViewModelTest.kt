package com.example.alias

import android.app.Application
import com.example.alias.data.DeckRepository
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.db.WordBrief
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.pack.ParsedPack
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var wordDao: TestWordDao
    private lateinit var deckRepository: TestDeckRepository
    private lateinit var deckManager: DeckManager
    private lateinit var settingsRepository: TestSettingsRepository
    private lateinit var settingsController: SettingsController
    private lateinit var gameController: GameController

    @Before
    fun setUp() {
        val context = Application()
        wordDao = TestWordDao().apply {
            setDeckData(
                "alpha",
                TestWordDao.DeckData(
                    language = "en",
                    isNsfw = false,
                    words = listOf("apple"),
                    categories = listOf("Alpha"),
                    wordClasses = listOf("NOUN"),
                    briefs = listOf(WordBrief("apple", 1, "Alpha", "NOUN")),
                ),
            )
            setDeckData(
                "beta",
                TestWordDao.DeckData(
                    language = "en",
                    isNsfw = false,
                    words = listOf("banana"),
                    categories = listOf("Beta"),
                    wordClasses = listOf("VERB"),
                    briefs = listOf(WordBrief("banana", 2, "Beta", "VERB")),
                ),
            )
        }
        deckRepository = TestDeckRepository(
            initialDecks = listOf(
                DeckEntity(
                    id = "alpha",
                    name = "Alpha",
                    language = "en",
                    isOfficial = true,
                    isNSFW = false,
                    version = 1,
                    updatedAt = 0L,
                    coverImageBase64 = null,
                ),
                DeckEntity(
                    id = "beta",
                    name = "Beta",
                    language = "en",
                    isOfficial = true,
                    isNSFW = false,
                    version = 1,
                    updatedAt = 0L,
                    coverImageBase64 = null,
                ),
            ),
        )
        val deckDao = TestDeckDao()
        val turnHistoryDao = TestTurnHistoryDao()
        settingsRepository = TestSettingsRepository(Settings(enabledDeckIds = setOf("alpha")))
        val bundledDeckProvider = EmptyBundledDeckProvider()
        val logger = TestDeckManagerLogger()
        deckManager = DeckManager(
            context = context,
            deckRepository = deckRepository,
            wordDao = wordDao,
            deckDao = deckDao,
            turnHistoryDao = turnHistoryDao,
            settingsRepository = settingsRepository,
            downloader = PackDownloader(OkHttpClient(), settingsRepository),
            bundledDeckProvider = bundledDeckProvider,
            logger = logger,
        )
        settingsController = SettingsController(settingsRepository)
        val historyRepository = TestTurnHistoryRepository()
        val engineFactory = TestGameEngineFactory()
        gameController = GameController(historyRepository, engineFactory)
    }

    @Test
    fun enablingAdditionalDeckRefreshesAvailableCategories() = runTest(dispatcherRule.dispatcher) {
        val vm = MainViewModel(deckManager, settingsController, gameController)
        settingsController.setEnabledDeckIds(setOf("alpha", "beta"))
        val updatedCategories = withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                vm.availableCategories.first { it == listOf("Alpha", "Beta") }
            }
        }

        assertEquals(listOf("Alpha", "Beta"), updatedCategories)
    }

    @Test
    fun togglingCategoryRefreshesAvailableCategories() = runTest(dispatcherRule.dispatcher) {
        val vm = MainViewModel(deckManager, settingsController, gameController)
        wordDao.updateCategories("alpha", listOf("Gamma"))
        wordDao.updateBriefs("alpha", listOf(WordBrief("apple", 1, "Gamma", "NOUN")))

        settingsController.updateCategoriesFilter(setOf("Gamma"))
        val updatedCategories = withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                vm.availableCategories.first { it.size == 1 && it.first() == "Gamma" }
            }
        }

        assertEquals(listOf("Gamma"), updatedCategories)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class TestWordDao : WordDao {
    data class DeckData(
        val language: String,
        val isNsfw: Boolean,
        var words: List<String>,
        var categories: List<String>,
        var wordClasses: List<String>,
        var briefs: List<WordBrief>,
    )

    private val decks = mutableMapOf<String, DeckData>()

    fun setDeckData(id: String, data: DeckData) {
        decks[id] = data
    }

    fun updateCategories(id: String, categories: List<String>) {
        decks[id]?.categories = categories
    }

    fun updateBriefs(id: String, briefs: List<WordBrief>) {
        decks[id]?.briefs = briefs
    }

    override suspend fun insertWords(words: List<com.example.alias.data.db.WordEntity>) =
        throw UnsupportedOperationException()

    override suspend fun insertWordClasses(entries: List<com.example.alias.data.db.WordClassEntity>) =
        throw UnsupportedOperationException()

    override suspend fun getWordTexts(deckId: String): List<String> =
        decks[deckId]?.words ?: emptyList()

    override suspend fun getWordCount(deckId: String): Int =
        decks[deckId]?.words?.size ?: 0

    override suspend fun deleteByDeck(deckId: String) {
        decks.remove(deckId)
    }

    override suspend fun getWordTextsForDecks(
        deckIds: List<String>,
        allowNSFW: Boolean,
        minDifficulty: Int,
        maxDifficulty: Int,
        categories: List<String>?,
        hasCategories: Int,
        classes: List<String>?,
        hasClasses: Int,
        languages: List<String>,
        hasLanguages: Int,
    ): List<String> {
        val allowedLanguages = if (hasLanguages == 1) {
            languages.map { it.lowercase(Locale.ROOT) }.toSet()
        } else {
            emptySet()
        }
        return deckIds.flatMap { id ->
            val deck = decks[id] ?: return@flatMap emptyList()
            if (!allowNSFW && deck.isNsfw) {
                emptyList()
            } else if (hasLanguages == 1 && !allowedLanguages.contains(deck.language.lowercase(Locale.ROOT))) {
                emptyList()
            } else {
                deck.words
            }
        }
    }

    override suspend fun getWordBriefsForDecks(
        deckIds: List<String>,
        allowNSFW: Boolean,
        minDifficulty: Int,
        maxDifficulty: Int,
        categories: List<String>?,
        hasCategories: Int,
        classes: List<String>?,
        hasClasses: Int,
        languages: List<String>,
        hasLanguages: Int,
    ): List<WordBrief> {
        val allowedLanguages = if (hasLanguages == 1) {
            languages.map { it.lowercase(Locale.ROOT) }.toSet()
        } else {
            emptySet()
        }
        return deckIds.flatMap { id ->
            val deck = decks[id] ?: return@flatMap emptyList()
            if (!allowNSFW && deck.isNsfw) {
                emptyList()
            } else if (hasLanguages == 1 && !allowedLanguages.contains(deck.language.lowercase(Locale.ROOT))) {
                emptyList()
            } else {
                deck.briefs
            }
        }
    }

    override suspend fun getAvailableCategories(
        deckIds: List<String>,
        allowNSFW: Boolean,
        languages: List<String>,
        hasLanguages: Int,
    ): List<String> {
        val allowedLanguages = if (hasLanguages == 1) {
            languages.map { it.lowercase(Locale.ROOT) }.toSet()
        } else {
            emptySet()
        }
        return deckIds.flatMap { id ->
            val deck = decks[id] ?: return@flatMap emptyList()
            if (!allowNSFW && deck.isNsfw) {
                emptyList()
            } else if (hasLanguages == 1 && !allowedLanguages.contains(deck.language.lowercase(Locale.ROOT))) {
                emptyList()
            } else {
                deck.categories
            }
        }.filter { it.isNotBlank() }.distinct().sorted()
    }

    override suspend fun getAvailableWordClasses(
        deckIds: List<String>,
        allowNSFW: Boolean,
        languages: List<String>,
        hasLanguages: Int,
    ): List<String> {
        val allowedLanguages = if (hasLanguages == 1) {
            languages.map { it.lowercase(Locale.ROOT) }.toSet()
        } else {
            emptySet()
        }
        return deckIds.flatMap { id ->
            val deck = decks[id] ?: return@flatMap emptyList()
            if (!allowNSFW && deck.isNsfw) {
                emptyList()
            } else if (hasLanguages == 1 && !allowedLanguages.contains(deck.language.lowercase(Locale.ROOT))) {
                emptyList()
            } else {
                deck.wordClasses
            }
        }.map { it.uppercase(Locale.ROOT) }.distinct().sorted()
    }

    override suspend fun getWordClassCounts(deckId: String): List<WordClassCount> =
        throw UnsupportedOperationException()

    override suspend fun getDeckCategories(deckId: String): List<String> =
        decks[deckId]?.categories ?: emptyList()

    override suspend fun getRandomWordSamples(deckId: String, limit: Int): List<String> =
        decks[deckId]?.words?.take(limit) ?: emptyList()

    override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> =
        throw UnsupportedOperationException()

    override suspend fun getRecentWords(deckId: String, limit: Int): List<String> =
        decks[deckId]?.words?.takeLast(limit) ?: emptyList()
}

private class TestDeckRepository(
    initialDecks: List<DeckEntity>,
) : DeckRepository {
    private val decksFlow = MutableStateFlow(initialDecks)

    override fun getDecks(): Flow<List<DeckEntity>> = decksFlow

    override suspend fun getWordCount(deckId: String): Int = throw UnsupportedOperationException()

    override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> =
        throw UnsupportedOperationException()

    override suspend fun getRecentWords(deckId: String, limit: Int): List<String> =
        throw UnsupportedOperationException()

    override suspend fun importJson(content: String) = throw UnsupportedOperationException()

    override suspend fun importPack(pack: ParsedPack) = throw UnsupportedOperationException()

    override suspend fun deleteDeck(deckId: String) = throw UnsupportedOperationException()
}

private class TestDeckDao : DeckDao {
    override suspend fun insertDecks(decks: List<DeckEntity>) = Unit

    override fun getDecks(): Flow<List<DeckEntity>> = MutableStateFlow(emptyList())

    override suspend fun deleteDeck(deckId: String) = Unit

    override suspend fun deleteAll() = Unit
}

private class TestTurnHistoryDao : TurnHistoryDao {
    override suspend fun insertAll(entries: List<TurnHistoryEntity>) = Unit

    override fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>> = MutableStateFlow(emptyList())

    override suspend fun getRecentWordsForDeck(deckId: String, limit: Int): List<String> = emptyList()

    override suspend fun deleteAll() = Unit
}

private class TestSettingsRepository(initial: Settings) : SettingsRepository {
    private val flow = MutableStateFlow(initial)
    override val settings: Flow<Settings> = flow

    private var bundledDeckHashes: Set<String> = emptySet()
    private var deletedBundledDeckIds: MutableSet<String> = mutableSetOf()
    private var deletedImportedDeckIds: MutableSet<String> = mutableSetOf()

    override suspend fun updateRoundSeconds(value: Int) {
        flow.value = flow.value.copy(roundSeconds = value)
    }

    override suspend fun updateTargetWords(value: Int) {
        flow.value = flow.value.copy(targetWords = value)
    }

    override suspend fun updateTargetScore(value: Int) {
        flow.value = flow.value.copy(targetScore = value)
    }

    override suspend fun updateScoreTargetEnabled(value: Boolean) {
        flow.value = flow.value.copy(scoreTargetEnabled = value)
    }

    override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) {
        flow.value = flow.value.copy(maxSkips = maxSkips, penaltyPerSkip = penaltyPerSkip)
    }

    override suspend fun updatePunishSkips(value: Boolean) {
        flow.value = flow.value.copy(punishSkips = value)
    }

    override suspend fun setEnabledDeckIds(ids: Set<String>) {
        flow.value = flow.value.copy(enabledDeckIds = ids)
    }

    override suspend fun removeEnabledDeckId(deckId: String) {
        flow.value = flow.value.copy(enabledDeckIds = flow.value.enabledDeckIds - deckId)
    }

    override suspend fun setDeckLanguagesFilter(languages: Set<String>) {
        flow.value = flow.value.copy(selectedDeckLanguages = languages)
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

    override suspend fun readBundledDeckHashes(): Set<String> = bundledDeckHashes

    override suspend fun writeBundledDeckHashes(entries: Set<String>) {
        bundledDeckHashes = entries
    }

    override suspend fun readDeletedBundledDeckIds(): Set<String> = deletedBundledDeckIds

    override suspend fun addDeletedBundledDeckId(deckId: String) {
        deletedBundledDeckIds += deckId
        flow.value = flow.value.copy(deletedBundledDeckIds = deletedBundledDeckIds.toSet())
    }

    override suspend fun removeDeletedBundledDeckId(deckId: String) {
        deletedBundledDeckIds -= deckId
        flow.value = flow.value.copy(deletedBundledDeckIds = deletedBundledDeckIds.toSet())
    }

    override suspend fun readDeletedImportedDeckIds(): Set<String> = deletedImportedDeckIds

    override suspend fun addDeletedImportedDeckId(deckId: String) {
        deletedImportedDeckIds += deckId
        flow.value = flow.value.copy(deletedImportedDeckIds = deletedImportedDeckIds.toSet())
    }

    override suspend fun removeDeletedImportedDeckId(deckId: String) {
        deletedImportedDeckIds -= deckId
        flow.value = flow.value.copy(deletedImportedDeckIds = deletedImportedDeckIds.toSet())
    }

    override suspend fun updateSeenTutorial(value: Boolean) {
        flow.value = flow.value.copy(seenTutorial = value)
    }

    override suspend fun clearAll() {
        bundledDeckHashes = emptySet()
        deletedBundledDeckIds.clear()
        deletedImportedDeckIds.clear()
        flow.value = Settings()
    }
}

private class EmptyBundledDeckProvider : BundledDeckProvider {
    override fun listBundledDeckFiles(): List<String> = emptyList()

    override fun readDeckAsset(fileName: String): String? = null
}

private class TestDeckManagerLogger : DeckManagerLogger {
    override fun debug(message: String) = Unit

    override fun warn(message: String, error: Throwable?) = Unit

    override fun error(message: String, error: Throwable?) = Unit
}

private class TestTurnHistoryRepository : TurnHistoryRepository {
    private val history = MutableStateFlow<List<TurnHistoryEntity>>(emptyList())

    override suspend fun save(entries: List<TurnHistoryEntity>) {
        history.value = history.value + entries
    }

    override fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>> = history

    override suspend fun clear() {
        history.value = emptyList()
    }
}

private class TestGameEngineFactory : GameEngineFactory {
    override fun create(words: List<String>, scope: kotlinx.coroutines.CoroutineScope): GameEngine {
        return FakeGameEngine()
    }
}

private class FakeGameEngine : GameEngine {
    private val stateFlow = MutableStateFlow<GameState>(GameState.Idle)
    override val state: StateFlow<GameState> = stateFlow.asStateFlow()

    override suspend fun startMatch(config: com.example.alias.domain.MatchConfig, teams: List<String>, seed: Long) = Unit

    override suspend fun correct() = Unit

    override suspend fun skip() = Unit

    override suspend fun nextTurn() = Unit

    override suspend fun startTurn() = Unit

    override suspend fun overrideOutcome(index: Int, correct: Boolean) = Unit

    override suspend fun peekNextWord(): String? = null
}
