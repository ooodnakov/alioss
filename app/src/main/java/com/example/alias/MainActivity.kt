package com.example.alias

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import android.os.VibrationEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import android.content.res.Configuration
import android.os.VibrationEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.example.alias.data.settings.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.alias.MainViewModel.UiEvent
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.example.alias.ui.TutorialOverlay
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.db.DifficultyBucket
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Brush
import kotlin.math.absoluteValue
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
                        val engine by vm.engine.collectAsState()
                        val gameState = engine?.let { current ->
                            val state by current.state.collectAsState()
                            state
                        }
                        val decks by vm.decks.collectAsState()
                        val recentHistoryFlow = remember { vm.recentHistory(12) }
                        val recentHistory by recentHistoryFlow.collectAsState(initial = emptyList())
                        AppScaffold(snackbarHostState = snack) {
                            HomeScreen(
                                gameState = gameState,
                                settings = settings,
                                decks = decks,
                                recentHistory = recentHistory,
                                onResumeMatch = { nav.navigate("game") },
                                onStartNewMatch = { vm.restartMatch(); nav.navigate("game") },
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
    gameState: GameState?,
    settings: Settings,
    decks: List<DeckEntity>,
    recentHistory: List<TurnHistoryEntity>,
    onResumeMatch: () -> Unit,
    onStartNewMatch: () -> Unit,
    onDecks: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1.4f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HomeHeroSection(
                    gameState = gameState,
                    settings = settings,
                    decks = decks,
                    recentHistory = recentHistory,
                    onResumeMatch = onResumeMatch,
                    onStartNewMatch = onStartNewMatch,
                    onHistory = onHistory,
                    onDecks = onDecks
                )
                HomeActionCard(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.quick_play),
                    subtitle = stringResource(R.string.quick_play_subtitle),
                    onClick = onStartNewMatch,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            }
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeHeroSection(
                gameState = gameState,
                settings = settings,
                decks = decks,
                recentHistory = recentHistory,
                onResumeMatch = onResumeMatch,
                onStartNewMatch = onStartNewMatch,
                onHistory = onHistory,
                onDecks = onDecks
            )
            HomeActionCard(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.quick_play),
                subtitle = stringResource(R.string.quick_play_subtitle),
                onClick = onStartNewMatch,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeroSection(
    gameState: GameState?,
    settings: Settings,
    decks: List<DeckEntity>,
    recentHistory: List<TurnHistoryEntity>,
    onResumeMatch: () -> Unit,
    onStartNewMatch: () -> Unit,
    onHistory: () -> Unit,
    onDecks: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = colors.onPrimaryContainer
    val gradient = remember(colors) {
        Brush.verticalGradient(
            0f to colors.primary.copy(alpha = 0.35f),
            1f to Color.Transparent
        )
    }
    val liveScores = when (gameState) {
        is GameState.TurnFinished -> gameState.scores
        is GameState.MatchFinished -> gameState.scores
        else -> null
    }
    val scoreboardState = rememberSaveable(settings.teams, saver = ScoreboardSaver) {
        mutableStateMapOf<String, Int>().apply {
            settings.teams.forEach { team -> this[team] = 0 }
        }
    }
    LaunchedEffect(liveScores) {
        if (liveScores != null) {
            scoreboardState.clear()
            settings.teams.forEach { team ->
                scoreboardState[team] = liveScores[team] ?: 0
            }
        }
    }
    val scoreboard = liveScores ?: scoreboardState.toMap()
    val hasProgress = recentHistory.isNotEmpty() || scoreboard.values.any { it != 0 }
    val heroTitle = when (gameState) {
        is GameState.MatchFinished -> stringResource(R.string.home_hero_title_victory)
        is GameState.TurnFinished -> if (gameState.matchOver) {
            stringResource(R.string.home_hero_title_victory)
        } else {
            stringResource(R.string.home_hero_title_ready)
        }
        is GameState.TurnActive -> stringResource(R.string.home_hero_title_playing)
        is GameState.TurnPending -> stringResource(R.string.home_hero_title_ready)
        else -> stringResource(R.string.home_hero_title_idle)
    }
    val heroSubtitle = when (gameState) {
        null, GameState.Idle -> stringResource(R.string.home_hero_idle_subtitle, settings.teams.size)
        is GameState.TurnPending -> stringResource(R.string.home_hero_pending_subtitle, gameState.team)
        is GameState.TurnActive -> stringResource(R.string.home_hero_active_subtitle, gameState.team, gameState.timeRemaining)
        is GameState.TurnFinished -> if (gameState.matchOver) {
            stringResource(R.string.home_match_point, gameState.team)
        } else {
            stringResource(R.string.home_hero_finished_subtitle, gameState.team, gameState.deltaScore)
        }
        is GameState.MatchFinished -> {
            val maxScore = scoreboard.maxOfOrNull { it.value }
            if (maxScore == null) {
                stringResource(R.string.home_match_finished_tie, 0)
            } else {
                val winners = scoreboard.filterValues { it == maxScore }.keys
                if (winners.size > 1) {
                    stringResource(R.string.home_match_finished_tie, maxScore)
                } else {
                    stringResource(R.string.home_match_finished_winner, winners.first(), maxScore)
                }
            }
        }
    }
    val favoriteDecks = remember(settings.enabledDeckIds, decks) {
        val enabled = settings.enabledDeckIds
        decks.filter { enabled.contains(it.id) }
            .sortedBy { it.name }
            .take(3)
    }
    val extraDecks = (settings.enabledDeckIds.size - favoriteDecks.size).coerceAtLeast(0)
    val highlight = recentHistory.firstOrNull()
    val highlightText = when {
        highlight == null -> stringResource(R.string.home_highlight_empty)
        highlight.correct -> stringResource(R.string.home_highlight_correct, highlight.team, highlight.word)
        else -> stringResource(R.string.home_highlight_skip, highlight.team, highlight.word)
    }
    val highlightIcon = when {
        highlight == null -> null
        highlight.correct -> Icons.Filled.Check
        else -> Icons.Filled.Close
    }
    val highlightTint = when {
        highlight == null -> contentColor.copy(alpha = 0.7f)
        highlight.correct -> colors.tertiary
        else -> colors.error
    }
    val showResume = gameState != null && gameState !is GameState.Idle

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.primaryContainer,
            contentColor = contentColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeLogo(size = 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(heroTitle, style = MaterialTheme.typography.headlineSmall, color = contentColor)
                        Text(heroSubtitle, style = MaterialTheme.typography.bodyLarge, color = contentColor.copy(alpha = 0.9f))
                    }
                }
                HomeScoreboardSection(scoreboard = scoreboard, hasProgress = hasProgress, contentColor = contentColor)
                FavoriteDecksSection(
                    favorites = favoriteDecks,
                    extra = extraDecks,
                    onDecks = onDecks,
                    contentColor = contentColor
                )
                RecentHighlightSection(
                    text = highlightText,
                    icon = highlightIcon,
                    iconTint = highlightTint,
                    contentColor = contentColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showResume) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onResumeMatch,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.resume_match))
                            }
                            OutlinedButton(
                                onClick = onStartNewMatch,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.start_new_game))
                            }
                        }
                    } else {
                        Button(
                            onClick = onStartNewMatch,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.start_new_game))
                        }
                    }
                    TextButton(
                        onClick = onHistory,
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) {
                        Text(stringResource(R.string.view_history))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScoreboardSection(
    scoreboard: Map<String, Int>,
    hasProgress: Boolean,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.scoreboard),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f)
        )
        if (!hasProgress) {
            Text(
                text = stringResource(R.string.home_scoreboard_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scoreboard.entries.sortedByDescending { it.value }.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = contentColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.key, style = MaterialTheme.typography.bodyMedium, color = contentColor)
                            Text(entry.value.toString(), style = MaterialTheme.typography.titleSmall, color = contentColor.copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteDecksSection(
    favorites: List<DeckEntity>,
    extra: Int,
    onDecks: () -> Unit,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_favorite_decks),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f)
        )
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.home_empty_favorites),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favorites.forEach { deck ->
                    AssistChip(
                        onClick = onDecks,
                        label = { Text(deck.name) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = contentColor.copy(alpha = 0.08f),
                            labelColor = contentColor,
                            leadingIconContentColor = contentColor
                        )
                    )
                }
                if (extra > 0) {
                    AssistChip(
                        onClick = onDecks,
                        label = { Text("+${extra}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = contentColor.copy(alpha = 0.08f),
                            labelColor = contentColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentHighlightSection(
    text: String,
    icon: ImageVector?,
    iconTint: Color,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_recent_highlight),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = contentColor.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            }
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

private val ScoreboardSaver: Saver<SnapshotStateMap<String, Int>, Map<String, Int>> = Saver(
    save = { it.toMap() },
    restore = { restored -> mutableStateMapOf<String, Int>().apply { putAll(restored) } }
)

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
        TutorialOverlay(
            verticalMode = settings.verticalSwipes,
            onDismiss = {
                showTutorial = false
                vm.updateSeenTutorial(true)
            },
            modifier = Modifier.zIndex(1f)
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DecksScreen(vm: MainViewModel, onDeckSelected: (DeckEntity) -> Unit) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
    val settings by vm.settings.collectAsState()
    val downloadProgress by vm.deckDownloadProgress.collectAsState()
    val availableCategories by vm.availableCategories.collectAsState()
    val availableWordClasses by vm.availableWordClasses.collectAsState()

    var url by rememberSaveable { mutableStateOf("") }
    var sha by rememberSaveable { mutableStateOf("") }
    var newTrusted by rememberSaveable { mutableStateOf("") }

    var minDifficulty by rememberSaveable(settings) { mutableStateOf(settings.minDifficulty.toString()) }
    var maxDifficulty by rememberSaveable(settings) { mutableStateOf(settings.maxDifficulty.toString()) }
    var selectedCategories by rememberSaveable(settings) { mutableStateOf(settings.selectedCategories) }
    var selectedWordClasses by rememberSaveable(settings) { mutableStateOf(settings.selectedWordClasses) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importDeckFromFile(it) }
    }

    var activeSheet by rememberSaveable { mutableStateOf<DeckSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState
        ) {
            when (sheet) {
                DeckSheet.FILTERS -> DeckFiltersSheet(
                    state = DeckFiltersSheetState(
                        difficulty = DifficultyFilterState(
                            minDifficulty = minDifficulty,
                            maxDifficulty = maxDifficulty
                        ),
                        categories = FilterSelectionState(
                            available = availableCategories,
                            selected = selectedCategories
                        ),
                        wordClasses = FilterSelectionState(
                            available = availableWordClasses,
                            selected = selectedWordClasses
                        )
                    ),
                    callbacks = DeckFiltersSheetCallbacks(
                        onMinDifficultyChange = { minDifficulty = it },
                        onMaxDifficultyChange = { maxDifficulty = it },
                        onCategoriesChange = { selectedCategories = it },
                        onWordClassesChange = { selectedWordClasses = it },
                        onApply = {
                            val lo = minDifficulty.toIntOrNull() ?: settings.minDifficulty
                            val hi = maxDifficulty.toIntOrNull() ?: settings.maxDifficulty
                            vm.updateDifficultyFilter(lo, hi)
                            vm.updateCategoriesFilter(selectedCategories)
                            vm.updateWordClassesFilter(selectedWordClasses)
                            activeSheet = null
                        }
                    )
                )

                DeckSheet.IMPORT -> DeckImportSheet(
                    state = DeckImportSheetState(
                        url = url,
                        sha256 = sha
                    ),
                    callbacks = DeckImportSheetCallbacks(
                        onUrlChange = { url = it },
                        onShaChange = { sha = it },
                        onPickFile = { filePicker.launch(arrayOf("application/json")) },
                        onDownload = {
                            vm.downloadPackFromUrl(url, sha)
                            activeSheet = null
                        },
                        onOpenTrusted = { activeSheet = DeckSheet.TRUSTED }
                    )
                )

                DeckSheet.TRUSTED -> DeckTrustedSourcesSheet(
                    trustedSources = trusted.toList(),
                    newSource = newTrusted,
                    onNewSourceChange = { newTrusted = it },
                    onRemove = { vm.removeTrustedSource(it) },
                    onAdd = {
                        val trimmed = newTrusted.trim()
                        if (trimmed.isNotEmpty()) {
                            vm.addTrustedSource(trimmed)
                            newTrusted = ""
                        }
                    }
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DecksHeroSummary(
                    state = DecksHeroSummaryState(
                        decks = decks,
                        enabledDeckIds = enabled
                    ),
                    actions = DecksHeroSummaryActions(
                        onFiltersClick = { activeSheet = DeckSheet.FILTERS },
                        onEnableAll = { vm.setAllDecksEnabled(true) },
                        onDisableAll = { vm.setAllDecksEnabled(false) },
                        onManageSources = { activeSheet = DeckSheet.TRUSTED }
                    )
                )
            }
            val progress = downloadProgress
            if (progress != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        DeckDownloadProgressIndicator(
                            progress = progress,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (decks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyDecksState(onImportClick = { activeSheet = DeckSheet.IMPORT })
                }
            } else {
                items(decks, key = { it.id }) { deck ->
                    DeckCard(
                        deck = deck,
                        enabled = enabled.contains(deck.id),
                        onToggle = { vm.setDeckEnabled(deck.id, it) },
                        onClick = { onDeckSelected(deck) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { activeSheet = DeckSheet.IMPORT },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = { Icon(Icons.Filled.Download, contentDescription = null) },
            text = { Text(stringResource(R.string.import_decks_action)) }
        )
    }
}

private enum class DeckSheet { FILTERS, IMPORT, TRUSTED }

private data class DecksHeroSummaryState(
    val decks: List<DeckEntity>,
    val enabledDeckIds: Set<String>,
)

private class DecksHeroSummaryActions(
    val onFiltersClick: () -> Unit,
    val onEnableAll: () -> Unit,
    val onDisableAll: () -> Unit,
    val onManageSources: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecksHeroSummary(
    state: DecksHeroSummaryState,
    actions: DecksHeroSummaryActions,
    modifier: Modifier = Modifier,
) {
    val enabledDecks = remember(state.decks, state.enabledDeckIds) {
        state.decks.filter { state.enabledDeckIds.contains(it.id) }
    }
    val activeCount = enabledDecks.size
    val languages = remember(enabledDecks) {
        enabledDecks.map { it.language.uppercase(Locale.getDefault()) }.distinct()
    }
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.title_decks),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.deck_active_summary, activeCount, state.decks.size),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            DeckLanguagesSummary(languages = languages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = actions.onFiltersClick) {
                    Icon(Icons.Filled.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_filters))
                }
                OutlinedButton(onClick = actions.onManageSources) {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.manage_trusted_sources))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = actions.onEnableAll) { Text(stringResource(R.string.enable_all)) }
                TextButton(onClick = actions.onDisableAll) { Text(stringResource(R.string.disable_all)) }
            }
            Text(
                text = stringResource(R.string.filters_hint),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckLanguagesSummary(languages: List<String>, modifier: Modifier = Modifier) {
    if (languages.isNotEmpty()) {
        Text(
            text = stringResource(R.string.deck_languages_summary, languages.joinToString(" • ")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { language ->
                AssistChip(onClick = {}, enabled = false, label = { Text(language) })
            }
        }
    } else {
        Text(
            text = stringResource(R.string.deck_languages_none),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckCard(
    deck: DeckEntity,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            DeckCoverArt(deck = deck)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(deck.language.uppercase(Locale.getDefault())) })
                    if (deck.isOfficial) {
                        AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_official_label)) })
                    }
                    if (deck.isNSFW) {
                        AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.deck_nsfw_label)) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (enabled) stringResource(R.string.deck_card_enabled) else stringResource(R.string.deck_card_disabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = enabled, onCheckedChange = onToggle)
                }
                Text(
                    text = stringResource(R.string.deck_card_view_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DeckCoverArt(deck: DeckEntity, modifier: Modifier = Modifier) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val initial = remember(deck.id, deck.name) {
        deck.name.firstOrNull()?.uppercaseChar()?.toString()
            ?: deck.language.uppercase(Locale.getDefault())
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(gradient)
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.align(Alignment.Center)
        )
        Text(
            text = stringResource(R.string.deck_cover_language, deck.language.uppercase(Locale.getDefault())),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

private val DeckCoverPalette = listOf(
    Color(0xFF6C63FF),
    Color(0xFF00BFA5),
    Color(0xFFFF7043),
    Color(0xFF7E57C2),
    Color(0xFF26C6DA),
    Color(0xFFF06292),
)

@Composable
private fun rememberDeckCoverBrush(deckId: String): Brush {
    val colors = remember(deckId) {
        val baseIndex = deckId.hashCode().absoluteValue % DeckCoverPalette.size
        val nextIndex = (baseIndex + 1) % DeckCoverPalette.size
        listOf(DeckCoverPalette[baseIndex], DeckCoverPalette[nextIndex])
    }
    return remember(colors) { Brush.linearGradient(colors) }
}

@Composable
private fun EmptyDecksState(onImportClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.no_decks_installed), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.no_decks_call_to_action), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onImportClick) { Text(stringResource(R.string.import_decks_action)) }
        }
    }
}

