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
}
