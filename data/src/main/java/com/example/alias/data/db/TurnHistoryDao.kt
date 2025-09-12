package com.example.alias.data.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface TurnHistoryDao {
    @Insert
    suspend fun insertAll(entries: List<TurnHistoryEntity>)
}