private data class DeckFiltersSheetState(
    val difficulty: DifficultyFilterState,
    val categories: FilterSelectionState,
    val wordClasses: FilterSelectionState,
)

private data class DifficultyFilterState(
    val minDifficulty: String,
    val maxDifficulty: String,
)

private data class FilterSelectionState(
    val available: List<String>,
    val selected: Set<String>,
)

private class DeckFiltersSheetCallbacks(
    val onMinDifficultyChange: (String) -> Unit,
    val onMaxDifficultyChange: (String) -> Unit,
    val onCategoriesChange: (Set<String>) -> Unit,
    val onWordClassesChange: (Set<String>) -> Unit,
    val onApply: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckFiltersSheet(
    state: DeckFiltersSheetState,
    callbacks: DeckFiltersSheetCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.filters_label), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.deck_filters_description), style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.difficulty.minDifficulty,
                onValueChange = callbacks.onMinDifficultyChange,
                label = { Text(stringResource(R.string.min_difficulty_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.difficulty.maxDifficulty,
                onValueChange = callbacks.onMaxDifficultyChange,
                label = { Text(stringResource(R.string.max_difficulty_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        FilterChipGroup(
            title = stringResource(R.string.categories_label),
            items = state.categories.available,
            selectedItems = state.categories.selected,
            onSelectionChanged = callbacks.onCategoriesChange,
        )
        FilterChipGroup(
            title = stringResource(R.string.word_classes_label),
            items = state.wordClasses.available,
            selectedItems = state.wordClasses.selected,
            onSelectionChanged = callbacks.onWordClassesChange,
        )
        Text(stringResource(R.string.filters_hint), style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = callbacks.onApply,
            modifier = Modifier.align(Alignment.End)
        ) { Text(stringResource(R.string.apply_label)) }
    }
}

private data class DeckImportSheetState(
    val url: String,
    val sha256: String,
)

private class DeckImportSheetCallbacks(
    val onUrlChange: (String) -> Unit,
    val onShaChange: (String) -> Unit,
    val onPickFile: () -> Unit,
    val onDownload: () -> Unit,
    val onOpenTrusted: () -> Unit,
)

@Composable
private fun DeckImportSheet(
    state: DeckImportSheetState,
    callbacks: DeckImportSheetCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.import_sheet_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.import_sheet_hint), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = callbacks.onPickFile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_file))
        }
        OutlinedTextField(
            value = state.url,
            onValueChange = callbacks.onUrlChange,
            label = { Text(stringResource(R.string.https_url)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.sha256,
            onValueChange = callbacks.onShaChange,
            label = { Text(stringResource(R.string.expected_sha256_optional)) },
            modifier = Modifier.fillMaxWidth()
        )
        FilledTonalButton(
            onClick = callbacks.onDownload,
            modifier = Modifier.align(Alignment.End),
            enabled = state.url.isNotBlank()
        ) { Text(stringResource(R.string.download_and_import)) }
        TextButton(onClick = callbacks.onOpenTrusted, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.Verified, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.manage_trusted_sources))
        }
    }
}

