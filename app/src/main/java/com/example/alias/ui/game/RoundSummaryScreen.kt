@file:Suppress("FunctionNaming", "FunctionName")

package com.example.alias.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import com.example.alias.ui.common.Scoreboard

@Composable
fun RoundSummaryScreen(vm: MainViewModel, s: GameState.TurnFinished, settings: Settings) {
    val penaltyPerSkip = remember(settings.punishSkips, settings.penaltyPerSkip) {
        if (settings.punishSkips) settings.penaltyPerSkip else 0
    }
    val timeline = remember(s.outcomes, penaltyPerSkip) { buildTimelineData(s.outcomes, penaltyPerSkip) }
    val colors = MaterialTheme.colorScheme
    val deltaColor = if (s.deltaScore >= 0) colors.tertiary else colors.error
    val stats = remember(timeline.events) { buildTurnSummaryStats(timeline.events) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            TurnSummaryHeader(
                team = s.team,
                deltaScore = s.deltaScore,
                matchOver = s.matchOver,
                stats = stats,
                deltaColor = deltaColor,
            )
        }
        item { ScoreboardCard(scores = s.scores) }
        item {
            TimelineCard(
                timeline = timeline,
                penaltyPerSkip = penaltyPerSkip,
                onOverride = { index, correct -> vm.overrideOutcome(index, correct) },
            )
        }
        item {
            Button(onClick = { vm.nextTurn() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.matchOver) stringResource(R.string.end_match) else stringResource(R.string.next_team))
            }
        }
    }
}

@Composable
private fun ScoreboardCard(
    scores: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Scoreboard(scores)
        }
    }
}

