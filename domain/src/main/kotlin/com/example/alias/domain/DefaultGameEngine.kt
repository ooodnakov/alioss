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
  private var correctTotal: Int = 0
  private var skipsRemaining: Int = 0
  private var timeRemaining: Int = 0
  private var timerJob: Job? = null
  private var currentWord: String = ""
  private val outcomes: MutableList<TurnOutcome> = mutableListOf()
  private var matchOver: Boolean = false
  private val mutex = Mutex()

  override fun startMatch(
    config: MatchConfig,
    teams: List<String>,
    seed: Long,
  ) {
    runBlocking {
      mutex.withLock {
        this@DefaultGameEngine.config = config
        this@DefaultGameEngine.teams = teams
        queue = words.shuffled(Random(seed)).toMutableList()
        scores.clear()
        teams.forEach { scores[it] = 0 }
        correctTotal = 0
        currentTeam = 0
        matchOver = false
        prepareTurnLocked()
      }
    }
  }

  override fun correct() {
    runBlocking {
      mutex.withLock {
        if (_state.value !is GameState.TurnActive) return@withLock
        turnScore++
        correctTotal++
        outcomes.add(TurnOutcome(currentWord, true, System.currentTimeMillis()))
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
        outcomes.add(TurnOutcome(currentWord, false, System.currentTimeMillis(), skipped = true))
        advanceLocked()
      }
    }
  }

  override fun nextTurn() {
    runBlocking {
      mutex.withLock {
        if (_state.value !is GameState.TurnFinished) return@withLock
        if (matchOver) {
          finishMatchLocked()
        } else {
          outcomes.clear()
          currentTeam = (currentTeam + 1) % teams.size
          prepareTurnLocked()
        }
      }
    }
  }

  override fun startTurn() {
    runBlocking {
      mutex.withLock {
        if (_state.value !is GameState.TurnPending) return@withLock
        startTurnLocked()
      }
    }
  }

  override fun peekNextWord(): String? {
    return runBlocking {
      mutex.withLock {
        queue.firstOrNull()
      }
    }
  }

  override fun overrideOutcome(
    index: Int,
    correct: Boolean,
  ) {
    runBlocking {
      mutex.withLock {
        val current = _state.value
        if (current !is GameState.TurnFinished) return@withLock
        val item = outcomes.getOrNull(index) ?: return@withLock
        if (item.correct == correct) return@withLock
        val team = current.team
        val change =
          if (correct) {
            val penalty = if (!item.correct && item.skipped) config.penaltyPerSkip else 0
            1 + penalty
          } else {
            -(1 + config.penaltyPerSkip)
          }
        turnScore += change
        scores[team] = scores.getOrDefault(team, 0) + change
        // Update total correct words across the match based on override
        if (item.correct != correct) {
          if (correct) correctTotal++ else correctTotal--
        }
        outcomes[index] = item.copy(correct = correct, skipped = !correct)
        val nowMatchOver = correctTotal >= config.targetWords
        _state.update { GameState.TurnFinished(team, turnScore, scores.toMap(), outcomes.toList(), nowMatchOver) }
      }
    }
  }

  private suspend fun prepareTurnLocked() {
    if (correctTotal >= config.targetWords) {
      finishMatchLocked()
      return
    }
    _state.update { GameState.TurnPending(teams[currentTeam]) }
  }

  private suspend fun startTurnLocked() {
    if (correctTotal >= config.targetWords) {
      finishMatchLocked()
      return
    }
    skipsRemaining = config.maxSkips
    timeRemaining = config.roundSeconds
    turnScore = 0
    outcomes.clear()
    timerJob?.cancel()
    timerJob = scope.launch { tickTimer() }
    advanceLocked()
  }

  private suspend fun advanceLocked() {
    if (correctTotal >= config.targetWords || queue.isEmpty()) {
      finishTurnLocked(byTimer = false)
      return
    }
    val next = queue.removeFirst()
    val remaining = (config.targetWords - correctTotal).coerceAtLeast(0)
    val team = teams[currentTeam]
    val totalScore = scores.getOrDefault(team, 0) + turnScore
    currentWord = next
    _state.update { GameState.TurnActive(team, next, remaining, totalScore, skipsRemaining, timeRemaining, config.roundSeconds) }
  }

  private suspend fun finishTurnLocked(byTimer: Boolean) {
    timerJob?.cancel()
    val team = teams[currentTeam]
    scores[team] = scores.getOrDefault(team, 0) + turnScore
    val reachedTarget = correctTotal >= config.targetWords
    val noWordsLeft = queue.isEmpty()
    // If timer expired while a word was shown, include it as pending (no penalty applied yet)
    if (byTimer) {
      val current = _state.value
      if (current is GameState.TurnActive) {
        outcomes.add(TurnOutcome(currentWord, false, System.currentTimeMillis(), skipped = false))
      }
    }
    matchOver = reachedTarget || (byTimer && noWordsLeft)
    // Always end the turn first; if matchOver, UI can finalize via nextTurn()
    _state.update { GameState.TurnFinished(team, turnScore, scores.toMap(), outcomes.toList(), matchOver) }
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
          finishTurnLocked(byTimer = true)
        }
      }
    }
  }
}
