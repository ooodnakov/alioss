package com.example.alioss.achievements

import com.example.alioss.data.achievements.AchievementSection
import com.example.alioss.data.achievements.AchievementState
import com.example.alioss.data.achievements.AchievementsRepository
import com.example.alioss.data.db.TurnHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementsManager @Inject constructor(
    private val repository: AchievementsRepository,
) {
    companion object {
        private const val FAST_WIN_TURN_LIMIT = 5
    }

    val achievements: Flow<List<AchievementState>> = repository.achievements

    private val mutex = Mutex()
    private var turnsInCurrentMatch: Int = 0

    suspend fun onMatchStarted() {
        mutex.withLock { turnsInCurrentMatch = 0 }
    }

    suspend fun onTurnCompleted(entries: List<TurnHistoryEntity>, matchOver: Boolean) {
        if (entries.isNotEmpty()) {
            val correctCount = entries.count { it.correct }
            if (correctCount > 0) {
                repository.recordCorrectGuesses(correctCount)
            }
            val isPerfect = entries.all { it.correct && !it.skipped }
            if (isPerfect) {
                repository.recordPerfectTurn()
            }
        }

        val shouldRecordFastWin = mutex.withLock {
            turnsInCurrentMatch += 1
            val qualifies = matchOver && turnsInCurrentMatch <= FAST_WIN_TURN_LIMIT
            if (matchOver) {
                turnsInCurrentMatch = 0
            }
            qualifies
        }
        if (shouldRecordFastWin) {
            repository.recordFastMatchWin()
        }
    }

    suspend fun onSettingsAdjusted() {
        repository.recordSettingsAdjustment()
    }

    suspend fun onSectionVisited(section: AchievementSection) {
        repository.recordSectionVisited(section)
    }
}