@Composable
private fun TimelineCard(
    timeline: TimelineData,
    penaltyPerSkip: Int,
    onOverride: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        if (timeline.events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.timeline_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.turn_timeline_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.timeline_score_breakdown),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    ScoreProgressGraph(
                        events = timeline.events,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                timeline.segments.forEachIndexed { segmentIndex, segment ->
                    TimelineSegmentHeader(
                        segment = segment,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        penaltyPerSkip = penaltyPerSkip,
                    )
                    segment.events.forEachIndexed { eventIndex, event ->
                        val hasPrev = segmentIndex > 0 || eventIndex > 0
                        val isLastSegment = segmentIndex == timeline.segments.lastIndex
                        val isLastEventInSegment = eventIndex == segment.events.lastIndex
                        val hasNext = !(isLastSegment && isLastEventInSegment)
                        TimelineEventRow(
                            event = event,
                            hasPrev = hasPrev,
                            hasNext = hasNext,
                            onOverride = { isCorrect -> onOverride(event.index, isCorrect) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val BONUS_STREAK_THRESHOLD = 3
private val TIMELINE_INDICATOR_CANVAS_WIDTH = 12.dp
private val TIMELINE_INDICATOR_NODE_RADIUS = 6.dp
private val TIMELINE_INDICATOR_VERTICAL_PADDING = 4.dp
private const val TIMELINE_INDICATOR_CONNECTOR_ALPHA = 0.35f

private enum class TimelineSegmentType { CORRECT, SKIP, PENDING }

private data class TimelineEvent(
    val index: Int,
    val outcome: TurnOutcome,
    val type: TimelineSegmentType,
    val change: Int,
    val cumulative: Int,
    val isBonus: Boolean,
    val elapsedMillis: Long,
)

private data class TimelineSegment(
    val type: TimelineSegmentType,
    val events: List<TimelineEvent>,
)

private data class TimelineData(
    val events: List<TimelineEvent>,
    val segments: List<TimelineSegment>,
)

private data class TurnSummaryStats(
    val totalCorrect: Int,
    val totalSkipped: Int,
    val totalPending: Int,
    val bonusCount: Int,
    val elapsedMillis: Long?,
)

private data class ScoreTimelinePoint(val time: Float, val score: Float)

private fun buildTimelineData(outcomes: List<TurnOutcome>, penaltyPerSkip: Int): TimelineData {
    if (outcomes.isEmpty()) return TimelineData(emptyList(), emptyList())
    val events = mutableListOf<TimelineEvent>()
    val start = outcomes.first().timestamp
    var cumulative = 0
    var streak = 0
    outcomes.forEachIndexed { index, outcome ->
        val type = when {
            outcome.correct -> TimelineSegmentType.CORRECT
            outcome.skipped -> TimelineSegmentType.SKIP
            else -> TimelineSegmentType.PENDING
        }
        val nextStreak = if (type == TimelineSegmentType.CORRECT) streak + 1 else 0
        val isBonus = type == TimelineSegmentType.CORRECT && nextStreak >= BONUS_STREAK_THRESHOLD
        val change = when (type) {
            TimelineSegmentType.CORRECT -> 1
            TimelineSegmentType.SKIP -> -penaltyPerSkip
            TimelineSegmentType.PENDING -> 0
        }
        cumulative += change
        val elapsed = if (index == 0) 0L else (outcome.timestamp - start).coerceAtLeast(0L)
        events += TimelineEvent(
            index = index,
            outcome = outcome,
            type = type,
            change = change,
            cumulative = cumulative,
            isBonus = isBonus,
            elapsedMillis = elapsed,
        )
        streak = if (type == TimelineSegmentType.CORRECT) nextStreak else 0
    }
    val segments = mutableListOf<TimelineSegment>()
    var currentType = events.first().type
    var bucket = mutableListOf<TimelineEvent>()
    events.forEach { event ->
        if (event.type != currentType) {
            segments += TimelineSegment(currentType, bucket.toList())
            bucket = mutableListOf()
            currentType = event.type
        }
        bucket += event
    }
    if (bucket.isNotEmpty()) {
        segments += TimelineSegment(currentType, bucket.toList())
    }
    return TimelineData(events = events, segments = segments)
}

private fun buildTurnSummaryStats(events: List<TimelineEvent>): TurnSummaryStats {
    if (events.isEmpty()) {
        return TurnSummaryStats(
            totalCorrect = 0,
            totalSkipped = 0,
            totalPending = 0,
            bonusCount = 0,
            elapsedMillis = null,
        )
    }
    var totalCorrect = 0
    var totalSkipped = 0
    var totalPending = 0
    var bonusCount = 0
    for (event in events) {
        when (event.type) {
            TimelineSegmentType.CORRECT -> totalCorrect++
            TimelineSegmentType.SKIP -> totalSkipped++
            TimelineSegmentType.PENDING -> totalPending++
        }
        if (event.isBonus) {
            bonusCount++
        }
    }
    val elapsedMillis = events.last().elapsedMillis
    return TurnSummaryStats(
        totalCorrect = totalCorrect,
        totalSkipped = totalSkipped,
        totalPending = totalPending,
        bonusCount = bonusCount,
        elapsedMillis = elapsedMillis,
    )
}

private fun buildScoreProgressPoints(events: List<TimelineEvent>): List<ScoreTimelinePoint> {
    if (events.isEmpty()) return emptyList()
    val hasElapsed = events.any { it.elapsedMillis > 0L }
    return buildList {
        add(ScoreTimelinePoint(0f, 0f))
        events.forEachIndexed { index, event ->
            val time = if (hasElapsed) event.elapsedMillis.toFloat() else (index + 1).toFloat()
            add(ScoreTimelinePoint(time, event.cumulative.toFloat()))
        }
    }
}

@Composable
private fun TurnSummaryHeader(
    team: String,
    deltaScore: Int,
    matchOver: Boolean,
    stats: TurnSummaryStats,
    deltaColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.turn_summary, team),
                    style = MaterialTheme.typography.headlineSmall,
                )
                val statusText = if (matchOver) {
                    stringResource(R.string.turn_summary_status_match_complete)
                } else {
                    stringResource(R.string.turn_summary_status_next_team)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            ScoreChangeHighlight(deltaScore = deltaScore, deltaColor = deltaColor)
            TurnSummaryStatsRow(stats = stats)
        }
    }
}

@Composable
private fun ScoreChangeHighlight(deltaScore: Int, deltaColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = deltaColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, deltaColor.copy(alpha = 0.35f)),
    ) {
        Text(
            text = stringResource(R.string.score_change, deltaScore),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = deltaColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TurnSummaryStatsRow(
    stats: TurnSummaryStats,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val statsToDisplay = buildList {
        add(
            SummaryStatDisplay(
                label = pluralStringResource(
                    R.plurals.turn_summary_stat_correct,
                    stats.totalCorrect,
                    stats.totalCorrect,
                ),
                value = stats.totalCorrect.toString(),
                color = colors.tertiary,
                style = MaterialTheme.typography.headlineSmall,
            ),
        )
        add(
            SummaryStatDisplay(
                label = pluralStringResource(
                    R.plurals.turn_summary_stat_skipped,
                    stats.totalSkipped,
                    stats.totalSkipped,
                ),
                value = stats.totalSkipped.toString(),
                color = if (stats.totalSkipped > 0) colors.error else colors.onSurfaceVariant,
                style = MaterialTheme.typography.headlineSmall,
            ),
        )
        if (stats.totalPending > 0) {
            add(
                SummaryStatDisplay(
                    label = pluralStringResource(
                        R.plurals.turn_summary_stat_pending,
                        stats.totalPending,
                        stats.totalPending,
                    ),
                    value = stats.totalPending.toString(),
                    color = colors.outline,
                    style = MaterialTheme.typography.headlineSmall,
                ),
            )
        }
        if (stats.bonusCount > 0) {
            add(
                SummaryStatDisplay(
                    label = pluralStringResource(
                        R.plurals.turn_summary_stat_bonus,
                        stats.bonusCount,
                        stats.bonusCount,
                    ),
                    value = stats.bonusCount.toString(),
                    color = colors.secondary,
                    style = MaterialTheme.typography.headlineSmall,
                ),
            )
        }
        stats.elapsedMillis?.let { elapsed ->
            val timeValue = if (elapsed < 1000L) {
                stringResource(R.string.turn_summary_stat_time_under_second_value)
            } else {
                stringResource(R.string.turn_summary_stat_time_value, elapsed / 1000f)
            }
            add(
                SummaryStatDisplay(
                    label = stringResource(R.string.turn_summary_stat_time_label),
                    value = timeValue,
                    color = colors.primary,
                    style = MaterialTheme.typography.titleMedium,
                ),
            )
        }
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        statsToDisplay.forEach { stat ->
            TurnSummaryStatCard(
                label = stat.label,
                value = stat.value,
                accentColor = stat.color,
                valueStyle = stat.style,
            )
        }
    }
}

private data class SummaryStatDisplay(
    val label: String,
    val value: String,
    val color: Color,
    val style: TextStyle,
)

@Composable
private fun TurnSummaryStatCard(
    label: String,
    value: String,
    accentColor: Color,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(min = 72.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun ScoreProgressGraph(events: List<TimelineEvent>, modifier: Modifier = Modifier) {
    val points = remember(events) { buildScoreProgressPoints(events) }
    if (points.size <= 1) return
    val colors = MaterialTheme.colorScheme
    val strokeColor = if (events.lastOrNull()?.cumulative ?: 0 >= 0) colors.tertiary else colors.error
    val fillColor = strokeColor.copy(alpha = 0.2f)
    val baselineColor = colors.outline.copy(alpha = 0.5f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.timeline_score_graph_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val xMin = points.minOf { it.time }
                val xMax = points.maxOf { it.time }
                val xRange = (xMax - xMin)
                val useIndex = xRange <= 0f
                val yMin = points.minOf { it.score }
                val yMax = points.maxOf { it.score }
                val yRange = (yMax - yMin)
                val offsets = points.mapIndexed { index, point ->
                    val xFraction = if (useIndex) {
                        index.toFloat() / points.lastIndex.toFloat()
                    } else {
                        (point.time - xMin) / xRange
                    }
                    val yFraction = if (yRange > 0f) {
                        (point.score - yMin) / yRange
                    } else {
                        0.5f
                    }
                    Offset(
                        x = xFraction.coerceIn(0f, 1f) * size.width,
                        y = size.height - yFraction.coerceIn(0f, 1f) * size.height,
                    )
                }
                val zeroLineY = when {
                    yRange > 0f && yMin <= 0f && yMax >= 0f -> {
                        val zeroFraction = (0f - yMin) / yRange
                        size.height - zeroFraction * size.height
                    }
                    yRange == 0f && yMin == 0f -> size.height * 0.5f
                    else -> null
                }
                if (zeroLineY != null) {
                    drawLine(
                        color = baselineColor,
                        start = Offset(0f, zeroLineY),
                        end = Offset(size.width, zeroLineY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (offsets.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(offsets.first().x, size.height)
                        offsets.forEach { lineTo(it.x, it.y) }
                        lineTo(offsets.last().x, size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )
                    val strokePath = Path().apply {
                        offsets.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = strokeColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawCircle(color = strokeColor, radius = 4.dp.toPx(), center = offsets.last())
                }
            }
        }
    }
}

@Composable
private fun TimelineSegmentHeader(
    segment: TimelineSegment,
    modifier: Modifier = Modifier,
    penaltyPerSkip: Int,
) {
    val color = timelineColor(segment.type)
    val title = when (segment.type) {
        TimelineSegmentType.CORRECT -> pluralStringResource(
            R.plurals.timeline_correct,
            segment.events.size,
            segment.events.size,
        )
        TimelineSegmentType.SKIP -> pluralStringResource(
            R.plurals.timeline_skipped,
            segment.events.size,
            segment.events.size,
        )
        TimelineSegmentType.PENDING -> pluralStringResource(
            R.plurals.timeline_pending,
            segment.events.size,
            segment.events.size,
        )
    }
    val delta = segment.events.sumOf { it.change }
    val subtitle = when (segment.type) {
        TimelineSegmentType.CORRECT -> stringResource(R.string.timeline_header_correct_subtitle, delta)
        TimelineSegmentType.SKIP -> if (penaltyPerSkip == 0 || delta == 0) {
            stringResource(R.string.timeline_header_skip_no_penalty)
        } else {
            stringResource(R.string.timeline_header_penalty_subtitle, delta)
        }
        TimelineSegmentType.PENDING -> stringResource(R.string.timeline_header_pending_subtitle)
    }
    val showBonus = segment.events.any { it.isBonus }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (segment.type) {
                TimelineSegmentType.CORRECT -> Icons.Filled.Check
                TimelineSegmentType.SKIP -> Icons.Filled.Close
                TimelineSegmentType.PENDING -> Icons.Filled.Schedule
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showBonus) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(stringResource(R.string.timeline_bonus_label)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    )
                }
            }
            Text(
                text = stringResource(R.string.timeline_change, delta),
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent,
    hasPrev: Boolean,
    hasNext: Boolean,
    onOverride: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = timelineColor(event.type)
    val timeLabel = if (event.elapsedMillis <= 0L) {
        stringResource(R.string.timeline_elapsed_time_start)
    } else {
        val seconds = event.elapsedMillis / 1000f
        stringResource(R.string.timeline_elapsed_time, seconds)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TimelineIndicator(
            color = color,
            showTopConnector = hasPrev,
            showBottomConnector = hasNext,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    event.outcome.word,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.isBonus) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text(stringResource(R.string.timeline_bonus_label)) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.timeline_change, event.change),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.timeline_running_total, event.cumulative),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onOverride(true) }) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.timeline_mark_correct))
            }
            IconButton(onClick = { onOverride(false) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.timeline_mark_incorrect))
            }
        }
    }
}

