package com.example.alias.data

import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.TurnHistoryEntity

interface TurnHistoryRepository {
    suspend fun save(entries: List<TurnHistoryEntity>)
}

class TurnHistoryRepositoryImpl(
    private val dao: TurnHistoryDao,
) : TurnHistoryRepository {
    override suspend fun save(entries: List<TurnHistoryEntity>) {
        dao.insertAll(entries)
    }
}

