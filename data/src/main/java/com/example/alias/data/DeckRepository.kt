package com.example.alias.data

import androidx.room.withTransaction
import com.example.alias.data.db.AliasDatabase
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.WordDao
import com.example.alias.data.pack.PackParser
import com.example.alias.data.pack.ParsedPack
import kotlinx.coroutines.flow.Flow

/** Repository responsible for deck persistence and import. */
interface DeckRepository {
    /** Stream of all decks stored locally. */
    fun getDecks(): Flow<List<DeckEntity>>

    /** Get number of words in a deck. */
    suspend fun getWordCount(deckId: String): Int

    /** Parse [content] as a JSON pack and store it. */
    suspend fun importJson(content: String)

    /** Store a pre-parsed [pack]. */
    suspend fun importPack(pack: ParsedPack)
}

class DeckRepositoryImpl(
    private val db: AliasDatabase,
    private val deckDao: DeckDao,
    private val wordDao: WordDao
) : DeckRepository {
    override fun getDecks(): Flow<List<DeckEntity>> = deckDao.getDecks()

    override suspend fun getWordCount(deckId: String): Int = wordDao.getWordCount(deckId)

    override suspend fun importJson(content: String) {
        importPack(PackParser.fromJson(content))
    }

    override suspend fun importPack(pack: ParsedPack) {
        db.withTransaction {
            deckDao.insertDecks(listOf(pack.deck))
            wordDao.insertWords(pack.words)
        }
    }
}
