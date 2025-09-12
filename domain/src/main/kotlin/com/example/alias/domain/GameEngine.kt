package com.example.alias.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Baseline interface for the game state machine.
 */
interface GameEngine {
    /** Current immutable state exposed to observers. */
    val state: StateFlow<GameState>

    /** Start a new match with the provided [config], [teams], and random [seed]. */
    fun startMatch(config: MatchConfig, teams: List<String>, seed: Long)

    /** Register that the current word was guessed correctly. */
    fun correct()

    /** Register that the current word was skipped. */
    fun skip()

    /** Advance to the next team's turn after a finished turn. */
    fun nextTurn()

    /** Override the outcome of a word at [index] in the last turn. */
    fun overrideOutcome(index: Int, correct: Boolean)
}

/**
 * Representation of the game state.
 */
sealed interface GameState {
    /** Waiting to start a match. */
    data object Idle : GameState

    /** A turn is active and [word] should be explained by [team]. */
    data class TurnActive(
        val team: String,
        val word: String,
        val remaining: Int,
        val score: Int,
        val skipsRemaining: Int,
        val timeRemaining: Int,
        val totalSeconds: Int,
    ) : GameState

    /** A team's turn has ended and awaits the next team. */
    data class TurnFinished(
        val team: String,
        val deltaScore: Int,
        val scores: Map<String, Int>,
        val outcomes: List<TurnOutcome>,
        val matchOver: Boolean,
    ) : GameState

    /** The current match has finished. */
    data class MatchFinished(
        val scores: Map<String, Int>,
    ) : GameState
}

/** Configuration options for starting a match. */
data class MatchConfig(
    val targetWords: Int,
    val maxSkips: Int,
    val penaltyPerSkip: Int,
    val roundSeconds: Int,
)

/** Outcome of a single word during a turn. */
data class TurnOutcome(
    val word: String,
    val correct: Boolean,
    val timestamp: Long,
)
