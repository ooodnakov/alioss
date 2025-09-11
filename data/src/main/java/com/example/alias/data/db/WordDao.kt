package com.example.alias.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("SELECT text FROM words WHERE deckId = :deckId")
    suspend fun getWordTexts(deckId: String): List<String>

    @Query(
        "SELECT text FROM words " +
            "WHERE deckId IN (:deckIds) " +
            "AND language = :language " +
            "AND (:allowNSFW = 1 OR isNSFW = 0)"
    )
    suspend fun getWordTextsForDecks(
        deckIds: List<String>,
        language: String,
        allowNSFW: Boolean,
    ): List<String>
}
