package com.example.alias

import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.MatchConfig
import com.example.alias.domain.MatchGoal
import com.example.alias.domain.MatchGoalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameController
    @Inject
    constructor(
        private val historyRepository: TurnHistoryRepository,
        private val gameEngineFactory: GameEngineFactory,
    ) {
        private var currentMatchId: String? = null
        fun createEngine(words: List<String>, scope: CoroutineScope): GameEngine {
            return gameEngineFactory.create(words, scope)
        }

        suspend fun startMatch(engine: GameEngine, settings: Settings) {
            currentMatchId = java.util.UUID.randomUUID().toString()
            val goal =
                if (settings.scoreTargetEnabled) {
                    MatchGoal(MatchGoalType.TARGET_SCORE, settings.targetScore)
                } else {
                    MatchGoal(MatchGoalType.TARGET_WORDS, settings.targetWords)
                }
            val config = MatchConfig(
                goal = goal,
                maxSkips = settings.maxSkips,
                penaltyPerSkip = if (settings.punishSkips) settings.penaltyPerSkip else 0,
                roundSeconds = settings.roundSeconds,
            )
            val seed = SecureRandom().nextLong()
            engine.startMatch(config, teams = settings.teams, seed = seed)
        }

        suspend fun completeTurn(engine: GameEngine, infoByWord: Map<String, WordInfo>) {
            val current = engine.state.value
            if (current is GameState.TurnFinished) {
                val entries = current.outcomes.map { outcome ->
                    TurnHistoryEntity(
                        id = 0L,
                        team = current.team,
                        word = outcome.word,
                        correct = outcome.correct,
                        skipped = outcome.skipped,
                        difficulty = infoByWord[outcome.word]?.difficulty,
                        timestamp = outcome.timestamp,
                        matchId = currentMatchId,
                    )
                }
                historyRepository.save(entries)
                engine.nextTurn()
            }
        }

        suspend fun startTurn(engine: GameEngine) {
            engine.startTurn()
        }

        suspend fun overrideOutcome(engine: GameEngine, index: Int, correct: Boolean) {
            engine.overrideOutcome(index, correct)
        }

        fun recentHistory(limit: Int): Flow<List<TurnHistoryEntity>> = historyRepository.getRecent(limit)

        suspend fun clearHistory() {
            historyRepository.clear()
        }
    }
