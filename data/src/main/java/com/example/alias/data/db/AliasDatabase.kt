package com.example.alias.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeckEntity::class, WordEntity::class, WordClassEntity::class, TurnHistoryEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class AliasDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao
    abstract fun turnHistoryDao(): TurnHistoryDao
}
