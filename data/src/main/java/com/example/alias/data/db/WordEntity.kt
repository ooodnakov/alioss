package com.example.alias.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity representing a word in a deck. */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId"), Index(value = ["language", "deckId"])]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: String,
    val text: String,
    val language: String,
    val stems: String?,
    val category: String?,
    val difficulty: Int,
    val tabooStems: String?,
    val isNSFW: Boolean
)
