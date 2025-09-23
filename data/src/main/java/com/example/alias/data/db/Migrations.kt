package com.example.alias.data.db

import androidx.room.migration.Migration

/**
 * Database migrations for AliasDatabase.
 * Each migration handles the transition from one version to the next.
 */

/**
 * Database migrations for AliasDatabase.
 * Each migration handles the transition from one version to the next.
 */

// Migration 1→2: Add turn_history table for game history tracking
val MIGRATION_1_2 = Migration(1, 2) { database ->
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `turn_history` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `team` TEXT NOT NULL,
            `word` TEXT NOT NULL,
            `correct` INTEGER NOT NULL,
            `timestamp` INTEGER NOT NULL
        )
    """,
    )
}

// Migration 2→3: Add indexes to words table for better query performance
val MIGRATION_2_3 = Migration(2, 3) { database ->
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_words_language` ON `words` (`language`)")
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_words_isNSFW` ON `words` (`isNSFW`)")
}

// Migration 3→4: Add composite index for language and deck queries
val MIGRATION_3_4 = Migration(3, 4) { database ->
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_words_language_deckId` ON `words` (`language`, `deckId`)")
}

// Migration 4→5: Add word_classes table and unique constraint on words
val MIGRATION_4_5 = Migration(4, 5) { database ->
    // Create word_classes table for word classification
    database.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `word_classes` (
            `deckId` TEXT NOT NULL,
            `wordText` TEXT NOT NULL,
            `wordClass` TEXT NOT NULL,
            PRIMARY KEY(`deckId`, `wordText`, `wordClass`),
            FOREIGN KEY(`deckId`, `wordText`) REFERENCES `words`(`deckId`, `text`) ON UPDATE CASCADE ON DELETE CASCADE
        )
    """,
    )

    // Add indexes for word_classes table
    database.execSQL("CREATE INDEX IF NOT EXISTS `index_word_classes_wordClass` ON `word_classes` (`wordClass`)")
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_word_classes_deckId_wordText` ON `word_classes` (`deckId`, `wordText`)",
    )

    // Add unique constraint to prevent duplicate words in same deck
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_words_deckId_text` ON `words` (`deckId`, `text`)")
}

// Migration 5→6: Add skipped and difficulty columns to turn_history
val MIGRATION_5_6 = Migration(5, 6) { database ->
    database.execSQL("ALTER TABLE `turn_history` ADD COLUMN `skipped` INTEGER NOT NULL DEFAULT 0")
    database.execSQL("ALTER TABLE `turn_history` ADD COLUMN `difficulty` INTEGER")
}

// Migration 6→7: Add cover image support to decks table
val MIGRATION_6_7 = Migration(6, 7) { database ->
    database.execSQL("ALTER TABLE `decks` ADD COLUMN `coverImageBase64` TEXT")
}

// Migration 7→8: Add matchId to turn_history for proper match grouping
val MIGRATION_7_8 = Migration(7, 8) { database ->
    database.execSQL("ALTER TABLE `turn_history` ADD COLUMN `matchId` TEXT")
}

// Migration 8→9: Add author metadata to decks
val MIGRATION_8_9 = Migration(8, 9) { database ->
    database.execSQL("ALTER TABLE `decks` ADD COLUMN `author` TEXT")
}

/**
 * All migrations as a list for easy registration in DataModule
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
)
