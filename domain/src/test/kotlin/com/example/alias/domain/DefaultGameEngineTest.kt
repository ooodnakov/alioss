package com.example.alias.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DefaultGameEngineTest {
    private val config = MatchConfig(
        targetWords = 10,
        maxSkips = 1,
        penaltyPerSkip = 0,
        roundSeconds = 60,
    )

    @Test
    fun blocked_word_is_not_used_in_match_or_future_matches() = runTest {
        val engine = DefaultGameEngine(listOf("apple", "banana"), this)
        engine.blockWord("banana")

        engine.startMatch(config, listOf("A"), seed = 0)
        val first = engine.state.value
        assertTrue(first is GameState.TurnActive)
        assertEquals("apple", first.word)

        engine.startMatch(config, listOf("A"), seed = 0)
        val second = engine.state.value
        assertTrue(second is GameState.TurnActive)
        assertEquals("apple", second.word)
    }

    @Test
    fun blocking_current_word_advances_to_next_word() = runTest {
        val engine = DefaultGameEngine(listOf("apple", "banana"), this)
        engine.startMatch(config, listOf("A"), seed = 0)
        val current = engine.state.value as GameState.TurnActive
        val firstWord = current.word
        engine.blockWord(firstWord)
        val afterBlock = engine.state.value
        assertTrue(afterBlock is GameState.TurnActive)
        assertNotEquals(firstWord, afterBlock.word)
    }
}