@Composable
private fun TimelineIndicator(color: Color, showTopConnector: Boolean, showBottomConnector: Boolean) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().width(TIMELINE_INDICATOR_CANVAS_WIDTH)) {
            val centerX = size.width / 2f
            val circleRadius = TIMELINE_INDICATOR_NODE_RADIUS.toPx()
            val centerY = circleRadius + TIMELINE_INDICATOR_VERTICAL_PADDING.toPx()
            if (showTopConnector) {
                drawLine(
                    color = color.copy(alpha = TIMELINE_INDICATOR_CONNECTOR_ALPHA),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, centerY - circleRadius),
                )
            }
            drawCircle(color = color, radius = circleRadius, center = Offset(centerX, centerY))
            if (showBottomConnector) {
                drawLine(
                    color = color.copy(alpha = TIMELINE_INDICATOR_CONNECTOR_ALPHA),
                    start = Offset(centerX, centerY + circleRadius),
                    end = Offset(centerX, size.height),
                )
            }
        }
    }
}

@Composable
private fun timelineColor(type: TimelineSegmentType): Color {
    val colors = MaterialTheme.colorScheme
    return when (type) {
        TimelineSegmentType.CORRECT -> colors.tertiary
        TimelineSegmentType.SKIP -> colors.error
        TimelineSegmentType.PENDING -> colors.outline
    }
}
