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

    @Query("DELETE FROM turn_history")
    suspend fun deleteAll()
}
