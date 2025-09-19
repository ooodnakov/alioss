package com.example.alias.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a single word class tag associated with a word entry.
 */
@Entity(
    tableName = "word_classes",
    primaryKeys = ["deckId", "wordText", "wordClass"],
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["deckId", "text"],
            childColumns = ["deckId", "wordText"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("wordClass"),
        Index(value = ["deckId", "wordText"]),
    ],
)
data class WordClassEntity(
    val deckId: String,
    val wordText: String,
    val wordClass: String,
)
