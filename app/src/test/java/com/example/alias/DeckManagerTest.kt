package com.example.alias

import android.app.Application
import android.content.Context
import com.example.alias.data.DeckRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.alias.testing.fakePngBytes
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64
import kotlin.text.Charsets

class DeckManagerTest {
    private lateinit var context: Context
    private lateinit var deckManager: DeckManager
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var wordDao: FakeWordDao
    private lateinit var deckRepository: FakeDeckRepository
    private lateinit var turnHistoryDao: FakeTurnHistoryDao
    private lateinit var bundledDeckProvider: FakeBundledDeckProvider
    private lateinit var logger: FakeDeckManagerLogger

    @Before
    fun setUp() {
        context = Application()
        settingsRepository = FakeSettingsRepository()
        wordDao = FakeWordDao()
        deckRepository = FakeDeckRepository(wordDao)
        bundledDeckProvider = FakeBundledDeckProvider()
        logger = FakeDeckManagerLogger()
        turnHistoryDao = FakeTurnHistoryDao(wordDao)
        deckManager = DeckManager(
            context = context,
            deckRepository = deckRepository,
            wordDao = wordDao,
            deckDao = FakeDeckDao(),
            turnHistoryDao = turnHistoryDao,
            settingsRepository = settingsRepository,
            downloader = PackDownloader(OkHttpClient(), settingsRepository),
            bundledDeckProvider = bundledDeckProvider,
            logger = logger,
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
        val key = DeckManager.WordClassAvailabilityKey(setOf("deck"), allowNSFW = false, languages = emptySet())

        val classes = deckManager.loadAvailableWordClasses(key)

        assertEquals(listOf("ADJ", "VERB", "UNKNOWN"), classes)
    }

    @Test
    fun buildWordQueryFiltersSetsFlagsBasedOnSelections() {
        val settings = Settings(
            enabledDeckIds = setOf("a", "b"),
            selectedCategories = setOf("party", "  ", " game "),
            selectedWordClasses = setOf("verb"),
            selectedDeckLanguages = setOf("en"),
            allowNSFW = true,
            minDifficulty = 2,
            maxDifficulty = 4,
        )

        val filters = deckManager.buildWordQueryFilters(settings)

        assertEquals(listOf("a", "b"), filters.deckIds)
        assertEquals(1, filters.categoryFilterEnabled)
        assertEquals(1, filters.wordClassFilterEnabled)
        assertEquals(listOf("party", "game"), filters.categories)
        assertEquals(listOf("VERB"), filters.wordClasses)
        assertEquals(listOf("en"), filters.languages)
        assertEquals(1, filters.languageFilterEnabled)
    }

    @Test
    fun loadWordsRespectsLanguageFilter() = runBlocking {
        wordDao.setDeckWords("english", language = "en", isNsfw = false, words = listOf("apple"))
        wordDao.setDeckWords("french", language = "fr", isNsfw = false, words = listOf("pomme"))

        val settings = Settings(
            enabledDeckIds = linkedSetOf("english", "french"),
            selectedDeckLanguages = setOf("en"),
        )

        val filters = deckManager.buildWordQueryFilters(settings)
        val words = deckManager.loadWords(filters)

        assertEquals(listOf("apple"), words)
    }

    @Test
    fun buildWordQueryFiltersDisablesFiltersWhenSelectionsEmpty() {
        val settings = Settings(
            enabledDeckIds = setOf("deck"),
            selectedCategories = emptySet(),
            selectedWordClasses = emptySet(),
        )

        val filters = deckManager.buildWordQueryFilters(settings)

        assertEquals(0, filters.categoryFilterEnabled)
        assertEquals(0, filters.wordClassFilterEnabled)
        assertNull(filters.categories)
        assertNull(filters.wordClasses)
    }

    @Test
    fun buildWordQueryFiltersDisablesCategoryFilterWhenSelectionsBlank() {
        val settings = Settings(
            enabledDeckIds = setOf("deck"),
            selectedCategories = setOf("   "),
            selectedWordClasses = emptySet(),
        )

        val filters = deckManager.buildWordQueryFilters(settings)

        assertEquals(0, filters.categoryFilterEnabled)
        assertNull(filters.categories)
    }

    @Test
    fun getDeckRecentWordsFiltersByDeckHistory() = runBlocking {
        wordDao.setDeckWords("alpha", language = "en", isNsfw = false, words = listOf("apple", "banana"))
        wordDao.setDeckWords("beta", language = "en", isNsfw = false, words = listOf("durian"))
        turnHistoryDao.setHistory(
            historyEntry(id = 1, word = "apple"),
            historyEntry(id = 2, word = "durian"),
            historyEntry(id = 3, word = "banana"),
            historyEntry(id = 4, word = "kiwi"),
        )

        val result = deckManager.getDeckRecentWords("alpha", limit = 2)

        assertEquals(listOf("banana", "apple"), result)
    }

    @Test
    fun prepareInitialLoadImportsAllDecksOnFirstRun() = runBlocking {
        val alphaJson = sampleDeckJson(id = "alpha", words = listOf("apple"))
        val betaJson = sampleDeckJson(id = "beta", words = listOf("banana"))
        bundledDeckProvider.assets["alpha.json"] = alphaJson
        bundledDeckProvider.assets["beta.json"] = betaJson

        val result = deckManager.prepareInitialLoad()

        val deckIds = deckRepository.getDecks().first().map { it.id }.toSet()
        assertEquals(2, deckRepository.importedPacks.size)
        assertEquals(setOf("alpha", "beta"), deckIds)
        assertEquals(setOf("alpha", "beta"), result.settings.enabledDeckIds)
        assertTrue(result.words.containsAll(listOf("apple", "banana")))
        val expectedHashes = setOf(
            "alpha.json:${sha256(alphaJson)}",
            "beta.json:${sha256(betaJson)}",
            "alpha:${sha256(alphaJson)}",
            "beta:${sha256(betaJson)}",
        )
        assertEquals(expectedHashes, settingsRepository.bundledDeckHashes)
    }

    @Test
    fun prepareInitialLoadReimportsUpdatedDeckWhenHashChanges() = runBlocking {
        val oldContent = sampleDeckJson(id = "alpha", version = 1, words = listOf("old"))
        settingsRepository.seedBundledDeckHashes(
            setOf("alpha.json:${sha256(oldContent)}", "alpha:${sha256(oldContent)}"),
        )
        deckRepository.setDecks(
            listOf(
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
            ),
        )
        wordDao.setDeckWords("alpha", language = "en", isNsfw = false, words = listOf("old"))

        val newContent = sampleDeckJson(id = "alpha", version = 2, words = listOf("new"))
        bundledDeckProvider.assets["alpha.json"] = newContent

        deckManager.prepareInitialLoad()

        assertEquals(1, deckRepository.importedPacks.size)
        assertEquals("alpha", deckRepository.importedPacks.single().deck.id)
        assertEquals(
            setOf("alpha.json:${sha256(newContent)}", "alpha:${sha256(newContent)}"),
            settingsRepository.bundledDeckHashes,
        )
    }

    @Test
    fun prepareInitialLoadPrunesRemovedBundledDecks() = runBlocking {
        val remaining = DeckEntity(
            id = "alpha",
            name = "Alpha",
            language = "en",
            isOfficial = true,
            isNSFW = false,
            version = 1,
            updatedAt = 0L,
            coverImageBase64 = null,
        )
        val removed = remaining.copy(id = "removed", name = "Removed")
        deckRepository.setDecks(listOf(remaining, removed))
        settingsRepository.seedBundledDeckHashes(setOf("removed:oldhash", "alpha:oldhash"))
        bundledDeckProvider.assets["alpha.json"] = sampleDeckJson(id = "alpha", words = listOf("keep"))

        deckManager.prepareInitialLoad()

        assertTrue("removed deck should be pruned", deckRepository.deletedDeckIds.contains("removed"))
        val finalDeckIds = deckRepository.getDecks().first().map { it.id }.toSet()
        assertEquals(setOf("alpha"), finalDeckIds)
    }

    @Test
    fun prepareInitialLoadSkipsUserDeletedBundledDecks() = runBlocking {
        val alphaJson = sampleDeckJson(id = "alpha", words = listOf("alphaWord"))
        val betaJson = sampleDeckJson(id = "beta", words = listOf("betaWord"))
        bundledDeckProvider.assets["alpha.json"] = alphaJson
        bundledDeckProvider.assets["beta.json"] = betaJson
        settingsRepository.setDeletedBundledDeckIds(setOf("alpha"))

        val result = deckManager.prepareInitialLoad()

        val importedIds = deckRepository.importedPacks.map { it.deck.id }.toSet()
        assertEquals(setOf("beta"), importedIds)
        assertEquals(setOf("beta"), result.settings.enabledDeckIds)
        assertTrue(result.words.contains("betaWord"))
        assertTrue(result.words.none { it == "alphaWord" })
    }

    @Test
    fun importPackFromJsonDownloadsCoverImageUrl() = runBlocking {
        withHttpsServer { server, origin, client ->
            val imageBytes = fakePngBytes(width = 512, height = 512, totalSize = 3 * 1024 * 1024)
            assertTrue(imageBytes.size > 1_000_000)
            val buffer = Buffer().write(imageBytes)
            server.enqueue(MockResponse().setResponseCode(200).setBody(buffer))
            val httpsUrl = server.url("/cover.png").toString().replace("http://", "https://")
            settingsRepository.setTrustedSources(setOf(origin, "localhost"))
            val downloader = PackDownloader(client, settingsRepository)
            val manager = DeckManager(
                context = context,
                deckRepository = deckRepository,
                wordDao = wordDao,
                deckDao = FakeDeckDao(),
                turnHistoryDao = turnHistoryDao,
                settingsRepository = settingsRepository,
                downloader = downloader,
                bundledDeckProvider = bundledDeckProvider,
                logger = logger,
            )

            val json = sampleDeckJson(id = "cover_remote", words = listOf("word"), coverImageUrl = httpsUrl)

            val result = manager.importPackFromJson(json)

            assertEquals("cover_remote", result.deckId)
            assertEquals(null, result.coverImageError)
            val imported = deckRepository.importedPacks.last()
            val storedBase64 = requireNotNull(imported.deck.coverImageBase64)
            val decoded = Base64.getDecoder().decode(storedBase64)
            assertArrayEquals(imageBytes, decoded)
        }
    }

    private fun historyEntry(id: Long, word: String): TurnHistoryEntity =
        TurnHistoryEntity(
            id = id,
            team = "team",
            word = word,
            correct = true,
            skipped = false,
            difficulty = null,
            timestamp = id * 1_000L,
        )

    private fun sampleDeckJson(
        id: String,
        language: String = "en",
        name: String = id.uppercase(),
        version: Int = 1,
        words: List<String>,
        coverImageUrl: String? = null,
        author: String? = "Alias Contributors",
    ): String {
        val wordsJson = words.joinToString(separator = ",") { word ->
            """{"text":"$word","difficulty":1}"""
        }
        val authorLine = author?.let { ",\n                \"author\":\"$it\"" } ?: ""
        val coverLine = coverImageUrl?.let { ",\n                \"coverImageUrl\":\"$it\"" } ?: ""
        return """
            {
              "format":"alias-deck@1",
              "deck":{
                "id":"$id",
                "name":"$name",
                "language":"$language"$authorLine,
                "isNSFW":false,
                "version":$version,
                "updatedAt":0$coverLine
              },
              "words":[ $wordsJson ]
            }
        """.trimIndent()
    }
    private suspend fun withHttpsServer(
        block: suspend (server: MockWebServer, origin: String, client: OkHttpClient) -> Unit,
    ) {
        val localhostCert = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverCerts = HandshakeCertificates.Builder().heldCertificate(localhostCert).build()
        val clientCerts = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCert.certificate)
            .build()
        val server = MockWebServer()
        try {
            server.useHttps(serverCerts.sslSocketFactory(), false)
            server.start()
            val client = OkHttpClient.Builder()
                .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
                .build()
            val origin = "https://localhost:${'$'}{server.port}"
            block(server, origin, client)
        } finally {
            server.shutdown()
        }
    }

