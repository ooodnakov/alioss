package com.example.alias.ui.decks

import android.net.Uri
import com.example.alias.MainViewModel.DeckDownloadProgress
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.settings.Settings
import kotlinx.coroutines.flow.StateFlow

/** Abstraction of the data and commands needed by deck-related screens. */
interface DecksScreenViewModel {
    val decks: StateFlow<List<DeckEntity>>
    val enabledDeckIds: StateFlow<Set<String>>
    val trustedSources: StateFlow<Set<String>>
    val settings: StateFlow<Settings>
    val deckDownloadProgress: StateFlow<DeckDownloadProgress?>
    val availableCategories: StateFlow<List<String>>
    val availableWordClasses: StateFlow<List<String>>

    fun importDeckFromFile(uri: Uri)
    fun updateDifficultyFilter(min: Int, max: Int)
    fun updateDeckLanguagesFilter(languages: Set<String>)
    fun updateCategoriesFilter(categories: Set<String>)
    fun updateWordClassesFilter(wordClasses: Set<String>)
    fun downloadPackFromUrl(url: String, expectedSha256: String?)
    fun removeTrustedSource(entry: String)
    fun addTrustedSource(entry: String)
    fun restoreDeletedBundledDeck(deckId: String)
    fun restoreDeletedImportedDeck(deckId: String)
    fun permanentlyDeleteImportedDeck(deck: DeckEntity)
    fun permanentlyDeleteImportedDeck(deckId: String)
    fun setAllDecksEnabled(enableAll: Boolean)
    fun setDeckEnabled(id: String, enabled: Boolean)
    fun deleteDeck(deck: DeckEntity)

    suspend fun getWordCount(deckId: String): Int
    suspend fun getDeckCategories(deckId: String): List<String>
    suspend fun getDeckWordClassCounts(deckId: String): List<WordClassCount>
    suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket>
    suspend fun getDeckRecentWords(deckId: String, limit: Int = 8): List<String>
    suspend fun getDeckWordSamples(deckId: String, limit: Int = 5): List<String>
}
