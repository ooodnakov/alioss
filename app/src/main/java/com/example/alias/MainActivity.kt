package com.example.alias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import android.os.VibrationEffect
import com.example.alias.data.settings.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.alias.ui.AppScaffold
import com.example.alias.ui.HistoryScreen
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import com.example.alias.MainViewModel.UiEvent
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.example.alias.ui.TutorialOverlay
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.data.db.DeckEntity
import androidx.compose.ui.platform.LocalUriHandler
import com.google.accompanist.placeholder.material3.placeholder
private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS
private const val HISTORY_LIMIT = 50



@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                val snack = remember { SnackbarHostState() }
                val settings by vm.settings.collectAsState()

                LaunchedEffect(settings.uiLanguage) {
                    val locales = when (val tag = settings.uiLanguage) {
                        "system" -> LocaleListCompat.getEmptyLocaleList()
                        else -> {
                            val parsed = LocaleListCompat.forLanguageTags(tag)
                            if (parsed.isEmpty) LocaleListCompat.getEmptyLocaleList() else parsed
                        }
                    }
                    val appLocales = AppCompatDelegate.getApplicationLocales()
                    if (appLocales != locales) {
                        AppCompatDelegate.setApplicationLocales(locales)
                        val newLocale = if (locales.isEmpty) {
                            LocaleListCompat.getAdjustedDefault().get(0)
                        } else {
                            locales.get(0)
                        }
                        newLocale?.let(Locale::setDefault)
                    }
                }

                // Collect general UI events and show snackbars
                LaunchedEffect(Unit) {
                    vm.uiEvents.collect { ev: UiEvent ->
                        // Always show, but enforce a 1s auto-dismiss for non-indefinite events.
                        val showJob = launch {
                            val result = snack.showSnackbar(
                                message = ev.message,
                                actionLabel = ev.actionLabel,
                                withDismissAction = ev.actionLabel == null,
                                duration = ev.duration
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                ev.onAction?.invoke()
                            }
                        }
                        if (ev.duration != SnackbarDuration.Indefinite) {
                            launch {
                                kotlinx.coroutines.delay(1000)
                                snack.currentSnackbarData?.dismiss()
                            }
                        }
                        showJob.join()
                    }
                }
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    enterTransition = { slideInHorizontally() },
                    exitTransition = { fadeOut() }
                ) {
                    composable("home") {
                        AppScaffold(title = stringResource(R.string.app_name), snackbarHostState = snack) {
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
                        AppScaffold(title = stringResource(R.string.title_game), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
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
                        AppScaffold(title = stringResource(R.string.title_decks), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            DecksScreen(vm = vm, onDeckSelected = { nav.navigate("deck/${'$'}{it.id}") })
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
                                title = "Deck",
                                onBack = { nav.popBackStack() },
                                snackbarHostState = snack
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.deck_not_found))
                                }
                            }
                        } else {
                            AppScaffold(
                                title = deck.name,
                                onBack = { nav.popBackStack() },
                                snackbarHostState = snack
                            ) {
                                DeckDetailScreen(vm = vm, deck = deck)
                            }
                        }
                    }
                    composable("settings") {
                        AppScaffold(title = stringResource(R.string.title_settings), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            SettingsScreen(
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onAbout = { nav.navigate("about") }
                            )
                        }
                    }
                    composable("history") {
                        AppScaffold(title = stringResource(R.string.title_history), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            val historyFlow = remember { vm.recentHistory(HISTORY_LIMIT) }
                            val history by historyFlow.collectAsState(initial = emptyList())
                            HistoryScreen(history)
                        }
                    }
                    composable("about") {
                        AppScaffold(title = stringResource(R.string.title_about), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(id = R.drawable.ic_launcher_foreground_asset), contentDescription = null, modifier = Modifier.size(48.dp))
                    Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = colors.primary)
                }
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
            Image(painterResource(id = R.drawable.ic_launcher_foreground_asset), contentDescription = null, modifier = Modifier.size(56.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = colors.primary
            )
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
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    ElevatedCard(
        onClick = onClick,
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.team_label, s.team))
                Button(onClick = { vm.startTurn() }) { Text(stringResource(R.string.start_turn)) }
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
            val CardStack: @Composable () -> Unit = {
                val nextWord = frozenNext ?: computedNext
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
                    )
                }
            }

            val infoMap by vm.wordInfoByText.collectAsState()
            val Controls: @Composable () -> Unit = {
                Text("${s.timeRemaining}s", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.team_label, s.team), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.remaining_label, s.remaining)) })
                    AssistChip(onClick = {}, enabled = false, label = { Text(pluralStringResource(R.plurals.score_label, s.score, s.score)) })
                    AssistChip(onClick = {}, enabled = false, label = { Text(pluralStringResource(R.plurals.skips_label, s.skipsRemaining, s.skipsRemaining)) })
                    val meta = infoMap[s.word]
                    if (meta != null) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("D${meta.difficulty}") })
                        meta.category?.takeIf { it.isNotBlank() }?.let { cat ->
                            AssistChip(onClick = {}, enabled = false, label = { Text(cat) })
                        }
                    }
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
                                if (!isProcessing) {
                                    isProcessing = true
                                    scope.launch { engine.correct() }
                                }
                            }
                            val onSkip = {
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
                            if (!isProcessing) {
                                isProcessing = true
                                scope.launch { engine.correct() }
                            }
                        }
                        val onSkip = {
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
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            ) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.correct)) }
                            Button(
                                onClick = onSkip,
                                enabled = !isProcessing && s.skipsRemaining > 0,
                                modifier = Modifier.fillMaxWidth().height(80.dp)
                            ) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.skip)) }
                        }
                    } else {
                        val onCorrect = {
                            if (!isProcessing) {
                                isProcessing = true
                                scope.launch { engine.correct() }
                            }
                        }
                        val onSkip = {
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
                                onClick = onCorrect,
                                enabled = !isProcessing,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.correct)) }
                            Button(
                                onClick = onSkip,
                                enabled = !isProcessing && s.skipsRemaining > 0,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) { Icon(Icons.Filled.Close, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.skip)) }
                        }
                    }
                    Button(onClick = { vm.restartMatch() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restart_match)) }
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
                Button(onClick = { vm.restartMatch() }) { Text(stringResource(R.string.restart_match)) }
        }
    }
}
}

