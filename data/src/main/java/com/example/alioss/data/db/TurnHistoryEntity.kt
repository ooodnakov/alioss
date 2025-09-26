package com.example.alioss.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turn_history")
data class TurnHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val team: String,
    val word: String,
    val correct: Boolean,
    val skipped: Boolean,
    val difficulty: Int?,
    val timestamp: Long,
    val matchId: String? = null,
)
