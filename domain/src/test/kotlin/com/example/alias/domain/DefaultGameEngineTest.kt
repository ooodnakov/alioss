package com.example.alias.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGameEngineTest {

    private val config = MatchConfig(
        targetWords = 4,
        maxSkips = 1,
        penaltyPerSkip = 1,
        roundSeconds = 5,
    )

    @Test
    fun `shuffles deterministically`() = runTest {
        val words = listOf("a", "b", "c", "d")
        val engine = DefaultGameEngine(words, this)

        fun runMatch(): List<String> {
            engine.startMatch(config, teams = listOf("t"), seed = 123L)
            val seen = mutableListOf<String>()
            while (true) {
                val s = engine.state.value
                if (s is GameState.TurnActive) {
                    seen += s.word
                    engine.correct()
                } else if (s is GameState.MatchFinished) {
                    break
                }
            }
            return seen
        }

        val first = runMatch()
        val second = runMatch()
        assertEquals(first, second)
    }

    @Test
    fun `skip uses limit and applies penalty`() = runTest {
        val engine = DefaultGameEngine(listOf("a", "b", "c"), this)
        engine.startMatch(config, teams = listOf("t"), seed = 0L)

        var s = engine.state.value as GameState.TurnActive
        assertEquals(1, s.skipsRemaining)

        engine.skip()
        s = engine.state.value as GameState.TurnActive
        assertEquals(0, s.skipsRemaining)
        assertEquals(-1, s.score)

        engine.skip() // should be ignored
        s = engine.state.value as GameState.TurnActive
        assertEquals(0, s.skipsRemaining)
        assertEquals(-1, s.score)
    }

    @Test
    fun `timer counts down and finishes`() = runTest {
        val engine = DefaultGameEngine(listOf("a"), this)
        val shortConfig = config.copy(targetWords = 1, roundSeconds = 2)
        engine.startMatch(shortConfig, teams = listOf("t"), seed = 0L)

        advanceTimeBy(2000)
        runCurrent()
        assertTrue(engine.state.value is GameState.MatchFinished)
    }

    @Test
    fun `advances to next team`() = runTest {
        val words = listOf("a", "b", "c", "d")
        val engine = DefaultGameEngine(words, this)
        val short = config.copy(targetWords = 2, roundSeconds = 1)
        engine.startMatch(short, teams = listOf("A", "B"), seed = 0L)

        // let timer expire for first team
        advanceTimeBy(1000)
        runCurrent()
        val finished = engine.state.value as GameState.TurnFinished
        assertEquals("A", finished.team)
        assertEquals(0, finished.deltaScore)

        engine.nextTurn()
        val active = engine.state.value as GameState.TurnActive
        assertEquals("B", active.team)
    }
}
