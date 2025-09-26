package com.example.alioss.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeckEntity::class, WordEntity::class, WordClassEntity::class, TurnHistoryEntity::class],
    version = 9,
    exportSchema = true,
)
abstract class AliossDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao
    abstract fun turnHistoryDao(): TurnHistoryDao
}
