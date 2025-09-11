package com.example.alias.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeckEntity::class, WordEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AliasDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao
}
