package com.example.alias.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Query("SELECT * FROM decks")
    fun getDecks(): Flow<List<DeckEntity>>
}
