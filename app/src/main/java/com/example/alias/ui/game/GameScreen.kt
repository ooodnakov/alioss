package com.example.alias.ui.game

import android.content.res.Configuration
import android.os.VibrationEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.ui.CountdownOverlay
import com.example.alias.ui.TutorialOverlay
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.example.alias.ui.common.Scoreboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LARGE_BUTTON_HEIGHT = 80.dp
private const val CARD_ASPECT_RATIO = 1.8f
private const val PRE_TURN_COUNTDOWN_SECONDS = 3
private val TIMER_SAFE_COLOR = Color(0xFF4CAF50)
private val TIMER_WARNING_COLOR = Color(0xFFFFC107)
private val TIMER_CRITICAL_COLOR = Color(0xFFF44336)

@Composable
fun GameScreen(vm: MainViewModel, engine: GameEngine, settings: Settings) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    DisposableEffect(settings.orientation) {
        val original = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = when (settings.orientation) {
            "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose { activity?.requestedOrientation = original }
    }
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    val tone = remember { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80) }
    DisposableEffect(Unit) {
        onDispose { tone.release() }
    }
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    val showTutorialOnFirstTurn by vm.showTutorialOnFirstTurn.collectAsState()
    val seenTutorial = settings.seenTutorial
    var cardBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(state) {
        if (showTutorialOnFirstTurn && state is GameState.TurnActive && !seenTutorial) {
            // First turn started, tutorial will show below
        } else if (showTutorialOnFirstTurn) {
            // Tutorial was dismissed or not first turn
            vm.dismissTutorialOnFirstTurn()
        }
        if (state !is GameState.TurnActive) {
            cardBounds = null
        }
    }

    val activeState = state as? GameState.TurnActive

    if (showTutorialOnFirstTurn && activeState != null && !seenTutorial) {
        TutorialOverlay(
            verticalMode = settings.verticalSwipes,
            allowSkip = activeState.skipsRemaining > 0,
            onDismiss = {
                vm.updateSeenTutorial(true)
            },
            cardBounds = cardBounds,
            modifier = Modifier.zIndex(1f),
        )
    }
    when (val s = state) {
        GameState.Idle -> Text(stringResource(R.string.idle))
        is GameState.TurnPending -> {
            val countdownState = rememberCountdownState(scope)
            DisposableEffect(Unit) {
                onDispose { countdownState.cancel() }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .zIndex(0f),
                    contentAlignment = Alignment.Center,
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.next_team),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = s.team,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Scoreboard(s.scores)
                            Text(
                                text = pluralStringResource(
                                    R.plurals.turn_pending_status,
                                    s.remainingToWin,
                                    s.remainingToWin,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (settings.hapticsEnabled) {
                                        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                        vibrator?.vibrate(effect)
                                    }
                                    if (!countdownState.isRunning) {
                                        countdownState.start(onFinished = vm::startTurn)
                                    }
                                },
                                enabled = !countdownState.isRunning,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(LARGE_BUTTON_HEIGHT),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.start_turn), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                countdownState.value?.let { value ->
                    CountdownOverlay(
                        value = value,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                    )
                }
            }
        }
        is GameState.TurnActive -> {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val rawProgress = if (s.totalSeconds > 0) s.timeRemaining.toFloat() / s.totalSeconds else 0f
            val progress by animateFloatAsState(rawProgress, label = "timerProgress")
            val targetColor = if (rawProgress > 0.5f) {
                val t = (1 - rawProgress) * 2f
                lerp(TIMER_SAFE_COLOR, TIMER_WARNING_COLOR, t)
            } else {
                val t = rawProgress * 2f
                lerp(TIMER_WARNING_COLOR, TIMER_CRITICAL_COLOR, 1 - t)
            }
            val barColor by animateColorAsState(targetColor, label = "timerColor")
            LaunchedEffect(s.timeRemaining) {
                if (settings.hapticsEnabled && (s.timeRemaining == 10 || s.timeRemaining == 3)) {
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                }
            }
            var isProcessing by remember { mutableStateOf(false) }
            var committing by remember { mutableStateOf(false) }
            var frozenNext by remember { mutableStateOf<String?>(null) }
            var computedNext by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(s.word) {
                isProcessing = false
                committing = false
                frozenNext = null
                computedNext = engine.peekNextWord()
            }
            val infoMap by vm.wordInfoByText.collectAsState()
            val CardStack: @Composable () -> Unit = {
                val nextWord = frozenNext ?: computedNext
                val nextMeta = nextWord?.let { infoMap[it] }
                val currentMeta = infoMap[s.word]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CARD_ASPECT_RATIO)
                        .onGloballyPositioned { coordinates ->
                            cardBounds = coordinates.boundsInRoot()
                        },
                ) {
                    if (nextWord != null) {
                        WordCard(
                            word = nextWord,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0f)
                                .alpha(if (committing) 1f else 0f),
                            enabled = false,
                            vibrator = null,
                            hapticsEnabled = false,
                            onActionStart = {},
                            onAction = {},
                            animateAppear = false,
                            allowSkip = s.skipsRemaining > 0,
                            verticalMode = settings.verticalSwipes,
                            wordDifficulty = nextMeta?.difficulty,
                            wordCategory = nextMeta?.category,
                            wordClass = nextMeta?.wordClass,
                        )
                    }
                    WordCard(
                        word = s.word,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        enabled = true,
                        vibrator = vibrator,
                        hapticsEnabled = settings.hapticsEnabled,
                        onActionStart = {
                            if (!committing) {
                                frozenNext = computedNext
                                committing = true
                            }
                            isProcessing = true
                        },
                        onAction = {
                            when (it) {
                                WordCardAction.Correct -> {
                                    if (settings.soundEnabled) {
                                        tone.startTone(
                                            android.media.ToneGenerator.TONE_PROP_ACK,
                                            100,
                                        )
                                    }
                                    scope.launch {
                                        engine.correct()
                                        isProcessing = false
                                    }
                                }
                                WordCardAction.Skip -> {
                                    if (s.skipsRemaining > 0) {
                                        if (settings.soundEnabled) {
                                            tone.startTone(
                                                android.media.ToneGenerator.TONE_PROP_NACK,
                                                100,
                                            )
                                        }
                                        scope.launch {
                                            engine.skip()
                                            isProcessing = false
                                        }
                                    } else {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        allowSkip = s.skipsRemaining > 0,
                        verticalMode = settings.verticalSwipes,
                        animateAppear = false,
                        wordDifficulty = currentMeta?.difficulty,
                        wordCategory = currentMeta?.category,
                        wordClass = currentMeta?.wordClass,
                    )
                }
            }
            val Controls: @Composable () -> Unit = {
                Text(
                    pluralStringResource(
                        R.plurals.time_remaining_seconds,
                        s.timeRemaining,
                        s.timeRemaining,
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(stringResource(R.string.team_label, s.team), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.remaining_label, s.remaining)) },
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(pluralStringResource(R.plurals.score_label, s.score, s.score)) },
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                pluralStringResource(R.plurals.skips_label, s.skipsRemaining, s.skipsRemaining),
                            )
                        },
                    )
                }
                Text(stringResource(R.string.summary_label, s.remaining, s.score, s.skipsRemaining))
            }

            if (isLandscape) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = barColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Controls()
                            val onCorrect = {
                                if (settings.hapticsEnabled) {
                                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                    vibrator?.vibrate(effect)
                                }
                                if (!isProcessing) {
                                    isProcessing = true
                                    scope.launch { engine.correct() }
                                }
                            }
                            val onSkip = {
                                if (settings.hapticsEnabled) {
                                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                    vibrator?.vibrate(effect)
                                }
                                if (!isProcessing) {
                                    if (s.skipsRemaining > 0) {
                                        isProcessing = true
                                        scope.launch { engine.skip() }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = onCorrect,
                                    enabled = !isProcessing,
                                    modifier = Modifier.weight(1f).height(60.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.correct))
                                }
                                Button(
                                    onClick = onSkip,
                                    enabled = !isProcessing && s.skipsRemaining > 0,
                                    modifier = Modifier.weight(1f).height(60.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.skip))
                                }
                            }
                            Button(
                                onClick = { vm.restartMatch() },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.restart_match)) }
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CardStack()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = barColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Controls()
                    CardStack()
                    if (settings.oneHandedLayout) {
                        val onCorrect = {
                            if (settings.hapticsEnabled) {
                                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                vibrator?.vibrate(effect)
                            }
                            if (!isProcessing) {
                                isProcessing = true
                                scope.launch { engine.correct() }
                            }
                        }
                        val onSkip = {
                            if (settings.hapticsEnabled) {
                                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                vibrator?.vibrate(effect)
                            }
                            if (!isProcessing) {
                                if (s.skipsRemaining > 0) {
                                    isProcessing = true
                                    scope.launch { engine.skip() }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = onSkip,
                                enabled = !isProcessing && s.skipsRemaining > 0,
                                modifier = Modifier.weight(1f).height(60.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.skip))
                            }
                            Button(
                                onClick = onCorrect,
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f).height(60.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.correct))
                            }
                        }
                    }
                    Button(onClick = {
                        if (settings.hapticsEnabled) {
                            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                            vibrator?.vibrate(effect)
                        }
                        vm.restartMatch()
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restart_match)) }
                }
            }
        }
        is GameState.TurnFinished -> {
            RoundSummaryScreen(vm = vm, s = s, settings = settings)
        }
        is GameState.MatchFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎉 Match over 🎉", style = MaterialTheme.typography.headlineSmall)
                Scoreboard(s.scores)
                Text(stringResource(R.string.start_new_match))
                Button(onClick = {
                    if (settings.hapticsEnabled) {
                        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        vibrator?.vibrate(effect)
                    }
                    vm.restartMatch()
                }) { Text(stringResource(R.string.restart_match)) }
            }
        }
    }
}

@Composable
private fun rememberCountdownState(scope: CoroutineScope): CountdownState {
    return remember(scope) { CountdownState(scope) }
}

@Stable
private class CountdownState(
    private val coroutineScope: CoroutineScope,
) {
    var value by mutableStateOf<Int?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun start(
        durationSeconds: Int = PRE_TURN_COUNTDOWN_SECONDS,
        onFinished: () -> Unit,
    ) {
        if (isRunning) return
        isRunning = true
        job = coroutineScope.launch {
            try {
                for (value in durationSeconds downTo 1) {
                    this@CountdownState.value = value
                    delay(1000)
                }
                onFinished()
            } finally {
                reset()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        reset()
    }

    private fun reset() {
        job = null
        value = null
        isRunning = false
    }
}
