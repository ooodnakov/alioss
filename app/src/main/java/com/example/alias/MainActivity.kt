package com.example.alias

import android.content.res.Configuration
import android.os.Bundle
import android.os.VibrationEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.alias.MainViewModel.UiEvent
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import com.example.alias.ui.AppScaffold
import com.example.alias.ui.CountdownOverlay
import com.example.alias.ui.HistoryScreen
import com.example.alias.ui.TutorialOverlay
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.google.accompanist.placeholder.material3.placeholder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import dagger.hilt.android.AndroidEntryPoint

private val LARGE_BUTTON_HEIGHT = 80.dp
private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS
private const val HISTORY_LIMIT = 50
private const val PRE_TURN_COUNTDOWN_SECONDS = 3



@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                val snack = remember { SnackbarHostState() }
                val settings by vm.settings.collectAsState()

                LaunchedEffect(settings.uiLanguage) {
                    applyLocalePreference(settings.uiLanguage)
                }

                // Collect general UI events and show snackbars
                LaunchedEffect(Unit) {
                    vm.uiEvents.collect { ev: UiEvent ->
                        if (ev.dismissCurrent) {
                            snack.currentSnackbarData?.dismiss()
                        }
                        val duration = if (ev.actionLabel != null && ev.duration == SnackbarDuration.Short) {
                            SnackbarDuration.Long
                        } else {
                            ev.duration
                        }
                        if (ev.message.isBlank()) {
                            return@collect
                        }
                        val result = snack.showSnackbar(
                            message = ev.message,
                            actionLabel = ev.actionLabel,
                            withDismissAction = ev.actionLabel == null,
                            duration = duration
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            ev.onAction?.invoke()
                        }
                    }
                }
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    enterTransition = { slideInHorizontally() },
                    exitTransition = { fadeOut() }
                ) {
                    composable("home") {
                        AppScaffold(snackbarHostState = snack) {
                            HomeScreen(
                                onQuickPlay = { vm.restartMatch(); nav.navigate("game") },
                                onDecks = { nav.navigate("decks") },
                                onSettings = { nav.navigate("settings") },
                                onHistory = { nav.navigate("history") }
                            )
                        }
                    }
                    composable("game") {
                        val engine by vm.engine.collectAsState()
                        val settings by vm.settings.collectAsState()
                        AppScaffold(snackbarHostState = snack) {
                            if (engine == null) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .placeholder(true)
                                ) {}
                            } else {
                                GameScreen(vm, engine!!, settings)
                            }
                        }
                    }
                    composable("decks") {
                        AppScaffold(snackbarHostState = snack) {
                            DecksScreen(vm = vm, onDeckSelected = { nav.navigate("deck/${it.id}") })
                        }
                    }
                    composable(
                        route = "deck/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = requireNotNull(backStackEntry.arguments?.getString("id"))
                        val decks by vm.decks.collectAsState()
                        val deck = decks.find { it.id == id }
                        if (deck == null) {
                            AppScaffold(
                                snackbarHostState = snack
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.deck_not_found))
                                }
                            }
                        } else {
                            AppScaffold(
                                snackbarHostState = snack
                            ) {
                                DeckDetailScreen(vm = vm, deck = deck)
                            }
                        }
                    }
                    composable("settings") {
                        AppScaffold(snackbarHostState = snack) {
                            SettingsScreen(
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onAbout = { nav.navigate("about") }
                            )
                        }
                    }
                    composable("history") {
                        AppScaffold(snackbarHostState = snack) {
                            val historyFlow = remember { vm.recentHistory(HISTORY_LIMIT) }
                            val history by historyFlow.collectAsState(initial = emptyList())
                            HistoryScreen(history)
                        }
                    }
                    composable("about") {
                        AppScaffold(snackbarHostState = snack) {
                            AboutScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onQuickPlay: () -> Unit,
    onDecks: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: brand + primary action
            Column(
                modifier = Modifier.weight(1.4f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                HomeLogo(size = 72.dp)
                HomeActionCard(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.quick_play),
                    subtitle = stringResource(R.string.quick_play_subtitle),
                    onClick = onQuickPlay,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            }
            // Right: secondary actions stacked
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeActionCard(
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    title = stringResource(R.string.title_decks),
                    subtitle = stringResource(R.string.decks_subtitle),
                    onClick = onDecks,
                    containerColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer
                )
                HomeActionCard(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.title_settings),
                    subtitle = stringResource(R.string.settings_subtitle),
                    onClick = onSettings,
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                )
                HomeActionCard(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.title_history),
                    subtitle = stringResource(R.string.history_subtitle),
                    onClick = onHistory,
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title / branding
            HomeLogo(size = 96.dp)
            // Primary actions as sleek cards
            HomeActionCard(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.quick_play),
                subtitle = stringResource(R.string.quick_play_subtitle),
                onClick = onQuickPlay,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            )
            HomeActionCard(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = stringResource(R.string.title_decks),
                subtitle = stringResource(R.string.decks_subtitle),
                onClick = onDecks,
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer
            )
            HomeActionCard(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.title_settings),
                subtitle = stringResource(R.string.settings_subtitle),
                onClick = onSettings,
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer
            )
            HomeActionCard(
                icon = Icons.Filled.History,
                title = stringResource(R.string.title_history),
                subtitle = stringResource(R.string.history_subtitle),
                onClick = onHistory,
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun HomeLogo(size: Dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    ElevatedCard(
        onClick = {
            vibrator?.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            onClick()
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

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
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    // Show tutorial overlay on first play (or when re-enabled via Settings)
    var showTutorial by rememberSaveable(settings.seenTutorial) { mutableStateOf(!settings.seenTutorial) }
    if (showTutorial) {
        com.example.alias.ui.TutorialOverlay(onDismiss = {
            showTutorial = false
            vm.updateSeenTutorial(true)
        })
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
                contentAlignment = Alignment.Center
            ) {
                // Padded content box (lower z-index)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .zIndex(0f),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
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
                                    .height(LARGE_BUTTON_HEIGHT)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.start_turn), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                // Full-screen overlay on top (higher z-index)
                countdownState.value?.let { value ->
                    CountdownOverlay(
                        value = value,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
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
                lerp(Color(0xFF4CAF50), Color(0xFFFFC107), t)
            } else {
                val t = rawProgress * 2f
                lerp(Color(0xFFFFC107), Color(0xFFF44336), 1 - t)
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
                // Word advanced: re-enable actions and unfreeze preview
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
                Box(Modifier.fillMaxWidth().height(200.dp)) {
                    if (nextWord != null) {
                        WordCard(
                            word = nextWord,
                            modifier = Modifier
                                .matchParentSize()
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
                        modifier = Modifier.matchParentSize().zIndex(1f),
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
                                    if (settings.soundEnabled) tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 100)
                                    scope.launch { engine.correct(); isProcessing = false }
                                }
                                WordCardAction.Skip -> {
                                    if (s.skipsRemaining > 0) {
                                        if (settings.soundEnabled) tone.startTone(android.media.ToneGenerator.TONE_PROP_NACK, 100)
                                        scope.launch { engine.skip(); isProcessing = false }
                                    } else { isProcessing = false }
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
                        s.timeRemaining
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Text(stringResource(R.string.team_label, s.team), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.remaining_label, s.remaining)) })
                    AssistChip(onClick = {}, enabled = false, label = { Text(pluralStringResource(R.plurals.score_label, s.score, s.score)) })
                    AssistChip(onClick = {}, enabled = false, label = { Text(pluralStringResource(R.plurals.skips_label, s.skipsRemaining, s.skipsRemaining)) })
                }
                Text(stringResource(R.string.summary_label, s.remaining, s.score, s.skipsRemaining))
            }

            if (isLandscape) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(progress = { progress }, color = barColor, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                            Button(onClick = onCorrect, enabled = !isProcessing, modifier = Modifier.weight(1f).height(60.dp)) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.correct)) }
                            Button(onClick = onSkip, enabled = !isProcessing && s.skipsRemaining > 0, modifier = Modifier.weight(1f).height(60.dp)) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.skip)) }
                            }
                            Button(onClick = { vm.restartMatch() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restart_match)) }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            CardStack()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(progress = { progress }, color = barColor, modifier = Modifier.fillMaxWidth())
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
                                } else {
                                    // No skips left; ignore without blocking input
                                }
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = onCorrect,
                                enabled = !isProcessing,
                                modifier = Modifier.fillMaxWidth().height(LARGE_BUTTON_HEIGHT)
                            ) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.correct)) }
                            Button(
                                onClick = onSkip,
                                enabled = !isProcessing && s.skipsRemaining > 0,
                                modifier = Modifier.fillMaxWidth().height(LARGE_BUTTON_HEIGHT)
                            ) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.skip)) }
                        }
                    } else {
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
                                } else {
                                    // No skips left; ignore without blocking input
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = onSkip,
                                enabled = !isProcessing && s.skipsRemaining > 0,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.skip)) }
                            Button(
                                onClick = onCorrect,
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.correct)) }
                            
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
            RoundSummaryScreen(vm = vm, s = s)
        }
        is GameState.MatchFinished -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
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
    private val coroutineScope: CoroutineScope
) {
    var value by mutableStateOf<Int?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun start(
        durationSeconds: Int = PRE_TURN_COUNTDOWN_SECONDS,
        onFinished: () -> Unit
    ) {
        if (isRunning) return
        isRunning = true
        job = coroutineScope.launch {
            try {
                for (value in durationSeconds downTo 1) {
                    this@CountdownState.value = value
                    kotlinx.coroutines.delay(1000)
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

@Composable
private fun DecksScreen(vm: MainViewModel, onDeckSelected: (DeckEntity) -> Unit) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
    val settings by vm.settings.collectAsState()
    val downloadProgress by vm.deckDownloadProgress.collectAsState()
    // Status snackbars are handled globally via vm.uiEvents
    var url by rememberSaveable { mutableStateOf("") }
    var sha by rememberSaveable { mutableStateOf("") }
    var newTrusted by rememberSaveable { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importDeckFromFile(it) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.title_decks), style = MaterialTheme.typography.headlineSmall)
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.filters_label), style = MaterialTheme.typography.titleMedium)
                    val availableCategories by vm.availableCategories.collectAsState()
                    val availableWordClasses by vm.availableWordClasses.collectAsState()
                    var selectedCats by rememberSaveable(settings) { mutableStateOf(settings.selectedCategories) }
                    var selectedClasses by rememberSaveable(settings) { mutableStateOf(settings.selectedWordClasses) }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var minDiff by rememberSaveable(settings) { mutableStateOf(settings.minDifficulty.toString()) }
                        var maxDiff by rememberSaveable(settings) { mutableStateOf(settings.maxDifficulty.toString()) }

                        OutlinedTextField(
                            value = minDiff,
                            onValueChange = { minDiff = it },
                            label = { Text(stringResource(R.string.min_difficulty_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = maxDiff,
                            onValueChange = { maxDiff = it },
                            label = { Text(stringResource(R.string.max_difficulty_label)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Button(onClick = {
                            val lo = minDiff.toIntOrNull() ?: settings.minDifficulty
                            val hi = maxDiff.toIntOrNull() ?: settings.maxDifficulty
                            vm.updateDifficultyFilter(lo, hi)
                            vm.updateCategoriesFilter(selectedCats)
                            vm.updateWordClassesFilter(selectedClasses)
                        }) {
                            Text(stringResource(R.string.apply_label))
                        }
                    }
                    FilterChipGroup(
                        title = stringResource(R.string.categories_label),
                        items = availableCategories,
                        selectedItems = selectedCats,
                        onSelectionChanged = { selectedCats = it }
                    )
                    FilterChipGroup(
                        title = stringResource(R.string.word_classes_label),
                        items = availableWordClasses,
                        selectedItems = selectedClasses,
                        onSelectionChanged = { selectedClasses = it }
                    )
                    Text(stringResource(R.string.filters_hint))
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.installed_decks), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { vm.setAllDecksEnabled(true) }) { Text(stringResource(R.string.enable_all)) }
                            TextButton(onClick = { vm.setAllDecksEnabled(false) }) { Text(stringResource(R.string.disable_all)) }
                        }
                    }
                    downloadProgress?.let {
                        DeckDownloadProgressIndicator(progress = it)
                        if (decks.isNotEmpty()) {
                            HorizontalDivider()
                        }
                    }
                    if (decks.isEmpty()) {
                        Text(stringResource(R.string.no_decks_installed))
                    } else {
                        decks.forEachIndexed { index, deck ->
                            val isEnabled = enabled.contains(deck.id)
                            ListItem(
                                headlineContent = { Text(deck.name) },
                                supportingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(onClick = {}, enabled = false, label = { Text(deck.language.uppercase()) })
                                        if (deck.isNSFW) AssistChip(onClick = {}, enabled = false, label = { Text("NSFW") })
                                    }
                                },
                                trailingContent = {
                                    Switch(checked = isEnabled, onCheckedChange = { vm.setDeckEnabled(deck.id, it) })
                                },
                                modifier = Modifier.clickable { onDeckSelected(deck) }
                            )
                            if (index < decks.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.import_download), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { filePicker.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.import_file)) }
                        OutlinedButton(onClick = {
                            runCatching {
                                val host = java.net.URI(url).host ?: ""
                                if (host.isNotBlank()) vm.addTrustedSource(host)
                            }
                        }) { Text(stringResource(R.string.trust_host)) }
                    }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.https_url)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sha, onValueChange = { sha = it }, label = { Text(stringResource(R.string.expected_sha256_optional)) }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { vm.downloadPackFromUrl(url, sha) }) { Text(stringResource(R.string.download_and_import)) }
                    }
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.trusted_sources), style = MaterialTheme.typography.titleMedium)
                    if (trusted.isEmpty()) {
                        Text(stringResource(R.string.no_trusted_sources_yet))
                    } else {
                        trusted.forEachIndexed { i, entry ->
                            ListItem(
                                headlineContent = { Text(entry) },
                                trailingContent = {
                                    IconButton(onClick = { vm.removeTrustedSource(entry) }) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
                                }
                            )
                            if (i < trusted.size - 1) HorizontalDivider()
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newTrusted, onValueChange = { newTrusted = it }, label = { Text(stringResource(R.string.add_host_origin)) }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { if (newTrusted.isNotBlank()) { vm.addTrustedSource(newTrusted.trim()); newTrusted = "" } }) { Text(stringResource(R.string.add)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckDownloadProgressIndicator(progress: MainViewModel.DeckDownloadProgress) {
    val totalBytes = progress.totalBytes?.takeIf { it > 0L }
    val fraction = totalBytes?.let { bytesTotal ->
        val clamped = progress.bytesRead.coerceAtMost(bytesTotal)
        (clamped.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
    }
    val statusText = when (progress.step) {
        MainViewModel.DeckDownloadStep.DOWNLOADING -> fraction?.let {
            stringResource(R.string.deck_download_percent, (it * 100).roundToInt())
        } ?: stringResource(R.string.deck_download_downloading)

        MainViewModel.DeckDownloadStep.IMPORTING -> stringResource(R.string.deck_download_importing)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(statusText, style = MaterialTheme.typography.bodyMedium)
        val indicatorModifier = Modifier.fillMaxWidth()
        if (fraction != null && progress.step == MainViewModel.DeckDownloadStep.DOWNLOADING) {
            LinearProgressIndicator(progress = { fraction }, modifier = indicatorModifier)
        } else {
            LinearProgressIndicator(modifier = indicatorModifier)
        }
    }
}

@Composable
private fun FilterChipGroup(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
) {
    if (items.isEmpty()) return
    Text(title)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val selected = selectedItems.contains(item)
            FilterChip(
                selected = selected,
                onClick = {
                    val updatedSelection = if (selected) selectedItems - item else selectedItems + item
                    onSelectionChanged(updatedSelection)
                },
                label = { Text(item) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckDetailScreen(vm: MainViewModel, deck: DeckEntity) {
    var count by remember { mutableStateOf<Int?>(null) }
    var categories by remember { mutableStateOf<List<String>?>(null) }
    var wordExamples by remember { mutableStateOf<List<String>>(emptyList()) }
    var examplesLoading by remember { mutableStateOf(false) }
    var examplesError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshExamples() {
        examplesLoading = true
        examplesError = false
        val result = runCatching { vm.getDeckWordSamples(deck.id) }
        wordExamples = result.getOrDefault(emptyList())
        examplesError = result.isFailure
        examplesLoading = false
    }

    LaunchedEffect(deck.id) {
        launch { count = vm.getWordCount(deck.id) }
        launch { categories = runCatching { vm.getDeckCategories(deck.id) }.getOrElse { emptyList() } }
        launch { refreshExamples() }
    }

    val configuration = LocalConfiguration.current
    val downloadDateText = remember(deck.updatedAt, configuration) {
        val timestamp = deck.updatedAt
        if (timestamp <= 0L) {
            null
        } else {
            val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(deck.name, style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, enabled = false, label = { Text(deck.language.uppercase()) })
            if (deck.isOfficial) {
                AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_official_label)) })
            }
            if (deck.isNSFW) {
                AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_nsfw_label)) })
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val countText = count?.toString() ?: "…"
            Text(stringResource(R.string.deck_word_count, countText))
            Text(stringResource(R.string.deck_version_label, deck.version))
            Text(
                downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                    ?: stringResource(R.string.deck_downloaded_unknown)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.deck_categories_title), style = MaterialTheme.typography.titleMedium)
            when (val currentCategories = categories) {
                null -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                else -> {
                    if (currentCategories.isEmpty()) {
                        Text(stringResource(R.string.deck_categories_empty))
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentCategories.forEach { category ->
                                AssistChip(onClick = {}, enabled = false, label = { Text(category) })
                            }
                        }
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.deck_examples_title), style = MaterialTheme.typography.titleMedium)
                when {
                    examplesLoading -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.deck_examples_loading))
                        }
                    }
                    examplesError -> {
                        Text(stringResource(R.string.deck_examples_error), color = MaterialTheme.colorScheme.error)
                    }
                    wordExamples.isEmpty() -> {
                        Text(stringResource(R.string.deck_examples_empty))
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            wordExamples.forEach { example ->
                                Text("• ${example}")
                            }
                        }
                    }
                }
                TextButton(onClick = { scope.launch { refreshExamples() } }, enabled = !examplesLoading) {
                    Text(stringResource(R.string.deck_examples_reload))
                }
            }
        }
    }
}