@Composable
private fun DecksScreen(vm: MainViewModel, onDeckSelected: (DeckEntity) -> Unit) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
    val settings by vm.settings.collectAsState()
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
                    val available by vm.availableCategories.collectAsState()
                    var selectedCats by rememberSaveable(settings) { mutableStateOf(settings.selectedCategories) }
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
                        }) {
                            Text(stringResource(R.string.apply_label))
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(available) { cat ->
                            val selected = selectedCats.contains(cat)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedCats = selectedCats.toMutableSet().also {
                                        if (selected) it.remove(cat) else it.add(cat)
                                    }
                                },
                                label = { Text(cat) }
                            )
                        }
                    }
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
private fun DeckDetailScreen(vm: MainViewModel, deck: DeckEntity) {
    var count by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(deck.id) { count = vm.getWordCount(deck.id) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(deck.name, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, enabled = false, label = { Text(deck.language.uppercase()) })
            if (deck.isNSFW) AssistChip(onClick = {}, enabled = false, label = { Text("NSFW") })
        }
        val countText = count?.toString() ?: "…"
        Text(stringResource(R.string.deck_word_count, countText))
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
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
    val scope = rememberCoroutineScope()
    var teams by rememberSaveable(s) { mutableStateOf(s.teams) }


    val canSave = teams.count { it.isNotBlank() } >= MIN_TEAMS
    val applySettings: () -> kotlinx.coroutines.Job = {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.round_and_goals), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = round, onValueChange = { round = it }, label = { Text(stringResource(R.string.round_seconds_label)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(stringResource(R.string.target_words_label)) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.skips_section), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = maxSkips, onValueChange = { maxSkips = it }, label = { Text(stringResource(R.string.max_skips_label)) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = penalty, onValueChange = { penalty = it }, label = { Text(stringResource(R.string.penalty_per_skip_label)) }, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.punish_skips_label), modifier = Modifier.weight(1f))
                        Switch(checked = punishSkips, onCheckedChange = { punishSkips = it })
                    }
                }
            }
        }
        item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.language_and_content), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ui_language_label))
                    val selectedLanguage = remember(uiLang) { resolveUiLanguageSelection(uiLang) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedLanguage == "system",
                            onClick = { uiLang = "system" },
                            label = { Text(stringResource(R.string.system_default_label)) }
                        )
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = { uiLang = "en" },
                            label = { Text(stringResource(R.string.english_label)) }
                        )
                        FilterChip(
                            selected = selectedLanguage == "ru",
                            onClick = { uiLang = "ru" },
                            label = { Text(stringResource(R.string.russian_label)) }
                        )
                    }
                    OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text(stringResource(R.string.language_hint)) }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.allow_nsfw_label), modifier = Modifier.weight(1f))
                        Switch(checked = nsfw, onCheckedChange = { nsfw = it })
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feedback_and_layout), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.haptics_label), modifier = Modifier.weight(1f))
                        Switch(checked = haptics, onCheckedChange = { haptics = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.sound_effects_label), modifier = Modifier.weight(1f))
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.one_hand_layout_label), modifier = Modifier.weight(1f))
                        Switch(checked = oneHand, onCheckedChange = { oneHand = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.vertical_swipes_label), modifier = Modifier.weight(1f))
                        Switch(checked = verticalSwipes, onCheckedChange = { verticalSwipes = it })
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.orientation_label))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val current = orientation
                            FilterChip(
                                selected = current == "system",
                                onClick = { orientation = "system"; vm.setOrientation("system") },
                                label = { Text(stringResource(R.string.auto_label)) },
                                leadingIcon = { Icon(Icons.Filled.ScreenRotation, contentDescription = null) }
                            )
                            FilterChip(
                                selected = current == "portrait",
                                onClick = { orientation = "portrait"; vm.setOrientation("portrait") },
                                label = { Text(stringResource(R.string.portrait_label)) },
                                leadingIcon = { Icon(Icons.Filled.ScreenLockPortrait, contentDescription = null) }
                            )
                            FilterChip(
                                selected = current == "landscape",
                                onClick = { orientation = "landscape"; vm.setOrientation("landscape") },
                                label = { Text(stringResource(R.string.landscape_label)) },
                                leadingIcon = { Icon(Icons.Filled.ScreenLockLandscape, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.teams_label), style = MaterialTheme.typography.titleMedium)
                    teams.forEachIndexed { index, name ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { new -> teams = teams.toMutableList().also { it[index] = new } },
                                label = { Text("Team ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { teams = teams.toMutableList().also { it.removeAt(index) } },
                                enabled = teams.size > MIN_TEAMS
                            ) { Icon(Icons.Filled.Delete, contentDescription = "Remove team") }
                        }
                        if (index < teams.lastIndex) HorizontalDivider()
                    }
                    if (teams.size < MAX_TEAMS) {
                        OutlinedButton(onClick = { teams = teams + ctx.getString(R.string.team_default_name, teams.size + 1) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(stringResource(R.string.add_team_label), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { applySettings() }, enabled = canSave, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save_label)) }
                FilledTonalButton(onClick = {
                    scope.launch {
                        applySettings().join()
                        vm.restartMatch()
                        onBack()
                    }
                }, enabled = canSave, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save_and_restart_label)) }
            }
        }
        item {
            OutlinedButton(
                onClick = { vm.updateSeenTutorial(false) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.show_tutorial_again)) }
        }
        item { OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.title_about)) } }
        item {
            var confirm by rememberSaveable { mutableStateOf(false) }
            if (confirm) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { confirm = false }) {
                    ElevatedCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.reset_confirm_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.reset_confirm_message))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { confirm = false }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                                Button(onClick = {
                                    confirm = false
                                    vm.resetLocalData()
                                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.confirm)) }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_local_data)) }
        }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) } }
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
            val suffix = if (leaders.size > 1 && isLeader) stringResource(R.string.tie_suffix) else if (isLeader) " \u2190" else ""
            Text("$team: $score$suffix")
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
