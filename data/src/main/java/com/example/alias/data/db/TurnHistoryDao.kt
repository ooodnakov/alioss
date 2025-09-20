package com.example.alias.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnHistoryDao {
    @Insert
    suspend fun insertAll(entries: List<TurnHistoryEntity>)

    @Query("SELECT * FROM turn_history ORDER BY id DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>>

    @Query(
        "SELECT word FROM turn_history " +
            "WHERE EXISTS (" +
            "    SELECT 1 FROM words " +
            "    WHERE deckId = :deckId AND text = turn_history.word" +
            ") " +
            "ORDER BY id DESC " +
            "LIMIT :limit",
    )
    suspend fun getRecentWordsForDeck(deckId: String, limit: Int): List<String>

    @Query("DELETE FROM turn_history")
    suspend fun deleteAll()
}
