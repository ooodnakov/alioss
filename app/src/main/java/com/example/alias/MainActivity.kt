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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import android.os.VibrationEffect
import com.example.alias.data.settings.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.alias.domain.GameEngine
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alias.ui.AppScaffold
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import com.example.alias.ui.WordCard
import com.example.alias.ui.WordCardAction
import com.example.alias.data.settings.SettingsRepository
private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                val snack = remember { SnackbarHostState() }

                // Collect general UI events and show snackbars
                LaunchedEffect(Unit) {
                    vm.uiEvents.collect { ev: UiEvent ->
                        // Always show, but enforce a 1s auto-dismiss for non-indefinite events.
                        val showJob = launch {
                            val result = snack.showSnackbar(
                                message = ev.message,
                                actionLabel = ev.actionLabel,
                                withDismissAction = ev.actionLabel == null,
                                duration = if (ev.duration == SnackbarDuration.Indefinite) SnackbarDuration.Indefinite else SnackbarDuration.Indefinite
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
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        AppScaffold(title = stringResource(R.string.app_name), snackbarHostState = snack) {
                            HomeScreen(
                                onQuickPlay = { vm.restartMatch(); nav.navigate("game") },
                                onDecks = { nav.navigate("decks") },
                                onSettings = { nav.navigate("settings") }
                            )
                        }
                    }
                    composable("game") {
                        val engine by vm.engine.collectAsState()
                        val settings by vm.settings.collectAsState()
                        AppScaffold(title = stringResource(R.string.title_game), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            if (engine == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.loading))
                                }
                            } else {
                                GameScreen(vm, engine!!, settings)
                            }
                        }
                    }
                    composable("decks") {
                        AppScaffold(title = stringResource(R.string.title_decks), onBack = { nav.popBackStack() }, snackbarHostState = snack) {
                            DecksScreen(vm = vm)
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
private fun HomeScreen(onQuickPlay: () -> Unit, onDecks: () -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title / branding
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = colors.primary
        )
        Spacer(Modifier.height(8.dp))
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
            icon = Icons.Filled.LibraryBooks,
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
    val state by engine.state.collectAsState()
    when (val s = state) {
        GameState.Idle -> Text(stringResource(R.string.idle))
        is GameState.TurnActive -> {
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
            LaunchedEffect(s.word) {
                // Word advanced: re-enable actions and unfreeze preview
                isProcessing = false
                committing = false
                frozenNext = null
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = { progress }, color = barColor, modifier = Modifier.fillMaxWidth())
                Text("${s.timeRemaining}s", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.team_label, s.team), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.remaining_label, s.remaining)) })
                      AssistChip(
                          onClick = {},
                          enabled = false,
                          label = {
                              Text(
                                  LocalContext.current.resources.getQuantityString(
                                      R.plurals.score_label,
                                      s.score,
                                      s.score
                                  )
                              )
                          }
                      )
                      AssistChip(
                          onClick = {},
                          enabled = false,
                          label = {
                              Text(
                                  LocalContext.current.resources.getQuantityString(
                                      R.plurals.skips_label,
                                      s.skipsRemaining,
                                      s.skipsRemaining
                                  )
                              )
                          }
                      )
                }
                        val computedNext = engine.peekNextWord()
                        val nextWord = frozenNext ?: computedNext
                        Box(Modifier.fillMaxWidth().height(200.dp)) {
                            // Pre-render the next card underneath to avoid flicker when advancing
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
                                    soundEnabled = false,
                                    onActionStart = {},
                                    onAction = {},
                                    animateAppear = false,
                                    allowSkip = s.skipsRemaining > 0,
                                )
                            }
                            WordCard(
                                word = s.word,
                                modifier = Modifier.matchParentSize().zIndex(1f),
                                enabled = true,
                                vibrator = vibrator,
                                hapticsEnabled = settings.hapticsEnabled,
                                soundEnabled = settings.soundEnabled,
                                onActionStart = {
                                    // Freeze the preview so it doesn't swap to next-next mid-animation
                                    if (!committing) {
                                        frozenNext = computedNext
                                        committing = true
                                    }
                                    isProcessing = true
                                },
                                onAction = {
                                    when (it) {
                                        WordCardAction.Correct -> {
                                            engine.correct()
                                            isProcessing = false
                                        }
                                        WordCardAction.Skip -> if (s.skipsRemaining > 0) {
                                            engine.skip()
                                            isProcessing = false
                                        } else {
                                            // No skips left: ignore gesture and keep card
                                            isProcessing = false
                                        }
                                    }
                                },
                                allowSkip = s.skipsRemaining > 0,
                                animateAppear = false,
                            )
                        }
                Text(stringResource(R.string.summary_label, s.remaining, s.score, s.skipsRemaining))
                if (settings.oneHandedLayout) {
                    val onCorrect = {
                        if (!isProcessing) {
                            isProcessing = true
                            engine.correct()
                        }
                    }
                    val onSkip = {
                        if (!isProcessing) {
                            if (s.skipsRemaining > 0) {
                                isProcessing = true
                                engine.skip()
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
                            engine.correct()
                        }
                    }
                    val onSkip = {
                        if (!isProcessing) {
                            if (s.skipsRemaining > 0) {
                                isProcessing = true
                                engine.skip()
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
private fun DecksScreen(vm: MainViewModel) {
    val decks by vm.decks.collectAsState()
    val enabled by vm.enabledDeckIds.collectAsState()
    val trusted by vm.trustedSources.collectAsState()
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Installed Decks", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { vm.setAllDecksEnabled(true) }) { Text("Enable all") }
                            TextButton(onClick = { vm.setAllDecksEnabled(false) }) { Text("Disable all") }
                        }
                    }
                    if (decks.isEmpty()) {
                        Text("No decks installed")
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
                                }
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
                    Text("Import / Download", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { filePicker.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) { Text("Import file") }
                        OutlinedButton(onClick = {
                            runCatching {
                                val host = java.net.URI(url).host ?: ""
                                if (host.isNotBlank()) vm.addTrustedSource(host)
                            }
                        }) { Text("Trust host") }
                    }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("HTTPS URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sha, onValueChange = { sha = it }, label = { Text("Expected SHA-256 (optional)") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { vm.downloadPackFromUrl(url, sha) }) { Text("Download & import") }
                    }
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trusted sources", style = MaterialTheme.typography.titleMedium)
                    if (trusted.isEmpty()) {
                        Text("No trusted sources yet")
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
                        OutlinedTextField(value = newTrusted, onValueChange = { newTrusted = it }, label = { Text("Add host/origin") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { if (newTrusted.isNotBlank()) { vm.addTrustedSource(newTrusted.trim()); newTrusted = "" } }) { Text("Add") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val s by vm.settings.collectAsState()
    var round by rememberSaveable(s) { mutableStateOf(s.roundSeconds.toString()) }
    var target by rememberSaveable(s) { mutableStateOf(s.targetWords.toString()) }
    var maxSkips by rememberSaveable(s) { mutableStateOf(s.maxSkips.toString()) }
    var penalty by rememberSaveable(s) { mutableStateOf(s.penaltyPerSkip.toString()) }
    var lang by rememberSaveable(s) { mutableStateOf(s.languagePreference) }
    var punishSkips by rememberSaveable(s) { mutableStateOf(s.punishSkips) }
    var nsfw by rememberSaveable(s) { mutableStateOf(s.allowNSFW) }
    var sound by rememberSaveable(s) { mutableStateOf(s.soundEnabled) }
    var haptics by rememberSaveable(s) { mutableStateOf(s.hapticsEnabled) }
    var oneHand by rememberSaveable(s) { mutableStateOf(s.oneHandedLayout) }
    var orientation by rememberSaveable(s) { mutableStateOf(s.orientation) }
    val scope = rememberCoroutineScope()
    var teams by rememberSaveable(s) { mutableStateOf(s.teams) }


    val canSave = teams.count { it.isNotBlank() } >= MIN_TEAMS
    val applySettings = {
        scope.launch {
            vm.updateSettings(
                roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                targetWords = target.toIntOrNull() ?: s.targetWords,
                maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                punishSkips = punishSkips,
                language = lang.ifBlank { s.languagePreference },
                allowNSFW = nsfw,
                sound = sound,
                haptics = haptics,
                oneHanded = oneHand,
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
                    Text("Round & Goals", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = round, onValueChange = { round = it }, label = { Text("Round seconds") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target words") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Skips", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = maxSkips, onValueChange = { maxSkips = it }, label = { Text("Max skips") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = penalty, onValueChange = { penalty = it }, label = { Text("Penalty/skip") }, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Punish skips", modifier = Modifier.weight(1f))
                        Switch(checked = punishSkips, onCheckedChange = { punishSkips = it })
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Language & Content", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = lang, onValueChange = { lang = it }, label = { Text("Language (e.g., en, ru)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow NSFW", modifier = Modifier.weight(1f))
                        Switch(checked = nsfw, onCheckedChange = { nsfw = it })
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Feedback & Layout", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sound effects", modifier = Modifier.weight(1f))
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Haptics", modifier = Modifier.weight(1f))
                        Switch(checked = haptics, onCheckedChange = { haptics = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("One-hand layout", modifier = Modifier.weight(1f))
                        Switch(checked = oneHand, onCheckedChange = { oneHand = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Orientation", modifier = Modifier.weight(1f))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text(orientation) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("system", "portrait", "landscape").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { orientation = it; expanded = false })
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Teams", style = MaterialTheme.typography.titleMedium)
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
                        OutlinedButton(onClick = { teams = teams + "Team ${teams.size + 1}" }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("Add Team", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { applySettings() }, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save") }
                FilledTonalButton(onClick = {
                    applySettings()
                    vm.restartMatch()
                    onBack()
                }, enabled = canSave, modifier = Modifier.weight(1f)) { Text("Save & Restart") }
            }
        }
        item { OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.title_about)) } }
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
                            Text("Alias", style = MaterialTheme.typography.headlineSmall)
                            Text("Version $version", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Links", style = MaterialTheme.typography.titleMedium)
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                        headlineContent = { Text("Source code") },
                        supportingContent = { Text("github.com/ooodnakov/alias-game") },
                        trailingContent = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/ooodnakov/alias-game") }
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                        headlineContent = { Text("Report an issue") },
                        supportingContent = { Text("Open GitHub issues") },
                        trailingContent = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/ooodnakov/alias-game/issues") }
                    )
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.title_about), style = MaterialTheme.typography.titleMedium)
                    Text("Author: Aleksandr Odnakov", style = MaterialTheme.typography.bodyMedium)
                    Text("No telemetry, no ads, all offline.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
                            tint = if (o.correct) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
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
            val suffix = if (leaders.size > 1 && isLeader) " (tie)" else if (isLeader) " ←" else ""
            Text("$team: $score$suffix")
        }
    }
}
