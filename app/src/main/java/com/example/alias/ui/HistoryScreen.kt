package com.example.alias.ui

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.alias.R
import com.example.alias.data.db.TurnHistoryEntity

private const val SPARKLINE_RECENT_ENTRIES_COUNT = 12
private val SPARKLINE_DOT_RADIUS = 4.dp
private const val SPARKLINE_STROKE_WIDTH = 4f
private const val TURN_BREAK_THRESHOLD_MILLIS = 3 * 60_000L
private const val GAME_BREAK_THRESHOLD_MILLIS = 20 * 60_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun historyScreen(
    history: List<TurnHistoryEntity>,
    onResetHistory: () -> Unit,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.history_reset_dialog_title)) },
            text = { Text(stringResource(R.string.history_reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetHistory()
                    },
                ) {
                    Text(stringResource(R.string.history_reset_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.history_reset_dialog_cancel))
                }
            },
        )
    }

    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_history))
        }
        return
    }

    val chronologicalHistory = remember(history) { history.sortedBy { it.timestamp } }
    val teams = remember(history) { history.map { it.team }.distinct().sorted() }
    val difficulties = remember(history) { history.mapNotNull { it.difficulty }.distinct().sorted() }

    val filterState = rememberHistoryFilterState()
    val filterListener = rememberHistoryFilterListener(filterState)
    var headerExpanded by rememberSaveable { mutableStateOf(false) }

    val games = remember(chronologicalHistory) { groupHistoryIntoGames(chronologicalHistory) }
    val filteredGames = remember(
        games,
        filterState.selectedTeam,
        filterState.selectedDifficulty,
        filterState.selectedResult,
    ) {
        val selectedTeam = filterState.selectedTeam
        val selectedDifficulty = filterState.selectedDifficulty
        val selectedResult = filterState.selectedResult
        val predicate: (TurnHistoryEntity) -> Boolean = { entry ->
            val matchesTeam = selectedTeam == null || entry.team == selectedTeam
            val matchesDifficulty = selectedDifficulty == null || entry.difficulty == selectedDifficulty
            val matchesResult = when (selectedResult) {
                ResultFilter.All -> true
                ResultFilter.Correct -> entry.correct
                ResultFilter.Skipped -> !entry.correct && entry.skipped
                ResultFilter.Missed -> !entry.correct && !entry.skipped
            }
            matchesTeam && matchesDifficulty && matchesResult
        }
        games.mapNotNull { it.filter(predicate) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.title_history),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { headerExpanded = !headerExpanded }) {
                    val (labelRes, icon) = if (headerExpanded) {
                        R.string.history_hide_header to Icons.Filled.KeyboardArrowUp
                    } else {
                        R.string.history_show_header to Icons.Filled.KeyboardArrowDown
                    }
                    val toggleLabel = stringResource(labelRes)
                    Text(toggleLabel)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = toggleLabel,
                    )
                }
                TextButton(onClick = { showResetDialog = true }) {
                    Text(stringResource(R.string.history_reset_action))
                }
            }
        }
        AnimatedVisibility(visible = headerExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                historyFilters(
                    teams = teams,
                    difficulties = difficulties,
                    filterState = filterState,
                    listener = filterListener,
                )
                historyPerformanceSection(history = chronologicalHistory)
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.history_section_recent_games), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.history_recent_count, filteredGames.size, games.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.history_no_entries_for_filters),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    historyGameCard(game = game)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun historyFilters(
    teams: List<String>,
    difficulties: List<Int>,
    filterState: HistoryFilterState,
    listener: HistoryFilterListener,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_team),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filterState.selectedTeam == null,
                    onClick = { listener.onTeamSelected(null) },
                    label = { Text(stringResource(R.string.history_filter_all_teams)) },
                )
                teams.forEach { team ->
                    val selected = filterState.selectedTeam == team
                    FilterChip(
                        selected = selected,
                        onClick = { listener.onTeamSelected(if (selected) null else team) },
                        label = { Text(team) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_difficulty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filterState.selectedDifficulty == null,
                    onClick = { listener.onDifficultySelected(null) },
                    label = { Text(stringResource(R.string.history_filter_all_difficulties)) },
                )
                if (difficulties.isEmpty()) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        label = { Text(stringResource(R.string.history_filter_unknown_difficulty)) },
                    )
                } else {
                    difficulties.forEach { level ->
                        val selected = filterState.selectedDifficulty == level
                        FilterChip(
                            selected = selected,
                            onClick = { listener.onDifficultySelected(if (selected) null else level) },
                            label = { Text(stringResource(R.string.word_difficulty_value, level)) },
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_result),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultFilter.values().forEach { filter ->
                    FilterChip(
                        selected = filterState.selectedResult == filter,
                        onClick = { listener.onResultSelected(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun historyGameCard(game: HistoryGame) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var expanded by rememberSaveable(game.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "historyGameRotation")
    val stateLabel = if (expanded) {
        stringResource(R.string.timeline_section_state_expanded)
    } else {
        stringResource(R.string.timeline_section_state_collapsed)
    }
    val summary = stringResource(R.string.history_game_summary, game.turns.size, game.totalWords)
    val dateLabel = remember(game.startTimestamp, game.endTimestamp, context) {
        DateUtils.formatDateTime(
            context,
            game.startTimestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
        )
    }
    val relativeTime = remember(game.endTimestamp) {
        DateUtils.getRelativeTimeSpanString(
            game.endTimestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val teams = remember(game.turns) { game.turns.map { it.team }.distinct() }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateLabel },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (teams.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.history_game_teams_label, teams.joinToString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text(
                        relativeTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        game.turns.forEach { turn ->
                            historyTurnCard(turn = turn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun historyTurnCard(turn: HistoryTurn) {
    val colors = MaterialTheme.colorScheme
    var expanded by rememberSaveable(turn.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "historyTurnRotation")
    val stateLabel = if (expanded) {
        stringResource(R.string.timeline_section_state_expanded)
    } else {
        stringResource(R.string.timeline_section_state_collapsed)
    }
    val relativeTime = remember(turn.endTimestamp) {
        DateUtils.getRelativeTimeSpanString(
            turn.endTimestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val correctLabel = pluralStringResource(
        R.plurals.turn_summary_stat_correct,
        turn.correctCount,
        turn.correctCount,
    )
    val skippedLabel = pluralStringResource(
        R.plurals.turn_summary_stat_skipped,
        turn.skippedCount,
        turn.skippedCount,
    )
    val pendingLabel = pluralStringResource(
        R.plurals.turn_summary_stat_pending,
        turn.missedCount,
        turn.missedCount,
    )

    val summaryText = remember(correctLabel, skippedLabel, pendingLabel) {
        listOf(correctLabel, skippedLabel, pendingLabel).joinToString(" • ")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = stateLabel },
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (expanded) 2.dp else 0.dp,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(turn.team, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        turn.entries.forEach { entry ->
                            historyEntryCard(
                                entry = entry,
                                modifier = Modifier.fillMaxWidth(),
                                showTeam = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberHistoryFilterState(): HistoryFilterState {
    return rememberSaveable(saver = HistoryFilterState.Saver) {
        HistoryFilterState(
            initialSelectedTeam = null,
            initialSelectedDifficulty = null,
            initialSelectedResult = ResultFilter.All,
        )
    }
}

@Composable
private fun rememberHistoryFilterListener(state: HistoryFilterState): HistoryFilterListener {
    return remember(state) { DefaultHistoryFilterListener(state) }
}

@Composable
private fun historyPerformanceSection(history: List<TurnHistoryEntity>) {
    val grouped = remember(history) { history.groupBy { it.team } }
    if (grouped.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.history_section_performance), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(grouped.entries.sortedBy { it.key }) { (team, entries) ->
                historyPerformanceCard(team = team, entries = entries)
            }
        }
    }
}

@Composable
private fun historyPerformanceCard(team: String, entries: List<TurnHistoryEntity>) {
    val ordered = remember(entries) { entries.sortedBy { it.timestamp } }
    val recent = remember(ordered) { ordered.takeLast(SPARKLINE_RECENT_ENTRIES_COUNT) }
    val total = ordered.size
    val correct = ordered.count { it.correct }
    val percent = if (total > 0) ((correct.toFloat() / total) * 100).roundToInt() else 0
    val sparkValues = remember(recent) {
        val values = ArrayList<Float>(recent.size)
        var runningCorrect = 0
        recent.forEachIndexed { index, entry ->
            if (entry.correct) runningCorrect++
            values += runningCorrect.toFloat() / (index + 1)
        }
        values
    }

    ElevatedCard(modifier = Modifier.width(220.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(team, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.history_performance_summary, correct, total, percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sparkline(
                values = sparkValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )
        }
    }
}

@Composable
private fun sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val baseline = color.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f,
        )
        if (values.isEmpty()) {
            return@Canvas
        }
        val clamped = values.map { it.coerceIn(0f, 1f) }
        if (clamped.size == 1) {
            val y = size.height - (clamped.first() * size.height)
            drawCircle(color = color, radius = SPARKLINE_DOT_RADIUS.toPx(), center = Offset(size.width / 2f, y))
            return@Canvas
        }
        val stepX = if (clamped.size > 1) size.width / (clamped.size - 1) else size.width
        val path = Path()
        clamped.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - (value * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = SPARKLINE_STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun historyEntryCard(
    entry: TurnHistoryEntity,
    modifier: Modifier = Modifier,
    showTeam: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val visuals = when {
        entry.correct -> HistoryEntryVisuals(
            icon = Icons.Filled.Check,
            accent = colors.tertiary,
            labelRes = R.string.history_result_correct,
        )
        entry.skipped -> HistoryEntryVisuals(
            icon = Icons.Filled.Close,
            accent = colors.error,
            labelRes = R.string.history_result_skipped,
        )
        else -> HistoryEntryVisuals(
            icon = Icons.Filled.Close,
            accent = colors.outline,
            labelRes = R.string.history_result_missed,
        )
    }
    val relativeTime = remember(entry.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            entry.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    val chipColors = AssistChipDefaults.assistChipColors(
        disabledContainerColor = colors.surfaceVariant,
        disabledLabelColor = colors.onSurfaceVariant,
    )
    val containerColor = visuals.accent.copy(alpha = 0.12f)
    val borderColor = visuals.accent.copy(alpha = 0.35f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = colors.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = null,
                    tint = visuals.accent,
                    modifier = Modifier.size(24.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        entry.word,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(visuals.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = visuals.accent,
                    fontWeight = FontWeight.Medium,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
                maxLines = 1,
            ) {
                if (showTeam) {
                    disabledCompactAssistChip(
                        label = entry.team,
                        colors = chipColors,
                    )
                }
                val difficultyLabel = entry.difficulty?.let { stringResource(R.string.word_difficulty_value, it) }
                    ?: stringResource(R.string.history_filter_unknown_difficulty)
                disabledCompactAssistChip(
                    label = difficultyLabel,
                    colors = chipColors,
                )
            }
        }
    }
}

@Suppress("ExperimentalMaterial3Api")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun disabledCompactAssistChip(
    label: String,
    colors: ChipColors,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        AssistChip(
            onClick = {},
            enabled = false,
            modifier = modifier.height(28.dp),
            colors = colors,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )
    }
}

private data class HistoryGame(
    val id: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val turns: List<HistoryTurn>,
) {
    val totalWords: Int get() = turns.sumOf { it.entries.size }

    fun filter(predicate: (TurnHistoryEntity) -> Boolean): HistoryGame? {
        val filteredTurns = turns.mapNotNull { it.filter(predicate) }
        if (filteredTurns.isEmpty()) return null
        val start = filteredTurns.minOf { it.startTimestamp }
        val end = filteredTurns.maxOf { it.endTimestamp }
        return copy(startTimestamp = start, endTimestamp = end, turns = filteredTurns)
    }
}

private data class HistoryTurn(
    val id: Long,
    val team: String,
    val entries: List<TurnHistoryEntity>,
    val startTimestamp: Long,
    val endTimestamp: Long,
) {
    val totalWords: Int get() = entries.size
    val correctCount: Int get() = entries.count { it.correct }
    val skippedCount: Int get() = entries.count { it.skipped }
    val missedCount: Int get() = entries.count { !it.correct && !it.skipped }

    fun filter(predicate: (TurnHistoryEntity) -> Boolean): HistoryTurn? {
        val filtered = entries.filter(predicate)
        if (filtered.isEmpty()) return null
        val ordered = filtered.sortedBy { it.timestamp }
        return copy(
            entries = ordered,
            startTimestamp = ordered.first().timestamp,
            endTimestamp = ordered.last().timestamp,
        )
    }
}

private fun groupHistoryIntoGames(history: List<TurnHistoryEntity>): List<HistoryGame> {
    if (history.isEmpty()) return emptyList()

    // Group by matchId for entries that have it
    val (withMatchId, withoutMatchId) = history.partition { it.matchId != null }

    val games = mutableListOf<HistoryGame>()

    // Process entries with matchId: each matchId is a separate game
    withMatchId.groupBy { it.matchId!! }.forEach { (matchId, entries) ->
        val turns = groupEntriesIntoTurns(entries.sortedBy { it.timestamp })
        if (turns.isNotEmpty()) {
            games += buildGame(turns)
        }
    }

    // Process entries without matchId: use time-based grouping
    if (withoutMatchId.isNotEmpty()) {
        val turns = groupEntriesIntoTurns(withoutMatchId.sortedBy { it.timestamp })
        val timeBasedGames = groupTurnsIntoGames(turns)
        games += timeBasedGames
    }

    return games.sortedByDescending { it.endTimestamp }
}

private fun groupEntriesIntoTurns(history: List<TurnHistoryEntity>): List<HistoryTurn> {
    val turns = mutableListOf<HistoryTurn>()
    var currentEntries = mutableListOf<TurnHistoryEntity>()
    var lastEntry: TurnHistoryEntity? = null

    history.forEach { entry ->
        val last = lastEntry
        val shouldStartNewTurn = when {
            currentEntries.isEmpty() -> false
            entry.team != currentEntries.last().team -> true
            last != null && entry.timestamp - last.timestamp > TURN_BREAK_THRESHOLD_MILLIS -> true
            else -> false
        }
        if (shouldStartNewTurn) {
            turns += buildTurn(currentEntries.toList())
            currentEntries = mutableListOf()
        }
        currentEntries += entry
        lastEntry = entry
    }
    if (currentEntries.isNotEmpty()) {
        turns += buildTurn(currentEntries.toList())
    }

    return turns
}

private fun groupTurnsIntoGames(turns: List<HistoryTurn>): List<HistoryGame> {
    if (turns.isEmpty()) return emptyList()

    val games = mutableListOf<HistoryGame>()
    var currentTurns = mutableListOf<HistoryTurn>()
    var lastTurnEnd: Long? = null

    turns.forEach { turn ->
        val last = lastTurnEnd
        val shouldStartNewGame = if (currentTurns.isNotEmpty() && last != null) {
            turn.startTimestamp - last > GAME_BREAK_THRESHOLD_MILLIS
        } else {
            false
        }
        if (shouldStartNewGame) {
            games += buildGame(currentTurns.toList())
            currentTurns = mutableListOf()
        }
        currentTurns += turn
        lastTurnEnd = turn.endTimestamp
    }
    if (currentTurns.isNotEmpty()) {
        games += buildGame(currentTurns.toList())
    }

    return games
}

private fun buildTurn(entries: List<TurnHistoryEntity>): HistoryTurn {
    require(entries.isNotEmpty()) { "Cannot build turn with no entries" }
    val team = entries.first().team
    val turnId = entries.maxOfOrNull { it.id } ?: entries.hashCode().toLong()
    return HistoryTurn(
        id = turnId,
        team = team,
        entries = entries,
        startTimestamp = entries.first().timestamp,
        endTimestamp = entries.last().timestamp,
    )
}

private fun buildGame(turns: List<HistoryTurn>): HistoryGame {
    require(turns.isNotEmpty()) { "Cannot build game with no turns" }
    val gameId = turns.maxOfOrNull { it.id } ?: turns.hashCode().toLong()
    return HistoryGame(
        id = gameId,
        startTimestamp = turns.first().startTimestamp,
        endTimestamp = turns.last().endTimestamp,
        turns = turns,
    )
}

@Stable
private class HistoryFilterState(
    initialSelectedTeam: String?,
    initialSelectedDifficulty: Int?,
    initialSelectedResult: ResultFilter,
) {
    var selectedTeam by mutableStateOf(initialSelectedTeam)
        private set

    var selectedDifficulty by mutableStateOf(initialSelectedDifficulty)
        private set

    var selectedResult by mutableStateOf(initialSelectedResult)
        private set

    fun selectTeam(team: String?) {
        selectedTeam = team
    }

    fun selectDifficulty(difficulty: Int?) {
        selectedDifficulty = difficulty
    }

    fun selectResult(result: ResultFilter) {
        selectedResult = result
    }

    companion object {
        val Saver = listSaver<HistoryFilterState, Any?>(
            save = {
                listOf(
                    it.selectedTeam,
                    it.selectedDifficulty,
                    it.selectedResult.name,
                )
            },
            restore = {
                val savedTeam = it.getOrNull(0) as? String
                val savedDifficulty = it.getOrNull(1) as? Int
                val savedResult = (it.getOrNull(2) as? String)?.let { name ->
                    ResultFilter.entries.find { result -> result.name == name }
                } ?: ResultFilter.All
                HistoryFilterState(
                    initialSelectedTeam = savedTeam,
                    initialSelectedDifficulty = savedDifficulty,
                    initialSelectedResult = savedResult,
                )
            },
        )
    }
}

private interface HistoryFilterListener {
    fun onTeamSelected(team: String?)
    fun onDifficultySelected(difficulty: Int?)
    fun onResultSelected(result: ResultFilter)
}

private class DefaultHistoryFilterListener(
    private val state: HistoryFilterState,
) : HistoryFilterListener {
    override fun onTeamSelected(team: String?) {
        state.selectTeam(team)
    }

    override fun onDifficultySelected(difficulty: Int?) {
        state.selectDifficulty(difficulty)
    }

    override fun onResultSelected(result: ResultFilter) {
        state.selectResult(result)
    }
}

private enum class ResultFilter(
    @StringRes val labelRes: Int,
) {
    All(R.string.history_filter_all_results),
    Correct(R.string.history_result_correct),
    Skipped(R.string.history_result_skipped),
    Missed(R.string.history_result_missed),
}

private data class HistoryEntryVisuals(
    val icon: ImageVector,
    val accent: Color,
    @StringRes val labelRes: Int,
)