private enum class SettingsTab(@StringRes val titleRes: Int) {
    MATCH_RULES(R.string.match_rules_tab),
    INPUT_FEEDBACK(R.string.input_feedback_tab),
    TEAMS(R.string.teams_tab),
    ADVANCED(R.string.advanced_tab)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var round by rememberSaveable(s) { mutableStateOf(s.roundSeconds.toString()) }
    var target by rememberSaveable(s) { mutableStateOf(s.targetWords.toString()) }
    var maxSkips by rememberSaveable(s) { mutableStateOf(s.maxSkips.toString()) }
    var penalty by rememberSaveable(s) { mutableStateOf(s.penaltyPerSkip.toString()) }
    var lang by rememberSaveable(s) { mutableStateOf(s.languagePreference) }
    var uiLang by rememberSaveable(s) { mutableStateOf(s.uiLanguage) }
    var punishSkips by rememberSaveable(s) { mutableStateOf(s.punishSkips) }
    var nsfw by rememberSaveable(s) { mutableStateOf(s.allowNSFW) }
    var haptics by rememberSaveable(s) { mutableStateOf(s.hapticsEnabled) }
    var sound by rememberSaveable(s) { mutableStateOf(s.soundEnabled) }
    var oneHand by rememberSaveable(s) { mutableStateOf(s.oneHandedLayout) }
    var verticalSwipes by rememberSaveable(s) { mutableStateOf(s.verticalSwipes) }
    var orientation by rememberSaveable(s) { mutableStateOf(s.orientation) }
    var teams by rememberSaveable(s) { mutableStateOf(s.teams) }
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.MATCH_RULES) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val teamSuggestions = stringArrayResource(R.array.team_name_suggestions).toList()

    val canSave = teams.count { it.isNotBlank() } >= MIN_TEAMS
    val applySettings: () -> Job = {
        scope.launch {
            vm.updateSettings(
                roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                targetWords = target.toIntOrNull() ?: s.targetWords,
                maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                punishSkips = punishSkips,
                language = lang.ifBlank { s.languagePreference },
                uiLanguage = uiLang,
                allowNSFW = nsfw,
                haptics = haptics,
                sound = sound,
                oneHanded = oneHand,
                verticalSwipes = verticalSwipes,
                orientation = orientation,
                teams = teams,
            )
        }
    }

    if (showResetDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showResetDialog = false }) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.reset_confirm_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.reset_confirm_message))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showResetDialog = false }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(onClick = {
                            showResetDialog = false
                            vm.resetLocalData()
                        }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            SettingsTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(stringResource(tab.titleRes)) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(targetState = selectedTab, label = "settings_tabs") { tab ->
                when (tab) {
                    SettingsTab.MATCH_RULES -> MatchRulesTab(
                        round = round,
                        onRoundChange = { round = it },
                        target = target,
                        onTargetChange = { target = it },
                        maxSkips = maxSkips,
                        onMaxSkipsChange = { maxSkips = it },
                        penalty = penalty,
                        onPenaltyChange = { penalty = it },
                        punishSkips = punishSkips,
                        onPunishSkipsChange = { punishSkips = it }
                    )

                    SettingsTab.INPUT_FEEDBACK -> InputFeedbackTab(
                        haptics = haptics,
                        onHapticsChange = { haptics = it },
                        sound = sound,
                        onSoundChange = { sound = it },
                        oneHand = oneHand,
                        onOneHandChange = { oneHand = it },
                        verticalSwipes = verticalSwipes,
                        onVerticalSwipesChange = { verticalSwipes = it },
                        orientation = orientation,
                        onOrientationChange = {
                            orientation = it
                            vm.setOrientation(it)
                        }
                    )

                    SettingsTab.TEAMS -> TeamsTab(
                        teams = teams,
                        canRemoveTeam = teams.size > MIN_TEAMS,
                        canAddTeam = teams.size < MAX_TEAMS,
                        onTeamNameChange = { index, value ->
                            teams = teams.toMutableList().also { list -> list[index] = value }
                        },
                        onTeamRemove = { index ->
                            teams = teams.toMutableList().also { list -> list.removeAt(index) }
                        },
                        onTeamAdd = {
                            teams = teams + ctx.getString(R.string.team_default_name, teams.size + 1)
                        },
                        onTeamMove = { from, to ->
                            if (from == to) return@TeamsTab
                            if (from !in teams.indices || to !in 0..teams.size) return@TeamsTab
                            val updated = teams.toMutableList()
                            val item = updated.removeAt(from)
                            val targetIndex = to.coerceIn(0, updated.size)
                            updated.add(targetIndex, item)
                            teams = updated
                        },
                        suggestions = teamSuggestions,
                        onApplySuggestion = { suggestion ->
                            if (teams.any { it.equals(suggestion, ignoreCase = true) }) {
                                return@TeamsTab
                            }
                            val targetIndex = teams.indexOfFirst { it.isBlank() }
                            teams = teams.toMutableList().also { list ->
                                when {
                                    targetIndex >= 0 -> list[targetIndex] = suggestion
                                    list.size < MAX_TEAMS -> list.add(suggestion)
                                }
                            }
                        }
                    )

                    SettingsTab.ADVANCED -> AdvancedTab(
                        uiLanguage = uiLang,
                        onUiLanguageChange = { uiLang = it },
                        language = lang,
                        onLanguageChange = { lang = it },
                        allowNsfw = nsfw,
                        onAllowNsfwChange = { nsfw = it },
                        onShowTutorialAgain = { vm.updateSeenTutorial(false) },
                        onAbout = onAbout,
                        onReset = { showResetDialog = true }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { applySettings() }, enabled = canSave, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save_label))
            }
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        applySettings().join()
                        vm.restartMatch()
                        onBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save_and_restart_label))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun MatchRulesTab(
    round: String,
    onRoundChange: (String) -> Unit,
    target: String,
    onTargetChange: (String) -> Unit,
    maxSkips: String,
    onMaxSkipsChange: (String) -> Unit,
    penalty: String,
    onPenaltyChange: (String) -> Unit,
    punishSkips: Boolean,
    onPunishSkipsChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.round_and_goals), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = round,
                        onValueChange = onRoundChange,
                        label = { Text(stringResource(R.string.round_seconds_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = onTargetChange,
                        label = { Text(stringResource(R.string.target_words_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.skips_section), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = maxSkips,
                            onValueChange = onMaxSkipsChange,
                            label = { Text(stringResource(R.string.max_skips_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = penalty,
                            onValueChange = onPenaltyChange,
                            label = { Text(stringResource(R.string.penalty_per_skip_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.punish_skips_label), modifier = Modifier.weight(1f))
                        Switch(checked = punishSkips, onCheckedChange = onPunishSkipsChange)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputFeedbackTab(
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    sound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    oneHand: Boolean,
    onOneHandChange: (Boolean) -> Unit,
    verticalSwipes: Boolean,
    onVerticalSwipesChange: (Boolean) -> Unit,
    orientation: String,
    onOrientationChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.feedback_and_layout), style = MaterialTheme.typography.titleMedium)
                    SettingsToggleRow(
                        label = stringResource(R.string.haptics_label),
                        checked = haptics,
                        onCheckedChange = onHapticsChange
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.sound_effects_label),
                        checked = sound,
                        onCheckedChange = onSoundChange
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.one_hand_layout_label),
                        checked = oneHand,
                        onCheckedChange = onOneHandChange
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.vertical_swipes_label),
                        checked = verticalSwipes,
                        onCheckedChange = onVerticalSwipesChange
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.orientation_label), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrientationChip(
                            selected = orientation == "system",
                            label = stringResource(R.string.auto_label),
                            icon = Icons.Filled.ScreenRotation,
                            onClick = { onOrientationChange("system") }
                        )
                        OrientationChip(
                            selected = orientation == "portrait",
                            label = stringResource(R.string.portrait_label),
                            icon = Icons.Filled.ScreenLockPortrait,
                            onClick = { onOrientationChange("portrait") }
                        )
                        OrientationChip(
                            selected = orientation == "landscape",
                            label = stringResource(R.string.landscape_label),
                            icon = Icons.Filled.ScreenLockLandscape,
                            onClick = { onOrientationChange("landscape") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun OrientationChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamsTab(
    teams: List<String>,
    canRemoveTeam: Boolean,
    canAddTeam: Boolean,
    onTeamNameChange: (Int, String) -> Unit,
    onTeamRemove: (Int) -> Unit,
    onTeamAdd: () -> Unit,
    onTeamMove: (Int, Int) -> Unit,
    suggestions: List<String>,
    onApplySuggestion: (String) -> Unit,
) {
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.teams_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.drag_to_reorder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        itemsIndexed(teams, key = { index, name -> "$index-$name" }) { index, name ->
            val isDragging = draggingIndex == index
            TeamEditorCard(
                index = index,
                name = name,
                canRemove = canRemoveTeam,
                onNameChange = onTeamNameChange,
                onRemove = onTeamRemove,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> itemHeights[index] = coords.size.height }
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                handleModifier = Modifier.pointerInput(teams) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffset += dragAmount.y
                            if (dragOffset > 0 && current < teams.lastIndex) {
                                val height = itemHeights[current] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset > height * 0.6f) {
                                    onTeamMove(current, current + 1)
                                    draggingIndex = current + 1
                                    dragOffset = 0f
                                }
                            } else if (dragOffset < 0 && current > 0) {
                                val height = itemHeights[current - 1] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset < -height * 0.6f) {
                                    onTeamMove(current, current - 1)
                                    draggingIndex = current - 1
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = 0f
                        }
                    )
                },
                isDragging = isDragging
            )
        }
        item {
            if (canAddTeam) {
                OutlinedButton(onClick = onTeamAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_team_label), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (suggestions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.team_suggestions_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.team_suggestions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(onClick = { onApplySuggestion(suggestion) }, label = { Text(suggestion) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamEditorCard(
    index: Int,
    name: String,
    canRemove: Boolean,
    onNameChange: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    isDragging: Boolean,
) {
    val elevation = if (isDragging) 8.dp else 2.dp
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: (index + 1).toString()
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initial, style = MaterialTheme.typography.titleMedium)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(index, it) },
                    label = { Text(stringResource(R.string.team_default_name, index + 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { onRemove(index) }, enabled = canRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_team))
                }
                Box(
                    modifier = handleModifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DragHandle, contentDescription = stringResource(R.string.team_drag_handle_description))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedTab(
    uiLanguage: String,
    onUiLanguageChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    allowNsfw: Boolean,
    onAllowNsfwChange: (Boolean) -> Unit,
    onShowTutorialAgain: () -> Unit,
    onAbout: () -> Unit,
    onReset: () -> Unit,
) {
    val selectedLanguage = remember(uiLanguage) { resolveUiLanguageSelection(uiLanguage) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.language_and_content), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ui_language_label))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedLanguage == "system",
                            onClick = { onUiLanguageChange("system") },
                            label = { Text(stringResource(R.string.system_default_label)) }
                        )
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = { onUiLanguageChange("en") },
                            label = { Text(stringResource(R.string.english_label)) }
                        )
                        FilterChip(
                            selected = selectedLanguage == "ru",
                            onClick = { onUiLanguageChange("ru") },
                            label = { Text(stringResource(R.string.russian_label)) }
                        )
                    }
                    OutlinedTextField(
                        value = language,
                        onValueChange = onLanguageChange,
                        label = { Text(stringResource(R.string.language_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.allow_nsfw_label),
                        checked = allowNsfw,
                        onCheckedChange = onAllowNsfwChange
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.support_and_data_label), style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.show_tutorial_again)) },
                        modifier = Modifier.clickable { onShowTutorialAgain() }
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.title_about)) },
                        modifier = Modifier.clickable(onClick = onAbout)
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.reset_local_data)) },
                        modifier = Modifier.clickable { onReset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val version = BuildConfig.VERSION_NAME
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero card
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = colors.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.version_label, version), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.links_label), style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.source_code_label)) },
                        supportingContent = { Text("github.com/ooodnakov/alias-game") },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/ooodnakov/alias-game") }
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.report_issue_label)) },
                        supportingContent = { Text(stringResource(R.string.open_github_issues_label)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/ooodnakov/alias-game/issues") }
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.title_about), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.author_line), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.privacy_line), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RoundSummaryScreen(vm: MainViewModel, s: GameState.TurnFinished) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        Text(stringResource(R.string.turn_summary, s.team), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.score_change, s.deltaScore))
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(s.outcomes) { index, o ->
                ListItem(
                    leadingContent = {
                        Icon(
                            if (o.correct) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (o.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    },
                    headlineContent = { Text(o.word) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { vm.overrideOutcome(index, true) }) {
                                Icon(Icons.Filled.Check, contentDescription = "Mark correct")
                            }
                            IconButton(onClick = { vm.overrideOutcome(index, false) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Mark incorrect")
                            }
                        }
                    }
                )
                if (index < s.outcomes.lastIndex) HorizontalDivider()
            }
        }
        Scoreboard(s.scores)
        Button(onClick = { vm.nextTurn() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (s.matchOver) stringResource(R.string.end_match) else stringResource(R.string.next_team))
        }
    }
}

@Composable
private fun Scoreboard(scores: Map<String, Int>) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.scoreboard), style = MaterialTheme.typography.titleMedium)
        val max = scores.values.maxOrNull() ?: 0
        val leaders = scores.filterValues { it == max }.keys
        scores.forEach { (team, score) ->
            val isLeader = leaders.contains(team)
            val suffix = if (leaders.size > 1 && isLeader) stringResource(R.string.tie_suffix) else ""
            val textStyle = if (isLeader) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            }
            val textColor = if (isLeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLeader) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
                Text(
                    text = "$team: $score$suffix",
                    style = textStyle,
                    color = textColor
                )
            }
        }
    }
}

private fun resolveUiLanguageSelection(raw: String): String {
    if (raw.equals("system", ignoreCase = true) || raw.isBlank()) {
        return "system"
    }
    val firstTag = raw.split(',').firstOrNull()?.trim().orEmpty()
    if (firstTag.isEmpty()) {
        return raw
    }
    val locale = Locale.forLanguageTag(firstTag)
    val language = locale.language
    return if (language.isNullOrEmpty()) raw else language
}
