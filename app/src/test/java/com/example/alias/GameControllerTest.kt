package com.example.alias

import com.example.alias.achievements.AchievementsManager
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.achievements.AchievementSection
import com.example.alias.data.achievements.AchievementState
import com.example.alias.data.achievements.AchievementsRepository
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.MatchConfig
import com.example.alias.domain.MatchGoalType
import com.example.alias.domain.TurnOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameControllerTest {
    private lateinit var historyRepository: FakeTurnHistoryRepository
    private lateinit var achievementsRepository: FakeAchievementsRepository
    private lateinit var achievementsManager: AchievementsManager
    private lateinit var controller: GameController

    @Before
    fun setUp() {
        historyRepository = FakeTurnHistoryRepository()
        achievementsRepository = FakeAchievementsRepository()
        achievementsManager = AchievementsManager(achievementsRepository)
        controller = GameController(historyRepository, achievementsManager, FakeGameEngineFactory())
    }

    @Test
    fun startMatchAppliesSkipPenaltyWhenPunished() = runBlocking {
        val engine = FakeGameEngine()
        val settings = Settings(punishSkips = false, penaltyPerSkip = 5)

        controller.startMatch(engine, settings)

        val config = engine.lastConfig
        requireNotNull(config)
        assertEquals(0, config.penaltyPerSkip)
    }

    @Test
    fun startMatchUsesScoreGoalWhenEnabled() = runBlocking {
        val engine = FakeGameEngine()
        val settings = Settings(scoreTargetEnabled = true, targetScore = 42)

        controller.startMatch(engine, settings)

        val goal = engine.lastConfig?.goal
        requireNotNull(goal)
        assertEquals(MatchGoalType.TARGET_SCORE, goal.type)
        assertEquals(42, goal.target)
    }

    @Test
    fun completeTurnSavesHistoryAndAdvances() = runBlocking {
        val engine = FakeGameEngine()
        val outcomes = listOf(TurnOutcome("word", correct = true, timestamp = 123L))
        engine.updateState(
            GameState.TurnFinished(
                team = "A",
                deltaScore = 1,
                scores = emptyMap(),
                outcomes = outcomes,
                matchOver = false,
            ),
        )

        controller.completeTurn(
            engine,
            mapOf("word" to WordInfo(difficulty = 2, category = "party", wordClass = "NOUN")),
        )

        assertEquals(1, historyRepository.saved.size)
        val entry = historyRepository.saved.single()
        assertEquals("word", entry.word)
        assertEquals(2, entry.difficulty)
        assertTrue(engine.nextTurnCalled)
    }

    private class FakeTurnHistoryRepository : TurnHistoryRepository {
        val saved = mutableListOf<TurnHistoryEntity>()
        override suspend fun save(entries: List<TurnHistoryEntity>) {
            saved.addAll(entries)
        }

        override fun getRecent(limit: Int): kotlinx.coroutines.flow.Flow<List<TurnHistoryEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun clear() {
            saved.clear()
        }
    }

    private class FakeAchievementsRepository : AchievementsRepository {
        override val achievements = MutableStateFlow(emptyList<AchievementState>())

        override suspend fun recordCorrectGuesses(count: Int) = Unit

        override suspend fun recordPerfectTurn() = Unit

        override suspend fun recordFastMatchWin() = Unit

        override suspend fun recordSettingsAdjustment() = Unit

        override suspend fun recordSectionVisited(section: AchievementSection) = Unit
    }

    private class FakeGameEngine : GameEngine {
        private val backingState = MutableStateFlow<GameState>(GameState.Idle)
        var lastConfig: MatchConfig? = null
        var nextTurnCalled = false

        override val state: StateFlow<GameState>
            get() = backingState

        override suspend fun startMatch(config: MatchConfig, teams: List<String>, seed: Long) {
            lastConfig = config
        }

        override suspend fun correct() = throw UnsupportedOperationException()
        override suspend fun skip() = throw UnsupportedOperationException()

        override suspend fun nextTurn() {
            nextTurnCalled = true
        }

        override suspend fun startTurn() = Unit

        override suspend fun overrideOutcome(index: Int, correct: Boolean) = Unit

        override suspend fun peekNextWord(): String? = null

        fun updateState(newState: GameState) {
            backingState.value = newState
        }
    }

    private class FakeGameEngineFactory : GameEngineFactory {
        override fun create(words: List<String>, scope: CoroutineScope): GameEngine {
            throw UnsupportedOperationException()
        }
    }
}
