package com.example.alias.data

import androidx.room.withTransaction
import com.example.alias.data.db.AliasDatabase
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.WordDao
import com.example.alias.data.pack.PackParser
import kotlinx.coroutines.flow.Flow

/** Repository responsible for deck persistence and import. */
interface DeckRepository {
    /** Stream of all decks stored locally. */
    fun getDecks(): Flow<List<DeckEntity>>

    /** Parse [content] as a JSON pack and store it. */
    suspend fun importJson(content: String)
}

class DeckRepositoryImpl(
    private val db: AliasDatabase,
    private val deckDao: DeckDao,
    private val wordDao: WordDao
) : DeckRepository {
    override fun getDecks(): Flow<List<DeckEntity>> = deckDao.getDecks()

    override suspend fun importJson(content: String) {
        val parsed = PackParser.fromJson(content)
        db.withTransaction {
            deckDao.insertDecks(listOf(parsed.deck))
            wordDao.insertWords(parsed.words)
        }
    }
}
