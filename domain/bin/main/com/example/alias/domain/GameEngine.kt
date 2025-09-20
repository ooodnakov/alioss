package com.example.alias.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Baseline interface for the game state machine.
 */
interface GameEngine {
    /** Current immutable state exposed to observers. */
    val state: StateFlow<GameState>

    /** Start a new match with the provided [config], [teams], and random [seed]. */
    suspend fun startMatch(
        config: MatchConfig,
        teams: List<String>,
        seed: Long,
    )

    /** Register that the current word was guessed correctly. */
    suspend fun correct()

    /** Register that the current word was skipped. */
    suspend fun skip()

    /** Advance to the next team's turn after a finished turn. */
    suspend fun nextTurn()

    /** Begin the currently pending team's turn. */
    suspend fun startTurn()

    /** Override the outcome of a word at [index] in the last turn. */
    suspend fun overrideOutcome(
        index: Int,
        correct: Boolean,
    )

    /** Optional hint for UI: preview the next word without advancing state. */
    suspend fun peekNextWord(): String?
}

/**
 * Representation of the game state.
 */
sealed interface GameState {
    /** Waiting to start a match. */
    data object Idle : GameState

    /** A team's turn is ready to start. */
    data class TurnPending(
        val team: String,
        val scores: Map<String, Int>,
        val goal: MatchGoal,
        val remainingToGoal: Int,
    ) : GameState

    /** A turn is active and [word] should be explained by [team]. */
    data class TurnActive(
        val team: String,
        val word: String,
        val goal: MatchGoal,
        val remainingToGoal: Int,
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
    val goal: MatchGoal,
    val maxSkips: Int,
    val penaltyPerSkip: Int,
    val roundSeconds: Int,
)

/** Match victory condition. */
data class MatchGoal(
    val type: MatchGoalType,
    val target: Int,
)

/** Types of supported match goals. */
enum class MatchGoalType {
    TARGET_WORDS,
    TARGET_SCORE,
}

/** Outcome of a single word during a turn. */
data class TurnOutcome(
    val word: String,
    val correct: Boolean,
    val timestamp: Long,
    val skipped: Boolean = false,
)
