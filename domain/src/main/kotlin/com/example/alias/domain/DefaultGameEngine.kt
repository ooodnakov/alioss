package com.example.alias.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * In-memory implementation of [GameEngine] supporting multiple teams and timed turns.
 */
class DefaultGameEngine(
    private val words: List<String>,
    private val scope: CoroutineScope,
) : GameEngine {

    private val _state = MutableStateFlow<GameState>(GameState.Idle)
    override val state: StateFlow<GameState> = _state.asStateFlow()

    private var queue: MutableList<String> = mutableListOf()
    private lateinit var config: MatchConfig
    private lateinit var teams: List<String>
    private val scores: MutableMap<String, Int> = mutableMapOf()
    private var currentTeam: Int = 0
    private var turnScore: Int = 0
    private var processed: Int = 0
    private var skipsRemaining: Int = 0
    private var timeRemaining: Int = 0
    private var timerJob: Job? = null

    override fun startMatch(config: MatchConfig, teams: List<String>, seed: Long) {
        this.config = config
        this.teams = teams
        queue = words.shuffled(Random(seed)).toMutableList()
        scores.clear()
        teams.forEach { scores[it] = 0 }
        processed = 0
        currentTeam = 0
        startTurn()
    }

    override fun correct() {
        if (_state.value !is GameState.TurnActive) return
        turnScore++
        processed++
        advance()
    }

    override fun skip() {
        if (_state.value !is GameState.TurnActive) return
        if (skipsRemaining <= 0) return
        skipsRemaining--
        turnScore -= config.penaltyPerSkip
        processed++
        advance()
    }

    override fun nextTurn() {
        if (_state.value !is GameState.TurnFinished) return
        currentTeam = (currentTeam + 1) % teams.size
        startTurn()
    }

    private fun startTurn() {
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishMatch()
            return
        }
        skipsRemaining = config.maxSkips
        timeRemaining = config.roundSeconds
        turnScore = 0
        timerJob?.cancel()
        timerJob = scope.launch { tickTimer() }
        advance()
    }

    private fun advance() {
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishTurn()
            return
        }
        val next = queue.removeFirst()
        val remaining = config.targetWords - processed
        val team = teams[currentTeam]
        val totalScore = scores[team]!! + turnScore
        _state.value = GameState.TurnActive(team, next, remaining, totalScore, skipsRemaining, timeRemaining)
    }

    private fun finishTurn() {
        timerJob?.cancel()
        val team = teams[currentTeam]
        scores[team] = scores[team]!! + turnScore
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishMatch()
        } else {
            _state.value = GameState.TurnFinished(team, turnScore, scores.toMap())
        }
    }

    private fun finishMatch() {
        timerJob?.cancel()
        _state.value = GameState.MatchFinished(scores.toMap())
    }

    private suspend fun tickTimer() {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            val current = _state.value
            if (current is GameState.TurnActive) {
                _state.value = current.copy(timeRemaining = timeRemaining)
            }
        }
        if (_state.value is GameState.TurnActive) {
            finishTurn()
        }
    }
}