@Composable
private fun DeckTrustedSourcesSheet(
    trustedSources: List<String>,
    newSource: String,
    onNewSourceChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.trusted_sources), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.trusted_sources_sheet_hint), style = MaterialTheme.typography.bodyMedium)
        if (trustedSources.isEmpty()) {
            Text(stringResource(R.string.no_trusted_sources_yet))
        } else {
            trustedSources.forEachIndexed { index, entry ->
                ListItem(
                    headlineContent = { Text(entry) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                    }
                )
                if (index < trustedSources.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newSource,
                onValueChange = onNewSourceChange,
                label = { Text(stringResource(R.string.add_host_origin)) },
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onAdd, enabled = newSource.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun DeckDownloadProgressIndicator(
    progress: MainViewModel.DeckDownloadProgress,
    modifier: Modifier = Modifier,
) {
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

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(statusText, style = MaterialTheme.typography.bodyMedium)
        val indicatorModifier = Modifier.fillMaxWidth()
        if (fraction != null && progress.step == MainViewModel.DeckDownloadStep.DOWNLOADING) {
            LinearProgressIndicator(progress = { fraction }, modifier = indicatorModifier)
        } else {
            LinearProgressIndicator(modifier = indicatorModifier)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    title: String,
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckDetailScreen(vm: MainViewModel, deck: DeckEntity) {
    var count by remember { mutableStateOf<Int?>(null) }
    var categories by remember { mutableStateOf<List<String>?>(null) }
    var histogram by remember { mutableStateOf<List<DifficultyBucket>>(emptyList()) }
    var histogramLoading by remember { mutableStateOf(true) }
    var recentWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentWordsLoading by remember { mutableStateOf(true) }
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
        launch {
            histogramLoading = true
            try {
                histogram = runCatching { vm.getDeckDifficultyHistogram(deck.id) }.getOrElse { emptyList() }
            } finally {
                histogramLoading = false
            }
        }
        launch {
            recentWordsLoading = true
            try {
                recentWords = runCatching { vm.getDeckRecentWords(deck.id) }.getOrElse { emptyList() }
            } finally {
                recentWordsLoading = false
            }
        }
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
        DeckDetailHero(deck = deck, count = count, downloadDateText = downloadDateText)

        ElevatedCard(Modifier.fillMaxWidth()) {
            val countText = count?.toString() ?: "…"
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.deck_word_count, countText), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.deck_version_label, deck.version))
                Text(
                    downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                        ?: stringResource(R.string.deck_downloaded_unknown)
                )
            }
        }

        DetailCard(title = stringResource(R.string.deck_categories_title)) {
            when (val currentCategories = categories) {
                null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                else -> {
                    if (currentCategories.isEmpty()) {
                        Text(stringResource(R.string.deck_categories_empty))
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategories.forEach { category ->
                                AssistChip(onClick = {}, enabled = false, label = { Text(category) })
                            }
                        }
                    }
                }
            }
        }

        DetailCard(title = stringResource(R.string.deck_difficulty_title)) {
            if (histogramLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                DeckDifficultyHistogram(buckets = histogram)
            }
        }

        DetailCard(title = stringResource(R.string.deck_recent_words_title)) {
            when {
                recentWordsLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                recentWords.isEmpty() -> {
                    Text(stringResource(R.string.deck_recent_words_empty))
                }

                else -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentWords.forEach { word ->
                            AssistChip(onClick = {}, enabled = false, label = { Text(word) })
                        }
                    }
                }
            }
        }

        DetailCard(title = stringResource(R.string.deck_examples_title)) {
            when {
                examplesLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                examplesError -> {
                    Text(stringResource(R.string.deck_examples_error), color = MaterialTheme.colorScheme.error)
                }

                wordExamples.isEmpty() -> {
                    Text(stringResource(R.string.deck_examples_empty))
                }

                else -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wordExamples.forEach { example ->
                            AssistChip(onClick = {}, enabled = false, label = { Text(example) })
                        }
                    }
                }
            }
            TextButton(
                onClick = { scope.launch { refreshExamples() } },
                enabled = !examplesLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.deck_examples_reload))
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckDetailHero(deck: DeckEntity, count: Int?, downloadDateText: String?) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val countText = count?.toString() ?: "…"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(deck.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeckTag(deck.language.uppercase(Locale.getDefault()))
                if (deck.isOfficial) {
                    DeckTag(stringResource(R.string.deck_official_label))
                }
                if (deck.isNSFW) {
                    DeckTag(stringResource(R.string.deck_nsfw_label))
                }
            }
            Text(
                text = stringResource(R.string.deck_word_count, countText),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                    ?: stringResource(R.string.deck_downloaded_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun DeckTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeckDifficultyHistogram(
    buckets: List<DifficultyBucket>,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) {
        Text(stringResource(R.string.deck_difficulty_empty), modifier = modifier)
        return
    }

    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buckets.forEach { bucket ->
            val fraction = bucket.count.toFloat() / maxCount.toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.word_difficulty_value, bucket.difficulty))
                    Text(bucket.count.toString(), style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
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

private data class TeamEditorEntry(val id: Long, val name: String)

private val TeamEditorEntryStateSaver = listSaver<MutableState<List<TeamEditorEntry>>, Any?>(
    save = { state -> state.value.flatMap { entry -> listOf(entry.id, entry.name) } },
    restore = { restored ->
        val entries = restored.chunked(2).mapNotNull { chunk ->
            val id = (chunk.getOrNull(0) as? Number)?.toLong() ?: return@mapNotNull null
            val name = chunk.getOrNull(1) as? String ?: ""
            TeamEditorEntry(id, name)
        }
        mutableStateOf(entries)
    }
)

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
    var teams by rememberSaveable(s, saver = TeamEditorEntryStateSaver) {
        mutableStateOf(s.teams.mapIndexed { index, name -> TeamEditorEntry(index.toLong(), name) })
    }
    var nextTeamId by rememberSaveable(s) { mutableStateOf(s.teams.size.toLong()) }
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.MATCH_RULES) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val teamSuggestions = stringArrayResource(R.array.team_name_suggestions).toList()

    val canSave = teams.count { it.name.isNotBlank() } >= MIN_TEAMS
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
                teams = teams.map { it.name },
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
                        onOrientationChange = { orientation = it }
                    )

                    SettingsTab.TEAMS -> TeamsTab(
                        teams = teams,
                        canRemoveTeam = teams.size > MIN_TEAMS,
                        canAddTeam = teams.size < MAX_TEAMS,
                        onTeamNameChange = { index, value ->
                            teams = teams.toMutableList().also { list ->
                                list[index] = list[index].copy(name = value)
                            }
                        },
                        onTeamRemove = { index ->
                            teams = teams.toMutableList().also { list -> list.removeAt(index) }
                        },
                        onTeamAdd = {
                            val defaultName = ctx.getString(R.string.team_default_name, teams.size + 1)
                            teams = teams + TeamEditorEntry(nextTeamId, defaultName)
                            nextTeamId += 1
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
                            if (teams.any { it.name.equals(suggestion, ignoreCase = true) }) {
                                return@TeamsTab
                            }
                            val targetIndex = teams.indexOfFirst { it.name.isBlank() }
                            teams = teams.toMutableList().also { list ->
                                when {
                                    targetIndex >= 0 -> list[targetIndex] = list[targetIndex].copy(name = suggestion)
                                    list.size < MAX_TEAMS -> {
                                        list += TeamEditorEntry(nextTeamId, suggestion)
                                        nextTeamId += 1
                                    }
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
    teams: List<TeamEditorEntry>,
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
        itemsIndexed(teams, key = { _, team -> team.id }) { index, team ->
            val isDragging = draggingIndex == index
            TeamEditorCard(
                index = index,
                name = team.name,
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
                                val height = itemHeights[current + 1] ?: return@detectDragGesturesAfterLongPress
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
