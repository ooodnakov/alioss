package com.example.alias.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private var currentWord: String = ""
    private val turnResults: MutableList<WordResult> = mutableListOf()
    private val mutex = Mutex()

    override fun startMatch(config: MatchConfig, teams: List<String>, seed: Long) {
        runBlocking {
            mutex.withLock {
                this@DefaultGameEngine.config = config
                this@DefaultGameEngine.teams = teams
                queue = words.shuffled(Random(seed)).toMutableList()
                scores.clear()
                teams.forEach { scores[it] = 0 }
                processed = 0
                currentTeam = 0
                startTurnLocked()
            }
        }
    }

    override fun correct() {
        runBlocking {
            mutex.withLock {
                if (_state.value !is GameState.TurnActive) return@withLock
                turnScore++
                processed++
                turnResults.add(WordResult(currentWord, true))
                advanceLocked()
            }
        }
    }

    override fun skip() {
        runBlocking {
            mutex.withLock {
                if (_state.value !is GameState.TurnActive) return@withLock
                if (skipsRemaining <= 0) return@withLock
                skipsRemaining--
                turnScore -= config.penaltyPerSkip
                processed++
                turnResults.add(WordResult(currentWord, false))
                advanceLocked()
            }
        }
    }

    override fun nextTurn() {
        runBlocking {
            mutex.withLock {
                if (_state.value !is GameState.TurnFinished) return@withLock
                currentTeam = (currentTeam + 1) % teams.size
                startTurnLocked()
            }
        }
    }

    override fun adjustScore(delta: Int) {
        runBlocking {
            mutex.withLock {
                val current = _state.value
                if (current is GameState.TurnFinished) {
                    val team = current.team
                    scores[team] = scores.getOrDefault(team, 0) + delta
                    _state.value = current.copy(
                        deltaScore = current.deltaScore + delta,
                        scores = scores.toMap(),
                    )
                }
            }
        }
    }

    private suspend fun startTurnLocked() {
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishMatchLocked()
            return
        }
        skipsRemaining = config.maxSkips
        timeRemaining = config.roundSeconds
        turnScore = 0
        turnResults.clear()
        timerJob?.cancel()
        timerJob = scope.launch { tickTimer() }
        advanceLocked()
    }

    private suspend fun advanceLocked() {
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishTurnLocked()
            return
        }
        val next = queue.removeFirst()
        val remaining = config.targetWords - processed
        val team = teams[currentTeam]
        val totalScore = scores.getOrDefault(team, 0) + turnScore
        currentWord = next
        _state.update { GameState.TurnActive(team, next, remaining, totalScore, skipsRemaining, timeRemaining, config.roundSeconds) }
    }

    private suspend fun finishTurnLocked() {
        timerJob?.cancel()
        val team = teams[currentTeam]
        scores[team] = scores.getOrDefault(team, 0) + turnScore
        if (processed >= config.targetWords || queue.isEmpty()) {
            finishMatchLocked()
        } else {
            _state.update { GameState.TurnFinished(team, turnScore, scores.toMap(), turnResults.toList()) }
        }
    }

    private suspend fun finishMatchLocked() {
        timerJob?.cancel()
        _state.update { GameState.MatchFinished(scores.toMap()) }
    }

    private suspend fun tickTimer() {
        while (true) {
            delay(1000)
            mutex.withLock {
                if (timeRemaining <= 0) return@withLock
                timeRemaining--
                _state.update { current ->
                    if (current is GameState.TurnActive) current.copy(timeRemaining = timeRemaining) else current
                }
                if (timeRemaining <= 0 && _state.value is GameState.TurnActive) {
                    finishTurnLocked()
                }
            }
        }
    }
}
