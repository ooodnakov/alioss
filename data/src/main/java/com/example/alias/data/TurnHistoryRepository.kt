package com.example.alias.data

import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.TurnHistoryEntity
import kotlinx.coroutines.flow.Flow

interface TurnHistoryRepository {
    suspend fun save(entries: List<TurnHistoryEntity>)
    fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>>
    suspend fun clear()
}

class TurnHistoryRepositoryImpl(
    private val dao: TurnHistoryDao,
) : TurnHistoryRepository {
    override suspend fun save(entries: List<TurnHistoryEntity>) {
        dao.insertAll(entries)
    }

    override fun getRecent(limit: Int): Flow<List<TurnHistoryEntity>> = dao.getRecent(limit)

    override suspend fun clear() {
        dao.deleteAll()
    }
}
