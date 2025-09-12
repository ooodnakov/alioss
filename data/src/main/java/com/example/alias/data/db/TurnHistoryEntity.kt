package com.example.alias.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turn_history")
data class TurnHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val team: String,
    val word: String,
    val correct: Boolean,
    val timestamp: Long,
)

