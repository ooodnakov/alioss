package com.example.alias.data

import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.WordBrief
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordClassEntity
import com.example.alias.data.db.WordDao
import com.example.alias.data.db.WordEntity
import com.example.alias.data.pack.ParsedPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DeckRepositoryTest {

    private lateinit var deckDao: FakeDeckDao
    private lateinit var wordDao: FakeWordDao
    private lateinit var repository: DeckRepository

    @Before
    fun setUp() {
        deckDao = FakeDeckDao()
        wordDao = FakeWordDao()
        repository = DeckRepositoryImpl(
            deckDao = deckDao,
            wordDao = wordDao,
            transactionRunner = { action -> action() },
        )
    }

    @Test
    fun importing_same_pack_twice_does_not_duplicate_words() = runBlocking {
        val pack = createPack(
            deckId = "test_deck",
            words = listOf(
                WordSpec(text = "Alpha"),
                WordSpec(text = "Beta"),
            ),
        )

        repository.importPack(pack)
        repository.importPack(pack)

        val storedWords = wordDao.getWordTexts("test_deck")
        assertEquals(2, storedWords.size)
        assertEquals(setOf("Alpha", "Beta"), storedWords.toSet())
        assertEquals(2, wordDao.getWordCount("test_deck"))
    }

    @Test
    fun importing_updated_pack_replaces_words_and_classes() = runBlocking {
        val deckId = "test_deck"
        val initialPack = createPack(
            deckId = deckId,
            version = 1,
            words = listOf(WordSpec(text = "Alpha", wordClass = "NOUN")),
        )
        val updatedPack = createPack(
            deckId = deckId,
            version = 2,
            words = listOf(WordSpec(text = "Gamma", wordClass = "VERB")),
        )

        repository.importPack(initialPack)
        repository.importPack(updatedPack)

        val storedWords = wordDao.getWordTexts(deckId)
        assertEquals(listOf("Gamma"), storedWords)
        val briefs = wordDao.getWordBriefsForDecks(
            deckIds = listOf(deckId),
            allowNSFW = true,
            minDifficulty = 1,
            maxDifficulty = 10,
            categories = emptyList(),
            hasCategories = 0,
            classes = emptyList(),
            hasClasses = 0,
        )
        assertEquals(1, briefs.size)
        val brief = briefs.single()
        assertEquals("Gamma", brief.text)
        assertEquals("VERB", brief.wordClass)
        assertFalse(brief.wordClass?.contains("NOUN") ?: false)
    }

    @Test
    fun deleting_deck_removes_it_and_words() = runBlocking {
        val deckId = "test_deck"
        val pack = createPack(
            deckId = deckId,
            words = listOf(WordSpec(text = "Alpha")),
        )

        repository.importPack(pack)
        assertEquals(1, deckDao.getDecks().first().size)
        assertEquals(1, wordDao.getWordCount(deckId))

        repository.deleteDeck(deckId)

        assertEquals(emptyList(), deckDao.getDecks().first())
        assertEquals(0, wordDao.getWordCount(deckId))
    }

    private fun createPack(
        deckId: String,
        version: Int = 1,
        words: List<WordSpec>,
    ): ParsedPack {
        val deck = DeckEntity(
            id = deckId,
            name = "Test Deck",
            language = "en",
            isOfficial = false,
            isNSFW = false,
            version = version,
            updatedAt = version.toLong(),
        )
        val wordEntities = words.map { spec ->
            WordEntity(
                deckId = deckId,
                text = spec.text,
                language = "en",
                stems = null,
                category = spec.category,
                difficulty = spec.difficulty,
                tabooStems = null,
                isNSFW = false,
            )
        }
        val classEntities = words
            .mapNotNull { spec ->
                spec.wordClass?.let { wordClass ->
                    WordClassEntity(
                        deckId = deckId,
                        wordText = spec.text,
                        wordClass = wordClass,
                    )
                }
            }
        return ParsedPack(deck = deck, words = wordEntities, wordClasses = classEntities)
    }

    private data class WordSpec(
        val text: String,
        val difficulty: Int = 1,
        val category: String? = null,
        val wordClass: String? = null,
    )

    private class FakeDeckDao : DeckDao {
        private val decks = LinkedHashMap<String, DeckEntity>()
        private val decksFlow = MutableStateFlow<List<DeckEntity>>(emptyList())

        override suspend fun insertDecks(decks: List<DeckEntity>) {
            decks.forEach { this.decks[it.id] = it }
            decksFlow.value = this.decks.values.toList()
        }

        override fun getDecks(): Flow<List<DeckEntity>> = decksFlow

        override suspend fun deleteDeck(deckId: String) {
            decks.remove(deckId)
            decksFlow.value = decks.values.toList()
        }

        override suspend fun deleteAll() {
            decks.clear()
            decksFlow.value = emptyList()
        }
    }

    private class FakeWordDao : WordDao {
        private val words = mutableListOf<WordEntity>()
        private val wordClassEntries = mutableListOf<WordClassEntity>()
        private var nextWordId = 1L

        override suspend fun insertWords(words: List<WordEntity>) {
            words.forEach { newWord ->
                val index = this.words.indexOfFirst {
                    it.deckId == newWord.deckId && it.text == newWord.text
                }
                if (index != -1) {
                    val existingId = this.words[index].id
                    this.words[index] = newWord.copy(id = existingId)
                } else {
                    this.words.add(newWord.copy(id = nextWordId++))
                }
            }
        }

        override suspend fun insertWordClasses(entries: List<WordClassEntity>) {
            entries.forEach { newEntry ->
                val index = wordClassEntries.indexOfFirst {
                    it.deckId == newEntry.deckId &&
                        it.wordText == newEntry.wordText &&
                        it.wordClass == newEntry.wordClass
                }
                if (index != -1) {
                    wordClassEntries[index] = newEntry
                } else {
                    wordClassEntries.add(newEntry)
                }
            }
        }

        override suspend fun getWordTexts(deckId: String): List<String> =
            words.filter { it.deckId == deckId }.map { it.text }

        override suspend fun getWordCount(deckId: String): Int =
            words.count { it.deckId == deckId }

        override suspend fun deleteByDeck(deckId: String) {
            words.removeAll { it.deckId == deckId }
            wordClassEntries.removeAll { it.deckId == deckId }
        }

        override suspend fun getDifficultyHistogram(deckId: String): List<DifficultyBucket> =
            words.filter { it.deckId == deckId }
                .groupBy { it.difficulty }
                .entries
                .map { (difficulty, entries) ->
                    DifficultyBucket(difficulty = difficulty, count = entries.size)
                }
                .sortedBy { it.difficulty }

        override suspend fun getRecentWords(deckId: String, limit: Int): List<String> =
            words.filter { it.deckId == deckId }
                .sortedByDescending { it.id }
                .take(limit)
                .map { it.text }

        override suspend fun getWordTextsForDecks(
            deckIds: List<String>,
            allowNSFW: Boolean,
            minDifficulty: Int,
            maxDifficulty: Int,
            categories: List<String>?,
            hasCategories: Int,
            classes: List<String>?,
            hasClasses: Int,
        ): List<String> = getWordBriefsForDecks(
            deckIds = deckIds,
            allowNSFW = allowNSFW,
            minDifficulty = minDifficulty,
            maxDifficulty = maxDifficulty,
            categories = categories,
            hasCategories = hasCategories,
            classes = classes,
            hasClasses = hasClasses,
        ).map { it.text }

        override suspend fun getWordBriefsForDecks(
            deckIds: List<String>,
            allowNSFW: Boolean,
            minDifficulty: Int,
            maxDifficulty: Int,
            categories: List<String>?,
            hasCategories: Int,
            classes: List<String>?,
            hasClasses: Int,
        ): List<WordBrief> {
            val requiredClasses = classes?.map { it.uppercase() } ?: emptyList()
            return words.filter { word ->
                deckIds.contains(word.deckId) &&
                    (allowNSFW || !word.isNSFW) &&
                    word.difficulty in minDifficulty..maxDifficulty &&
                    (hasCategories == 0 || (word.category != null && categories?.contains(word.category) == true)) &&
                    (hasClasses == 0 || classesForWord(word).any { requiredClasses.contains(it) })
            }.map { word ->
                val joinedClasses = classesForWord(word)
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ",")
                WordBrief(
                    text = word.text,
                    difficulty = word.difficulty,
                    category = word.category,
                    wordClass = joinedClasses,
                )
            }
        }

        private fun classesForWord(word: WordEntity): List<String> =
            wordClassEntries.filter { it.deckId == word.deckId && it.wordText == word.text }
                .map { it.wordClass.uppercase() }
                .distinct()

        override suspend fun getAvailableCategories(
            deckIds: List<String>,
            allowNSFW: Boolean,
        ): List<String> =
            words.filter { deckIds.contains(it.deckId) && (allowNSFW || !it.isNSFW) }
                .mapNotNull { it.category }
                .distinct()

        override suspend fun getAvailableWordClasses(
            deckIds: List<String>,
            allowNSFW: Boolean,
        ): List<String> {
            val relevantWords = words
                .filter {
                    deckIds.contains(it.deckId) &&
                        (allowNSFW || !it.isNSFW)
                }
                .map { it.deckId to it.text }
                .toSet()

            return wordClassEntries
                .filter { (it.deckId to it.wordText) in relevantWords }
                .map { it.wordClass.uppercase() }
                .distinct()
        }

        override suspend fun getWordClassCounts(deckId: String): List<WordClassCount> {
            return wordClassEntries
                .filter { it.deckId == deckId }
                .groupBy { it.wordClass.uppercase() }
                .map { (wordClass, entries) ->
                    WordClassCount(wordClass = wordClass, count = entries.size)
                }
                .sortedBy { it.wordClass }
        }

        override suspend fun getDeckCategories(deckId: String): List<String> {
            return words
                .filter { it.deckId == deckId }
                .mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }

        override suspend fun getRandomWordSamples(deckId: String, limit: Int): List<String> {
            return words
                .filter { it.deckId == deckId }
                .shuffled()
                .take(limit)
                .map { it.text }
        }
    }
}
