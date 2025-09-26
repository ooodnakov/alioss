package com.example.alioss.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGameEngineTest {
    private val config =
        MatchConfig(
            goal = MatchGoal(MatchGoalType.TARGET_WORDS, target = 4),
            maxSkips = 1,
            penaltyPerSkip = 1,
            roundSeconds = 5,
        )

    private fun wordGoal(target: Int) = MatchGoal(MatchGoalType.TARGET_WORDS, target)

    @Test
    fun `shuffles deterministically`() =
        runTest {
            val words = listOf("a", "b", "c", "d")
            val engine = DefaultGameEngine(words, this)

            suspend fun runMatch(): List<String> {
                engine.startMatch(config, teams = listOf("t"), seed = 123L)
                val seen = mutableListOf<String>()
                while (true) {
                    when (val s = engine.state.value) {
                        is GameState.TurnPending -> engine.startTurn()
                        is GameState.TurnActive -> {
                            seen += s.word
                            engine.correct()
                        }
                        is GameState.TurnFinished -> engine.nextTurn()
                        is GameState.MatchFinished -> return seen
                        GameState.Idle -> error("Unexpected Idle state during match")
                    }
                }
            }

            val first = runMatch()
            val second = runMatch()
            assertEquals(first, second)
        }

    @Test
    fun `pending state exposes scoreboard and remaining count`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a", "b", "c"), this)
            val cfg = config.copy(goal = wordGoal(2), roundSeconds = 5)
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            var pending = assertIs<GameState.TurnPending>(engine.state.value)
            assertEquals(mapOf("Team" to 0), pending.scores)
            assertEquals(MatchGoalType.TARGET_WORDS, pending.goal.type)
            assertEquals(2, pending.remainingToGoal)

            engine.startTurn()
            engine.correct()
            advanceTimeBy(cfg.roundSeconds * 1000L)
            runCurrent()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(1, finished.deltaScore)

            engine.nextTurn()
            pending = assertIs<GameState.TurnPending>(engine.state.value)
            assertEquals(mapOf("Team" to 1), pending.scores)
            assertEquals(1, pending.remainingToGoal)
        }

    @Test
    fun `skip uses limit and applies penalty`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a", "b", "c"), this)
            engine.startMatch(config, teams = listOf("t"), seed = 0L)

            engine.startTurn()
            var s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals(1, s.skipsRemaining)

            engine.skip()
            s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals(0, s.skipsRemaining)
            assertEquals(-1, s.score)

            engine.skip() // should be ignored
            s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals(0, s.skipsRemaining)
            assertEquals(-1, s.score)

            // Let timer elapse so no coroutine lingers after the test
            advanceTimeBy(config.roundSeconds * 1000L)
            runCurrent()
        }

    @Test
    fun `timer counts down and finishes`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a"), this)
            val shortConfig = config.copy(goal = wordGoal(1), roundSeconds = 2)
            engine.startMatch(shortConfig, teams = listOf("t"), seed = 0L)

            engine.startTurn()
            advanceTimeBy(2000)
            runCurrent()
            // Now emits TurnFinished(matchOver = true) first
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finished.matchOver)
            engine.nextTurn()
            assertTrue(engine.state.value is GameState.MatchFinished)
        }

    @Test
    fun `override from skip to correct restores penalty`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a"), this)
            val cfg = config.copy(goal = wordGoal(1), maxSkips = 1, penaltyPerSkip = 2, roundSeconds = 5)
            engine.startMatch(cfg, teams = listOf("t"), seed = 0L)

            engine.startTurn()
            var s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("a", s.word)
            engine.skip()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            // After skip: deltaScore = -2
            assertEquals(-2, finished.deltaScore)

            engine.overrideOutcome(0, true)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            // Change should be +3 (restore 2 penalty + 1 correct) -> total +1
            assertEquals(1, updated.deltaScore)
            assertEquals(1, updated.scores["t"]) // team total
            assertTrue(updated.matchOver) // reached goal
        }

    @Test
    fun `override from correct to incorrect applies penalty`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a"), this)
            val cfg = config.copy(goal = wordGoal(2), maxSkips = 1, penaltyPerSkip = 2, roundSeconds = 5)
            engine.startMatch(cfg, teams = listOf("t"), seed = 0L)

            engine.startTurn()
            assertIs<GameState.TurnActive>(engine.state.value)
            engine.correct()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(1, finished.deltaScore)

            engine.overrideOutcome(0, false)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            // Change should be -(1+2) = -3; total delta becomes -2
            assertEquals(-2, updated.deltaScore)
            assertEquals(-2, updated.scores["t"]) // team total
            assertTrue(updated.matchOver)
        }

    @Test
    fun `timer appends pending word without penalty`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a"), this)
            val cfg = config.copy(goal = wordGoal(2), maxSkips = 1, penaltyPerSkip = 1, roundSeconds = 1)
            engine.startMatch(cfg, teams = listOf("t"), seed = 0L)

            engine.startTurn()
            advanceTimeBy(1000)
            runCurrent()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            // One outcome appended for pending word; score unchanged
            assertEquals(1, finished.outcomes.size)
            assertEquals(0, finished.deltaScore)
            // Mark it correct; should add +1 (no penalty restoration since not a skip)
            engine.overrideOutcome(0, true)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(1, updated.deltaScore)
        }

    @Test
    fun `advances to next team`() =
        runTest {
            val words = listOf("a", "b", "c", "d")
            val engine = DefaultGameEngine(words, this)
            val short = config.copy(goal = wordGoal(2), roundSeconds = 1)
            engine.startMatch(short, teams = listOf("A", "B"), seed = 0L)

            engine.startTurn()
            // let timer expire for first team
            advanceTimeBy(short.roundSeconds * 1000L)
            runCurrent()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals("A", finished.team)
            assertEquals(0, finished.deltaScore)
            assertFalse(finished.matchOver)

            engine.nextTurn()
            engine.startTurn()
            val active = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("B", active.team)

            // Finish second team's timer to avoid leaving it running
            advanceTimeBy(short.roundSeconds * 1000L)
            runCurrent()
        }

    @Test
    fun `records outcomes and allows override`() =
        runTest {
            val words = listOf("apple", "banana")
            val engine = DefaultGameEngine(words, this)
            val short = config.copy(goal = wordGoal(2), roundSeconds = 10)
            engine.startMatch(short, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            var s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("apple", s.word)
            engine.correct()
            s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("banana", s.word)
            engine.skip()

            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(0, finished.deltaScore) // +1 -1
            assertEquals(2, finished.outcomes.size)
            assertTrue(finished.outcomes[0].correct)
            assertFalse(finished.outcomes[1].correct)
            assertTrue(finished.matchOver)

            engine.overrideOutcome(1, true)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(2, updated.deltaScore)
            assertTrue(updated.outcomes[1].correct)
            assertEquals(2, updated.scores["Team"])
        }

    @Test
    fun `override to correct finishes match when crossing target`() =
        runTest {
            val engine = DefaultGameEngine(listOf("apple", "banana"), this)
            val cfg = config.copy(goal = wordGoal(1), maxSkips = 1, penaltyPerSkip = 1, roundSeconds = 5)
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            var s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("apple", s.word)
            engine.skip()
            var s2 = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("banana", s2.word)

            // Let timer expire to finish the turn after skip
            advanceTimeBy(cfg.roundSeconds * 1000L)
            runCurrent()

            val finishedAfterSkip = assertIs<GameState.TurnFinished>(engine.state.value)
            assertEquals(-1, finishedAfterSkip.deltaScore)
            assertEquals(2, finishedAfterSkip.outcomes.size)

            engine.overrideOutcome(0, true)
            val finishedAfterOverride = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finishedAfterOverride.matchOver)
            assertEquals(1, finishedAfterOverride.deltaScore)
            assertEquals(1, finishedAfterOverride.scores["Team"])

            engine.nextTurn()
            assertIs<GameState.MatchFinished>(engine.state.value)
        }

    @Test
    fun `override to incorrect reopens match when dropping below target`() =
        runTest {
            val engine = DefaultGameEngine(listOf("apple", "banana"), this)
            val cfg = config.copy(goal = wordGoal(1), maxSkips = 1, penaltyPerSkip = 1, roundSeconds = 5)
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            var s = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals("apple", s.word)
            engine.correct()

            val finishedAfterCorrect = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finishedAfterCorrect.matchOver)
            assertEquals(1, finishedAfterCorrect.deltaScore)
            assertEquals(1, finishedAfterCorrect.scores["Team"])

            engine.overrideOutcome(0, false)
            val finishedAfterOverride = assertIs<GameState.TurnFinished>(engine.state.value)
            assertFalse(finishedAfterOverride.matchOver)
            assertEquals(-1, finishedAfterOverride.deltaScore)
            assertEquals(-1, finishedAfterOverride.scores["Team"])

            engine.nextTurn()
            assertIs<GameState.TurnPending>(engine.state.value)
        }

    @Test
    fun `override to incorrect preserves match completion when no words remain`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a", "b"), this)
            val cfg =
                config.copy(goal = wordGoal(2), maxSkips = 0, penaltyPerSkip = 0, roundSeconds = 1)
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            engine.correct() // consume first word
            advanceTimeBy(cfg.roundSeconds * 1000L)
            runCurrent()

            val finishedAfterTimer = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finishedAfterTimer.matchOver)
            assertTrue(finishedAfterTimer.outcomes.first().correct)

            engine.overrideOutcome(0, false)
            val finishedAfterOverride = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finishedAfterOverride.matchOver)
        }

    @Test
    fun `score goal ends match when target reached`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a", "b", "c"), this)
            val cfg = config.copy(goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 2))
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            val pending = assertIs<GameState.TurnPending>(engine.state.value)
            assertEquals(MatchGoalType.TARGET_SCORE, pending.goal.type)
            assertEquals(2, pending.remainingToGoal)

            engine.startTurn()
            var active = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals(MatchGoalType.TARGET_SCORE, active.goal.type)

            engine.correct()
            active = assertIs<GameState.TurnActive>(engine.state.value)
            assertEquals(1, active.score)
            assertEquals(1, active.remainingToGoal)

            engine.correct()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finished.matchOver)
            assertEquals(2, finished.scores["Team"])
        }

    @Test
    fun `score goal override to incorrect reopens match`() =
        runTest {
            val engine = DefaultGameEngine(listOf("a", "b", "c"), this)
            val cfg = config.copy(goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 2), penaltyPerSkip = 0)
            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            engine.correct()
            engine.correct()

            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finished.matchOver)

            engine.overrideOutcome(0, false)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            assertFalse(updated.matchOver)
            assertEquals(1, updated.scores["Team"])
        }

    @Test
    fun `match finishes when queue empties before reaching target`() =
        runTest {
            val words = listOf("apple", "banana")
            val engine = DefaultGameEngine(words, this)
            val cfg = config.copy(goal = wordGoal(3), roundSeconds = 10)
            engine.startMatch(cfg, teams = listOf("Solo"), seed = 0L)

            engine.startTurn()
            engine.correct()
            engine.correct()

            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finished.matchOver)

            engine.overrideOutcome(0, false)
            val updated = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(updated.matchOver)

            engine.nextTurn()
            assertIs<GameState.MatchFinished>(engine.state.value)
        }

    @Test
    fun `rotates teams consistently across many turns`() =
        runTest {
            val teams = listOf("Red", "Blue", "Green")
            val words = List(120) { "word$it" }
            val engine = DefaultGameEngine(words, this)
            val cfg =
                config.copy(
                    goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 999),
                    maxSkips = 0,
                    penaltyPerSkip = 0,
                    roundSeconds = 1,
                )
            engine.startMatch(cfg, teams = teams, seed = 42L)

            val observedOrder = mutableListOf<String>()
            val rounds = 10
            val expectedOrder = List(rounds * teams.size) { teams[it % teams.size] }

            repeat(rounds * teams.size) {
                val pending = assertIs<GameState.TurnPending>(engine.state.value)
                observedOrder += pending.team

                engine.startTurn()
                advanceTimeBy(cfg.roundSeconds * 1000L)
                runCurrent()

                val finished = assertIs<GameState.TurnFinished>(engine.state.value)
                assertEquals(pending.team, finished.team)
                assertFalse(finished.matchOver)

                engine.nextTurn()
            }

            assertEquals(expectedOrder, observedOrder)
            val nextPending = assertIs<GameState.TurnPending>(engine.state.value)
            assertEquals(teams[0], nextPending.team)
        }

    @Test
    fun `cumulative scores stay in sync across many turns`() =
        runTest {
            val teams = listOf("Alpha", "Beta")
            val random = Random(1234)
            val words = List(500) { "word$it" }
            val engine = DefaultGameEngine(words, this)
            val cfg =
                config.copy(
                    goal = MatchGoal(MatchGoalType.TARGET_SCORE, target = 500),
                    maxSkips = 2,
                    penaltyPerSkip = 2,
                    roundSeconds = 2,
                )
            engine.startMatch(cfg, teams = teams, seed = 99L)

            val cumulative = mutableMapOf<String, Int>()
            teams.forEach { cumulative[it] = 0 }

            repeat(40) {
                val pending = assertIs<GameState.TurnPending>(engine.state.value)
                val team = pending.team

                engine.startTurn()

                var correctCount = 0
                var skipCount = 0
                val actions = random.nextInt(from = 1, until = 5)
                repeat(actions) {
                    assertIs<GameState.TurnActive>(engine.state.value)
                    val useSkip = skipCount < cfg.maxSkips && random.nextBoolean()
                    if (useSkip) {
                        engine.skip()
                        skipCount++
                    } else {
                        engine.correct()
                        correctCount++
                    }
                }

                advanceTimeBy(cfg.roundSeconds * 1000L)
                runCurrent()

                val finished = assertIs<GameState.TurnFinished>(engine.state.value)
                assertEquals(team, finished.team)

                val expectedDelta = correctCount - (skipCount * cfg.penaltyPerSkip)
                assertEquals(expectedDelta, finished.deltaScore)

                cumulative[team] = cumulative.getValue(team) + expectedDelta
                teams.forEach { name -> assertEquals(cumulative.getValue(name), finished.scores.getValue(name)) }

                assertFalse(finished.matchOver)
                engine.nextTurn()
            }
        }

    @Test
    fun `peek next word does not advance queue`() =
        runTest {
            val words = listOf("alpha", "beta", "gamma", "delta")
            val expectedOrder = words.shuffled(Random(0L))
            val engine = DefaultGameEngine(words, this)
            val cfg = config.copy(goal = wordGoal(10), roundSeconds = 10)

            engine.startMatch(cfg, teams = listOf("Team"), seed = 0L)

            assertEquals(expectedOrder.first(), engine.peekNextWord())
            assertEquals(expectedOrder.first(), engine.peekNextWord(), "peek should not consume word")

            engine.startTurn()
            engine.assertActiveWordAndPeek(expectedOrder[0], expectedOrder[1])

            engine.correct()
            engine.assertActiveWordAndPeek(expectedOrder[1], expectedOrder[2])

            engine.correct()
            engine.assertActiveWordAndPeek(expectedOrder[2], expectedOrder[3])

            engine.correct()
            engine.assertActiveWordAndPeek(expectedOrder[3], null)

            engine.correct()
            val finished = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finished.matchOver)
            assertNull(engine.peekNextWord())

            engine.nextTurn()
            assertIs<GameState.MatchFinished>(engine.state.value)
        }

    @Test
    fun `start match resets previous progress`() =
        runTest {
            val words = listOf("alpha", "beta", "gamma", "delta")
            val engine = DefaultGameEngine(words, this)

            val firstConfig = config.copy(goal = wordGoal(1), roundSeconds = 1)
            engine.startMatch(firstConfig, teams = listOf("Team"), seed = 0L)

            engine.startTurn()
            engine.correct()
            advanceTimeBy(firstConfig.roundSeconds * 1000L)
            runCurrent()
            val finishedFirst = assertIs<GameState.TurnFinished>(engine.state.value)
            assertTrue(finishedFirst.matchOver)
            engine.nextTurn()
            assertIs<GameState.MatchFinished>(engine.state.value)

            val secondConfig = config.copy(goal = wordGoal(3), roundSeconds = 5)
            engine.startMatch(secondConfig, teams = listOf("Team"), seed = 1L)

            val pending = assertIs<GameState.TurnPending>(engine.state.value)
            assertEquals(mapOf("Team" to 0), pending.scores)
            assertEquals(3, pending.remainingToGoal)

            val expectedOrder = words.shuffled(Random(1L))
            assertEquals(expectedOrder.first(), engine.peekNextWord())
        }

    private suspend fun DefaultGameEngine.assertActiveWordAndPeek(
        expectedWord: String,
        expectedPeek: String?,
    ) {
        val active = assertIs<GameState.TurnActive>(state.value)
        assertEquals(expectedWord, active.word)
        assertEquals(expectedPeek, peekNextWord())
    }
}
