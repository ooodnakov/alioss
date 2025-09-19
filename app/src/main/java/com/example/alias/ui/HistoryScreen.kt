package com.example.alias.ui

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alias.R
import com.example.alias.data.db.TurnHistoryEntity
import kotlin.math.roundToInt

private const val SPARKLINE_RECENT_ENTRIES_COUNT = 12
private val SPARKLINE_DOT_RADIUS = 4.dp
private const val SPARKLINE_STROKE_WIDTH = 4f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(history: List<TurnHistoryEntity>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_history))
        }
        return
    }

    val sorted = remember(history) { history.sortedByDescending { it.timestamp } }
    val teams = remember(history) { history.map { it.team }.distinct().sorted() }
    val difficulties = remember(history) { history.mapNotNull { it.difficulty }.distinct().sorted() }

    var selectedTeam by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDifficulty by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedResult by rememberSaveable { mutableStateOf(ResultFilter.All) }

    val filtered = remember(sorted, selectedTeam, selectedDifficulty, selectedResult) {
        sorted.filter { entry ->
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.title_history), style = MaterialTheme.typography.headlineSmall)
        HistoryFilters(
            teams = teams,
            difficulties = difficulties,
            selectedTeam = selectedTeam,
            onTeamSelected = { selectedTeam = it },
            selectedDifficulty = selectedDifficulty,
            onDifficultySelected = { selectedDifficulty = it },
            selectedResult = selectedResult,
            onResultSelected = { selectedResult = it }
        )
        HistoryPerformanceSection(history = sorted)
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.history_section_recent_turns), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.history_recent_count, filtered.size, sorted.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.history_no_entries_for_filters),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { entry ->
                    HistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryFilters(
    teams: List<String>,
    difficulties: List<Int>,
    selectedTeam: String?,
    onTeamSelected: (String?) -> Unit,
    selectedDifficulty: Int?,
    onDifficultySelected: (Int?) -> Unit,
    selectedResult: ResultFilter,
    onResultSelected: (ResultFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_team),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTeam == null,
                    onClick = { onTeamSelected(null) },
                    label = { Text(stringResource(R.string.history_filter_all_teams)) }
                )
                teams.forEach { team ->
                    val selected = selectedTeam == team
                    FilterChip(
                        selected = selected,
                        onClick = { onTeamSelected(if (selected) null else team) },
                        label = { Text(team) }
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_difficulty),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDifficulty == null,
                    onClick = { onDifficultySelected(null) },
                    label = { Text(stringResource(R.string.history_filter_all_difficulties)) }
                )
                if (difficulties.isEmpty()) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = { Text(stringResource(R.string.history_filter_unknown_difficulty)) }
                    )
                } else {
                    difficulties.forEach { level ->
                        val selected = selectedDifficulty == level
                        FilterChip(
                            selected = selected,
                            onClick = { onDifficultySelected(if (selected) null else level) },
                            label = { Text(stringResource(R.string.word_difficulty_value, level)) }
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.history_filter_result),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedResult == filter,
                        onClick = { onResultSelected(filter) },
                        label = { Text(stringResource(filter.labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryPerformanceSection(history: List<TurnHistoryEntity>) {
    val grouped = remember(history) { history.groupBy { it.team } }
    if (grouped.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.history_section_performance), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(grouped.entries.sortedBy { it.key }) { (team, entries) ->
                HistoryPerformanceCard(team = team, entries = entries)
            }
        }
    }
}

@Composable
private fun HistoryPerformanceCard(team: String, entries: List<TurnHistoryEntity>) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(team, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.history_performance_summary, correct, total, percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Sparkline(
                values = sparkValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val baseline = color.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f
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
            style = Stroke(width = SPARKLINE_STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryEntryCard(entry: TurnHistoryEntity) {
    val visuals = when {
        entry.correct -> ResultVisuals(
            icon = Icons.Filled.Check,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            labelRes = R.string.history_result_correct
        )
        entry.skipped -> ResultVisuals(
            icon = Icons.Filled.Close,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            labelRes = R.string.history_result_skipped
        )
        else -> ResultVisuals(
            icon = Icons.Filled.Close,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            labelRes = R.string.history_result_missed
        )
    }
    val relativeTime = remember(entry.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            entry.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val chipColors = AssistChipDefaults.assistChipColors(
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = visuals.container)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(visuals.icon, contentDescription = null, tint = visuals.content)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(entry.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = {}, enabled = false, colors = chipColors, label = { Text(entry.team) })
                    val difficultyLabel = entry.difficulty?.let { stringResource(R.string.word_difficulty_value, it) }
                        ?: stringResource(R.string.history_filter_unknown_difficulty)
                    AssistChip(onClick = {}, enabled = false, colors = chipColors, label = { Text(difficultyLabel) })
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        colors = chipColors,
                        label = { Text(stringResource(visuals.labelRes)) }
                    )
                }
            }
        }
    }
}

private enum class ResultFilter(@StringRes val labelRes: Int) {
    All(R.string.history_filter_all_results),
    Correct(R.string.history_result_correct),
    Skipped(R.string.history_result_skipped),
    Missed(R.string.history_result_missed),
}

private data class ResultVisuals(
    val icon: ImageVector,
    val container: Color,
    val content: Color,
    @StringRes val labelRes: Int,
)
