package com.example.alias.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity representing a deck of words. */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
    val isOfficial: Boolean,
    val isNSFW: Boolean,
    val version: Int,
    val updatedAt: Long
)