    private fun sha256(content: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private class FakeBundledDeckProvider : BundledDeckProvider {
        val assets = linkedMapOf<String, String>()

        override fun listBundledDeckFiles(): List<String> = assets.keys.toList()

        override fun readDeckAsset(fileName: String): String? = assets[fileName]
    }

    private class FakeDeckManagerLogger : DeckManagerLogger {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        override fun debug(message: String) = Unit

        override fun warn(message: String, error: Throwable?) {
            warnings += message
        }

        override fun error(message: String, error: Throwable?) {
            errors += message
        }
    }

    private class FakeDeckRepository(
        private val wordDao: FakeWordDao,
        initialDecks: List<DeckEntity> = emptyList(),
    ) : DeckRepository {
        private val decksFlow = MutableStateFlow(initialDecks)
        val importedPacks = mutableListOf<ParsedPack>()
        val deletedDeckIds = mutableListOf<String>()

        override fun getDecks(): Flow<List<DeckEntity>> = decksFlow

        override suspend fun getWordCount(deckId: String): Int = throw UnsupportedOperationException()

        override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> =
            throw UnsupportedOperationException()

        override suspend fun getRecentWords(deckId: String, limit: Int): List<String> =
            throw UnsupportedOperationException()

        override suspend fun importJson(content: String) = throw UnsupportedOperationException()

        override suspend fun importPack(pack: ParsedPack) {
            importedPacks += pack
            wordDao.setDeckWords(
                pack.deck.id,
                pack.deck.language,
                pack.deck.isNSFW,
                pack.words.map { it.text },
            )
            decksFlow.value = decksFlow.value.filterNot { it.id == pack.deck.id } + pack.deck
        }

        override suspend fun deleteDeck(deckId: String) {
            deletedDeckIds += deckId
            wordDao.removeDeck(deckId)
            decksFlow.value = decksFlow.value.filterNot { it.id == deckId }
        }

        fun setDecks(decks: List<DeckEntity>) {
            decksFlow.value = decks
        }
    }

    private class FakeWordDao : WordDao {
        var availableWordClasses: List<String> = emptyList()
        private val deckWords = mutableMapOf<String, DeckWords>()

        data class DeckWords(val language: String, val words: List<String>, val isNsfw: Boolean)

        fun setDeckWords(deckId: String, language: String, isNsfw: Boolean, words: List<String>) {
            deckWords[deckId] = DeckWords(language, words, isNsfw)
        }

        fun removeDeck(deckId: String) {
            deckWords.remove(deckId)
        }

        fun wordsForDeck(deckId: String): Set<String> = deckWords[deckId]?.words?.toSet() ?: emptySet()

        override suspend fun insertWords(words: List<com.example.alias.data.db.WordEntity>) =
            throw UnsupportedOperationException()

        override suspend fun insertWordClasses(entries: List<com.example.alias.data.db.WordClassEntity>) =
            throw UnsupportedOperationException()

        override suspend fun getWordTexts(deckId: String): List<String> =
            throw UnsupportedOperationException()

        override suspend fun getWordCount(deckId: String): Int =
            throw UnsupportedOperationException()

        override suspend fun deleteByDeck(deckId: String) =
            throw UnsupportedOperationException()

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
            val allowedLanguages = if (hasLanguages == 0) emptySet() else languages.toSet()
            return deckIds.flatMap { id ->
                val info = deckWords[id]
                if (info == null) {
                    emptyList()
                } else if (!allowNSFW && info.isNsfw) {
                    emptyList()
                } else if (hasLanguages == 1 && !allowedLanguages.contains(info.language)) {
                    emptyList()
                } else {
                    info.words
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
        ): List<WordBrief> =
            throw UnsupportedOperationException()

        override suspend fun getAvailableCategories(
            deckIds: List<String>,
            allowNSFW: Boolean,
            languages: List<String>,
            hasLanguages: Int,
        ): List<String> =
            throw UnsupportedOperationException()

        override suspend fun getAvailableWordClasses(
            deckIds: List<String>,
            allowNSFW: Boolean,
            languages: List<String>,
            hasLanguages: Int,
        ): List<String> =
            availableWordClasses

        override suspend fun getWordClassCounts(deckId: String): List<WordClassCount> =
            throw UnsupportedOperationException()

        override suspend fun getDeckCategories(deckId: String): List<String> =
            throw UnsupportedOperationException()

        override suspend fun getRandomWordSamples(deckId: String, limit: Int): List<String> =
            throw UnsupportedOperationException()

        override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> =
            throw UnsupportedOperationException()

        override suspend fun getRecentWords(deckId: String, limit: Int): List<String> =
            throw UnsupportedOperationException()
    }

    private class FakeDeckDao : DeckDao {
        override suspend fun insertDecks(decks: List<DeckEntity>) =
            throw UnsupportedOperationException()

        override fun getDecks(): Flow<List<DeckEntity>> = MutableStateFlow(emptyList())

        override suspend fun deleteDeck(deckId: String) =
            throw UnsupportedOperationException()

        override suspend fun deleteAll() = Unit
    }

    private class FakeTurnHistoryDao(
        private val wordDao: FakeWordDao,
    ) : TurnHistoryDao {
        private val history = mutableListOf<TurnHistoryEntity>()

        fun setHistory(vararg entries: TurnHistoryEntity) {
            history.clear()
            history.addAll(entries.toList())
        }

        override suspend fun insertAll(entries: List<TurnHistoryEntity>) {
            history += entries
        }

        override fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>> =
            MutableStateFlow(history.asReversed().take(limit))

        override suspend fun getRecentWordsForDeck(deckId: String, limit: Int): List<String> {
            if (limit <= 0) return emptyList()
            val allowed = wordDao.wordsForDeck(deckId)
            if (allowed.isEmpty()) return emptyList()
            return history
                .asReversed()
                .asSequence()
                .map { it.word }
                .filter { allowed.contains(it) }
                .take(limit)
                .toList()
        }

        override suspend fun deleteAll() {
            history.clear()
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val flow = MutableStateFlow(Settings())
        override val settings: Flow<Settings> = flow
        var bundledDeckHashes: Set<String> = emptySet()
        private var deletedBundledDeckIds: MutableSet<String> = mutableSetOf()

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

        override suspend fun updateSeenTutorial(value: Boolean) {
            flow.value = flow.value.copy(seenTutorial = value)
        }

        override suspend fun readDeletedImportedDeckIds(): Set<String> = emptySet()

        override suspend fun addDeletedImportedDeckId(deckId: String) = Unit

        override suspend fun removeDeletedImportedDeckId(deckId: String) = Unit

        override suspend fun clearAll() {
            bundledDeckHashes = emptySet()
            deletedBundledDeckIds.clear()
            flow.value = Settings()
        }

        fun seedBundledDeckHashes(entries: Set<String>) {
            bundledDeckHashes = entries
        }

        fun setDeletedBundledDeckIds(ids: Set<String>) {
            deletedBundledDeckIds = ids.toMutableSet()
            flow.value = flow.value.copy(deletedBundledDeckIds = ids)
        }
    }
}
