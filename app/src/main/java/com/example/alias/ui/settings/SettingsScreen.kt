package com.example.alias.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.SettingsUpdateRequest
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private const val MIN_TEAMS = SettingsRepository.MIN_TEAMS
private const val MAX_TEAMS = SettingsRepository.MAX_TEAMS

@Composable
fun settingsScreen(vm: MainViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var round by rememberSaveable(s) { mutableStateOf(s.roundSeconds.toString()) }
    var target by rememberSaveable(s) { mutableStateOf(s.targetWords.toString()) }
    var scoreTarget by rememberSaveable(s) { mutableStateOf(s.targetScore.toString()) }
    var useScoreTarget by rememberSaveable(s) { mutableStateOf(s.scoreTargetEnabled) }
    var maxSkips by rememberSaveable(s) { mutableStateOf(s.maxSkips.toString()) }
    var penalty by rememberSaveable(s) { mutableStateOf(s.penaltyPerSkip.toString()) }
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
    val pagerState = rememberPagerState { SettingsTab.values().size }
    var selectedTab by rememberSaveable(pagerState) { mutableStateOf(SettingsTab.MATCH_RULES) }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = SettingsTab.values()[pagerState.currentPage]
    }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val teamSuggestions = stringArrayResource(R.array.team_name_suggestions).toList()

    val canSave = teams.count { it.name.isNotBlank() } >= MIN_TEAMS
    val matchRulesUiState = MatchRulesUiState(
        round = round,
        target = target,
        scoreTarget = scoreTarget,
        scoreTargetEnabled = useScoreTarget,
        maxSkips = maxSkips,
        penalty = penalty,
        punishSkips = punishSkips,
    )
    val matchRulesActions = MatchRulesActions(
        onRoundChange = { round = it },
        onTargetChange = { target = it },
        onScoreTargetChange = { scoreTarget = it },
        onScoreTargetEnabledChange = { useScoreTarget = it },
        onMaxSkipsChange = { maxSkips = it },
        onPenaltyChange = { penalty = it },
        onPunishSkipsChange = { punishSkips = it },
    )
    val inputFeedbackUiState = InputFeedbackUiState(
        haptics = haptics,
        sound = sound,
        oneHand = oneHand,
        verticalSwipes = verticalSwipes,
        orientation = orientation,
    )
    val inputFeedbackActions = InputFeedbackActions(
        onHapticsChange = { haptics = it },
        onSoundChange = { sound = it },
        onOneHandChange = { oneHand = it },
        onVerticalSwipesChange = { verticalSwipes = it },
        onOrientationChange = { orientation = it },
    )
    val teamsUiState = TeamsUiState(
        teams = teams,
        canRemoveTeam = teams.size > MIN_TEAMS,
        canAddTeam = teams.size < MAX_TEAMS,
        suggestions = teamSuggestions,
    )
    val teamsActions = TeamsActions(
        onTeamNameChange = { index, value ->
            teams = teams.mapIndexed { i, team ->
                if (i == index) team.copy(name = value) else team
            }
        },
        onTeamRemove = { index ->
            teams = teams.toMutableList().apply { removeAt(index) }
        },
        onTeamAdd = {
            val defaultName = ctx.getString(R.string.team_default_name, teams.size + 1)
            teams = teams + TeamEditorEntry(nextTeamId, defaultName)
            nextTeamId += 1
        },
        onTeamMove = move@{ from, to ->
            if (from == to) return@move
            if (from !in teams.indices || to !in 0..teams.size) return@move
            val updated = teams.toMutableList()
            val item = updated.removeAt(from)
            val targetIndex = to.coerceIn(0, updated.size)
            updated.add(targetIndex, item)
            teams = updated
        },
        onApplySuggestion = suggestion@{ suggestion ->
            if (teams.any { it.name.equals(suggestion, ignoreCase = true) }) return@suggestion
            val targetIndex = teams.indexOfFirst { it.name.isBlank() }
            teams = teams.toMutableList().apply {
                when {
                    targetIndex >= 0 -> this[targetIndex] = this[targetIndex].copy(name = suggestion)
                    size < MAX_TEAMS -> {
                        add(TeamEditorEntry(nextTeamId, suggestion))
                        nextTeamId += 1
                    }
                }
            }
        },
    )
    val advancedUiState = AdvancedUiState(
        uiLanguage = uiLang,
        allowNsfw = nsfw,
    )
    val advancedActions = AdvancedActions(
        onUiLanguageChange = { uiLang = it },
        onAllowNsfwChange = { nsfw = it },
        onShowTutorialAgain = { vm.updateSeenTutorial(false) },
        onAbout = onAbout,
        onReset = { showResetDialog = true },
    )
    val applySettings: () -> Job = {
        scope.launch {
            vm.updateSettings(
                SettingsUpdateRequest(
                    roundSeconds = round.toIntOrNull() ?: s.roundSeconds,
                    targetWords = target.toIntOrNull() ?: s.targetWords,
                    targetScore = scoreTarget.toIntOrNull() ?: s.targetScore,
                    scoreTargetEnabled = useScoreTarget,
                    maxSkips = maxSkips.toIntOrNull() ?: s.maxSkips,
                    penaltyPerSkip = penalty.toIntOrNull() ?: s.penaltyPerSkip,
                    punishSkips = punishSkips,
                    uiLanguage = uiLang,
                    allowNSFW = nsfw,
                    haptics = haptics,
                    sound = sound,
                    oneHanded = oneHand,
                    verticalSwipes = verticalSwipes,
                    orientation = orientation,
                    teams = teams.map { it.name },
                ),
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.headlineSmall)
        TabRow(selectedTabIndex = pagerState.currentPage) {
            SettingsTab.values().forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = {
                        selectedTab = tab
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(stringResource(tab.titleRes)) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (SettingsTab.values()[page]) {
                    SettingsTab.MATCH_RULES -> matchRulesTab(
                        state = matchRulesUiState,
                        actions = matchRulesActions,
                    )

                    SettingsTab.INPUT_FEEDBACK -> inputFeedbackTab(
                        state = inputFeedbackUiState,
                        actions = inputFeedbackActions,
                    )

                    SettingsTab.TEAMS -> teamsTab(
                        state = teamsUiState,
                        actions = teamsActions,
                    )

                    SettingsTab.ADVANCED -> advancedTab(
                        state = advancedUiState,
                        actions = advancedActions,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.save_and_restart_label))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

private enum class SettingsTab(
    @StringRes val titleRes: Int,
) {
    MATCH_RULES(R.string.match_rules_tab),
    INPUT_FEEDBACK(R.string.input_feedback_tab),
    TEAMS(R.string.teams_tab),
    ADVANCED(R.string.advanced_tab),
}

private data class TeamEditorEntry(val id: Long, val name: String)

private data class MatchRulesUiState(
    val round: String,
    val target: String,
    val scoreTarget: String,
    val scoreTargetEnabled: Boolean,
    val maxSkips: String,
    val penalty: String,
    val punishSkips: Boolean,
)

private data class MatchRulesActions(
    val onRoundChange: (String) -> Unit,
    val onTargetChange: (String) -> Unit,
    val onScoreTargetChange: (String) -> Unit,
    val onScoreTargetEnabledChange: (Boolean) -> Unit,
    val onMaxSkipsChange: (String) -> Unit,
    val onPenaltyChange: (String) -> Unit,
    val onPunishSkipsChange: (Boolean) -> Unit,
)

private data class InputFeedbackUiState(
    val haptics: Boolean,
    val sound: Boolean,
    val oneHand: Boolean,
    val verticalSwipes: Boolean,
    val orientation: String,
)

private data class InputFeedbackActions(
    val onHapticsChange: (Boolean) -> Unit,
    val onSoundChange: (Boolean) -> Unit,
    val onOneHandChange: (Boolean) -> Unit,
    val onVerticalSwipesChange: (Boolean) -> Unit,
    val onOrientationChange: (String) -> Unit,
)

private data class TeamsUiState(
    val teams: List<TeamEditorEntry>,
    val canRemoveTeam: Boolean,
    val canAddTeam: Boolean,
    val suggestions: List<String>,
)

private data class TeamsActions(
    val onTeamNameChange: (Int, String) -> Unit,
    val onTeamRemove: (Int) -> Unit,
    val onTeamAdd: () -> Unit,
    val onTeamMove: (Int, Int) -> Unit,
    val onApplySuggestion: (String) -> Unit,
)

private data class AdvancedUiState(
    val uiLanguage: String,
    val allowNsfw: Boolean,
)

private data class AdvancedActions(
    val onUiLanguageChange: (String) -> Unit,
    val onAllowNsfwChange: (Boolean) -> Unit,
    val onShowTutorialAgain: () -> Unit,
    val onAbout: () -> Unit,
    val onReset: () -> Unit,
)

private val TeamEditorEntryStateSaver = listSaver<MutableState<List<TeamEditorEntry>>, Any?>(
    save = { state -> state.value.flatMap { entry -> listOf(entry.id, entry.name) } },
    restore = { restored ->
        val entries = restored.chunked(2).mapNotNull { chunk ->
            val id = (chunk.getOrNull(0) as? Number)?.toLong() ?: return@mapNotNull null
            val name = chunk.getOrNull(1) as? String ?: ""
            TeamEditorEntry(id, name)
        }
        mutableStateOf(entries)
    },
)

@Composable
private fun matchRulesTab(
    state: MatchRulesUiState,
    actions: MatchRulesActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.round_and_goals), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.round,
                        onValueChange = actions.onRoundChange,
                        label = { Text(stringResource(R.string.round_seconds_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = state.target,
                        onValueChange = actions.onTargetChange,
                        label = { Text(stringResource(R.string.target_words_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.scoreTargetEnabled,
                        supportingText = if (state.scoreTargetEnabled) {
                            { Text(stringResource(R.string.target_words_disabled_supporting_text)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.score_target_toggle_label),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.scoreTargetEnabled,
                            onCheckedChange = actions.onScoreTargetEnabledChange,
                        )
                    }
                    OutlinedTextField(
                        value = state.scoreTarget,
                        onValueChange = actions.onScoreTargetChange,
                        label = { Text(stringResource(R.string.target_score_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = state.scoreTargetEnabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            value = state.maxSkips,
                            onValueChange = actions.onMaxSkipsChange,
                            label = { Text(stringResource(R.string.max_skips_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(
                            value = state.penalty,
                            onValueChange = actions.onPenaltyChange,
                            label = { Text(stringResource(R.string.penalty_per_skip_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.punish_skips_label), modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.punishSkips,
                            onCheckedChange = actions.onPunishSkipsChange,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun inputFeedbackTab(
    state: InputFeedbackUiState,
    actions: InputFeedbackActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.feedback_and_layout), style = MaterialTheme.typography.titleMedium)
                    settingsToggleRow(
                        label = stringResource(R.string.haptics_label),
                        checked = state.haptics,
                        onCheckedChange = actions.onHapticsChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.sound_effects_label),
                        checked = state.sound,
                        onCheckedChange = actions.onSoundChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.one_hand_layout_label),
                        checked = state.oneHand,
                        onCheckedChange = actions.onOneHandChange,
                    )
                    settingsToggleRow(
                        label = stringResource(R.string.vertical_swipes_label),
                        checked = state.verticalSwipes,
                        onCheckedChange = actions.onVerticalSwipesChange,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        orientationChip(
                            selected = state.orientation == "system",
                            label = stringResource(R.string.auto_label),
                            icon = Icons.Filled.ScreenRotation,
                            onClick = { actions.onOrientationChange("system") },
                        )
                        orientationChip(
                            selected = state.orientation == "portrait",
                            label = stringResource(R.string.portrait_label),
                            icon = Icons.Filled.ScreenLockPortrait,
                            onClick = { actions.onOrientationChange("portrait") },
                        )
                        orientationChip(
                            selected = state.orientation == "landscape",
                            label = stringResource(R.string.landscape_label),
                            icon = Icons.Filled.ScreenLockLandscape,
                            onClick = { actions.onOrientationChange("landscape") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun settingsToggleRow(
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
private fun orientationChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun teamsTab(
    state: TeamsUiState,
    actions: TeamsActions,
) {
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.teams_label), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.drag_to_reorder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(state.teams, key = { _, team -> team.id }) { index, team ->
            val isDragging = draggingIndex == index
            teamEditorCard(
                index = index,
                name = team.name,
                canRemove = state.canRemoveTeam,
                onNameChange = actions.onTeamNameChange,
                onRemove = actions.onTeamRemove,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> itemHeights[index] = coords.size.height }
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                handleModifier = Modifier.pointerInput(state.teams) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val current = draggingIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffset += dragAmount.y
                            if (dragOffset > 0 && current < state.teams.lastIndex) {
                                val height = itemHeights[current + 1] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset > height * 0.6f) {
                                    actions.onTeamMove(current, current + 1)
                                    draggingIndex = current + 1
                                    dragOffset = 0f
                                }
                            } else if (dragOffset < 0 && current > 0) {
                                val height = itemHeights[current - 1] ?: return@detectDragGesturesAfterLongPress
                                if (dragOffset < -height * 0.6f) {
                                    actions.onTeamMove(current, current - 1)
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
                        },
                    )
                },
                isDragging = isDragging,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = actions.onTeamAdd, enabled = state.canAddTeam, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_team_label))
                }
                OutlinedButton(
                    onClick = { state.suggestions.randomOrNull()?.let(actions.onApplySuggestion) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.team_suggestions_label))
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.team_suggestions_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.team_suggestions_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { actions.onApplySuggestion(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun teamEditorCard(
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: (index + 1).toString()
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                    singleLine = true,
                )
                IconButton(onClick = { onRemove(index) }, enabled = canRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_team))
                }
                Box(
                    modifier = handleModifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.team_drag_handle_description),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun advancedTab(
    state: AdvancedUiState,
    actions: AdvancedActions,
) {
    val selectedLanguage = remember(state.uiLanguage) { resolveUiLanguageSelection(state.uiLanguage) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.language_and_content), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ui_language_label))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedLanguage == "system",
                            onClick = { actions.onUiLanguageChange("system") },
                            label = { Text(stringResource(R.string.system_default_label)) },
                        )
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = { actions.onUiLanguageChange("en") },
                            label = { Text(stringResource(R.string.english_label)) },
                        )
                        FilterChip(
                            selected = selectedLanguage == "ru",
                            onClick = { actions.onUiLanguageChange("ru") },
                            label = { Text(stringResource(R.string.russian_label)) },
                        )
                    }
                    settingsToggleRow(
                        label = stringResource(R.string.allow_nsfw_label),
                        checked = state.allowNsfw,
                        onCheckedChange = actions.onAllowNsfwChange,
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
                        modifier = Modifier.clickable { actions.onShowTutorialAgain() },
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.title_about)) },
                        modifier = Modifier.clickable(onClick = actions.onAbout),
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.reset_local_data)) },
                        modifier = Modifier.clickable { actions.onReset() },
                    )
                }
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
