package com.example.alioss.data.achievements

import kotlinx.coroutines.flow.Flow

interface AchievementsRepository {
    /** Stream of all achievements with live progress. */
    val achievements: Flow<List<AchievementState>>

    /** Increment the number of correctly guessed words. */
    suspend fun recordCorrectGuesses(count: Int)

    /** Increment the number of perfect turns. */
    suspend fun recordPerfectTurn()

    /** Increment the number of fast wins. */
    suspend fun recordFastMatchWin()

    /** Track a manual settings adjustment. */
    suspend fun recordSettingsAdjustment()

    /** Persist that a specific app section was visited. */
    suspend fun recordSectionVisited(section: AchievementSection)
}
