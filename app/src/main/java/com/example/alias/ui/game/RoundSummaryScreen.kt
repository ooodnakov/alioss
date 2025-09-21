@file:Suppress("FunctionNaming", "FunctionName")

package com.example.alias.ui.game

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameState
import com.example.alias.domain.TurnOutcome
import com.example.alias.ui.common.scoreboard
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun roundSummaryScreen(vm: MainViewModel, s: GameState.TurnFinished, settings: Settings) {
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
            turnSummaryHeader(
                team = s.team,
                deltaScore = s.deltaScore,
                matchOver = s.matchOver,
                deltaColor = deltaColor,
            )
        }
        item {
            timelineCard(
                timeline = timeline,
                penaltyPerSkip = penaltyPerSkip,
                onOverride = { index, correct -> vm.overrideOutcome(index, correct) },
            )
        }
        item { scoreboardCard(scores = s.scores, stats = stats) }
        item {
            Button(onClick = { vm.nextTurn() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.matchOver) stringResource(R.string.end_match) else stringResource(R.string.next_team))
            }
        }
    }
}

@Composable
private fun scoreboardCard(
    scores: Map<String, Int>,
    stats: TurnSummaryStats,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            scoreboard(scores)
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.turn_summary_stats_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                turnSummaryStatsRow(stats = stats)
            }
        }
    }
}

@Composable
private fun timelineCard(
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                }
                ExpandableSection(
                    title = stringResource(R.string.timeline_breakdown_title),
                    subtitle = stringResource(R.string.timeline_breakdown_subtitle),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    initiallyExpanded = true,
                    contentArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    timeline.segments.forEach { segment ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            timelineSegmentHeader(
                                segment = segment,
                                penaltyPerSkip = penaltyPerSkip,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                segment.events.forEach { event ->
                                    timelineEventBlock(
                                        event = event,
                                        onToggle = { isCorrect -> onOverride(event.index, isCorrect) },
                                    )
                                }
                            }
                        }
                    }
                }
                ExpandableSection(
                    title = stringResource(R.string.timeline_graphs_title),
                    subtitle = stringResource(R.string.timeline_graphs_subtitle),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    initiallyExpanded = false,
                    contentArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    scoreProgressGraph(
                        events = timeline.events,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    timeBetweenWordsGraph(
                        events = timeline.events,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private const val BONUS_STREAK_THRESHOLD = 3

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
private data class TimeBetweenWordPoint(val index: Int, val seconds: Float)

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

private fun buildTimeBetweenPoints(events: List<TimelineEvent>): List<TimeBetweenWordPoint> {
    if (events.isEmpty()) return emptyList()
    var previous = 0L
    return events.mapIndexed { index, event ->
        val delta = (event.elapsedMillis - previous).coerceAtLeast(0L)
        previous = event.elapsedMillis
        TimeBetweenWordPoint(index = index, seconds = delta / 1000f)
    }
}

private fun generateAxisTicks(min: Float, max: Float, desiredTickCount: Int = 5): List<Float> {
    if (!min.isFinite() || !max.isFinite() || desiredTickCount <= 0) return emptyList()
    if (min == max) return listOf(min)
    val actualMin = min(min, max)
    val actualMax = max(min, max)
    val range = actualMax - actualMin
    if (range <= 0f) return listOf(actualMin)
    val tickCount = max(2, desiredTickCount)
    val niceRange = niceNumber(range, round = false)
    val tickSpacing = niceNumber(niceRange / (tickCount - 1), round = true)
    if (tickSpacing == 0f) return listOf(actualMin)
    val niceMin = floor((actualMin / tickSpacing).toDouble()).toFloat() * tickSpacing
    val niceMax = ceil((actualMax / tickSpacing).toDouble()).toFloat() * tickSpacing
    val ticks = mutableListOf<Float>()
    var value = niceMin
    var iteration = 0
    while (value <= niceMax + tickSpacing * 0.5f && iteration < 100) {
        ticks += value
        value += tickSpacing
        iteration++
    }
    return ticks
}

private fun niceNumber(range: Float, round: Boolean): Float {
    if (range <= 0f || range.isNaN() || range.isInfinite()) return 0f
    val exponent = floor(log10(range.toDouble())).toInt()
    val fraction = range / 10f.pow(exponent.toFloat())
    val niceFraction = if (round) {
        when {
            fraction < 1.5f -> 1f
            fraction < 3f -> 2f
            fraction < 7f -> 5f
            else -> 10f
        }
    } else {
        when {
            fraction <= 1f -> 1f
            fraction <= 2f -> 2f
            fraction <= 5f -> 5f
            else -> 10f
        }
    }
    return niceFraction * 10f.pow(exponent.toFloat())
}

private fun formatAxisValue(value: Float, allowFractions: Boolean): String {
    val adjusted = if (abs(value) < 1e-4f) 0f else value
    return if (allowFractions) {
        val rounded = adjusted.roundToInt()
        if (abs(adjusted - rounded) < 0.05f) {
            rounded.toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", adjusted)
        }
    } else {
        adjusted.roundToInt().toString()
    }
}

@Composable
private fun turnSummaryHeader(
    team: String,
    deltaScore: Int,
    matchOver: Boolean,
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
            scoreChangeHighlight(deltaScore = deltaScore, deltaColor = deltaColor)
        }
    }
}

@Composable
private fun scoreChangeHighlight(deltaScore: Int, deltaColor: Color, modifier: Modifier = Modifier) {
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
private fun turnSummaryStatsRow(
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
            turnSummaryStatCard(
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
private fun turnSummaryStatCard(
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
private fun scoreProgressGraph(events: List<TimelineEvent>, modifier: Modifier = Modifier) {
    val points = remember(events) { buildScoreProgressPoints(events) }
    if (points.size <= 1) return
    val hasElapsedData = remember(events) { events.any { it.elapsedMillis > 0L } }
    val colors = MaterialTheme.colorScheme
    val strokeColor = if (events.lastOrNull()?.cumulative ?: 0 >= 0) colors.tertiary else colors.error
    val fillColor = strokeColor.copy(alpha = 0.2f)
    val baselineColor = colors.outline.copy(alpha = 0.5f)
    val xAxisTitle = if (hasElapsedData) {
        stringResource(R.string.timeline_graph_axis_elapsed_seconds)
    } else {
        stringResource(R.string.timeline_graph_axis_word_order)
    }
    val yAxisTitle = stringResource(R.string.timeline_graph_axis_cumulative_score)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.timeline_score_graph_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yAxisTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .rotate(-90f)
                        .padding(end = 2.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val density = LocalDensity.current
                    val axisLabelStyle = MaterialTheme.typography.labelSmall
                    val axisLabelTextSizePx = remember(axisLabelStyle, density) {
                        with(density) {
                            (
                                if (axisLabelStyle.fontSize != TextUnit.Unspecified) {
                                    axisLabelStyle.fontSize
                                } else {
                                    12.sp
                                }
                                ).toPx()
                        }
                    }
                    val axisLabelColor = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    val xAxisPaint = remember(axisLabelColor, axisLabelTextSizePx) {
                        Paint().apply {
                            color = axisLabelColor.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = axisLabelTextSizePx
                            isAntiAlias = true
                        }
                    }
                    val yAxisPaint = remember(axisLabelColor, axisLabelTextSizePx) {
                        Paint().apply {
                            color = axisLabelColor.toArgb()
                            textAlign = Paint.Align.RIGHT
                            textSize = axisLabelTextSizePx
                            isAntiAlias = true
                        }
                    }
                    val xMin = remember(points) { points.minOf { it.time } }
                    val xMax = remember(points) { points.maxOf { it.time } }
                    val yMin = remember(points) { points.minOf { it.score } }
                    val yMax = remember(points) { points.maxOf { it.score } }
                    val useIndex = remember(points) { (xMax - xMin) <= 0f }
                    val xTickValues = remember(points, useIndex, xMin, xMax) {
                        val rawTicks = if (useIndex) {
                            generateAxisTicks(0f, points.lastIndex.toFloat())
                        } else {
                            generateAxisTicks(xMin, xMax)
                        }
                        rawTicks.filter { tick ->
                            if (useIndex) {
                                tick >= 0f && tick <= points.lastIndex.toFloat()
                            } else {
                                tick >= min(xMin, xMax) && tick <= max(xMin, xMax)
                            }
                        }
                    }
                    val yTickValues = remember(points, yMin, yMax) {
                        val rawTicks = if (yMin == yMax) listOf(yMin) else generateAxisTicks(yMin, yMax)
                        rawTicks.filter { tick -> tick >= min(yMin, yMax) && tick <= max(yMin, yMax) }
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                    ) {
                        val chartLeftPadding = 2.dp.toPx()
                        val chartRightPadding = 4.dp.toPx()
                        val chartTopPadding = 4.dp.toPx()
                        val chartBottomPadding = 8.dp.toPx()
                        val chartWidth = size.width - chartLeftPadding - chartRightPadding
                        val chartHeight = size.height - chartTopPadding - chartBottomPadding
                        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

                        val xRange = xMax - xMin
                        val yRange = yMax - yMin
                        val chartLeft = chartLeftPadding
                        val chartBottom = chartTopPadding + chartHeight
                        val axisStrokeWidth = 1.dp.toPx()
                        val tickLength = 6.dp.toPx()

                        fun mapX(index: Int, time: Float): Float {
                            val fraction = if (useIndex) {
                                index.toFloat() / points.lastIndex.toFloat()
                            } else {
                                if (xRange <= 0f) 0f else (time - xMin) / xRange
                            }
                            return chartLeft + fraction.coerceIn(0f, 1f) * chartWidth
                        }

                        fun mapXValue(value: Float): Float {
                            val fraction = if (useIndex) {
                                value / points.lastIndex.toFloat()
                            } else {
                                if (xRange <= 0f) 0f else (value - xMin) / xRange
                            }
                            return chartLeft + fraction.coerceIn(0f, 1f) * chartWidth
                        }

                        fun mapY(score: Float): Float {
                            val fraction = if (yRange > 0f) {
                                (score - yMin) / yRange
                            } else {
                                0.5f
                            }
                            return chartTopPadding + (1f - fraction.coerceIn(0f, 1f)) * chartHeight
                        }

                        drawLine(
                            color = axisLabelColor,
                            start = Offset(chartLeft, chartTopPadding),
                            end = Offset(chartLeft, chartBottom),
                            strokeWidth = axisStrokeWidth,
                        )
                        drawLine(
                            color = axisLabelColor,
                            start = Offset(chartLeft, chartBottom),
                            end = Offset(chartLeft + chartWidth, chartBottom),
                            strokeWidth = axisStrokeWidth,
                        )

                        val xFontMetrics = xAxisPaint.fontMetrics
                        val yFontMetrics = yAxisPaint.fontMetrics
                        val xLabelTop = chartBottom + tickLength + 4.dp.toPx()
                        val yLabelX = chartLeft - tickLength - 4.dp.toPx()

                        xTickValues.forEach { tick ->
                            val xPosition = mapXValue(tick)
                            drawLine(
                                color = axisLabelColor,
                                start = Offset(xPosition, chartBottom),
                                end = Offset(xPosition, chartBottom + tickLength),
                                strokeWidth = axisStrokeWidth,
                            )
                            val displayValue = if (hasElapsedData && !useIndex) tick / 1000f else tick
                            val label = formatAxisValue(displayValue, allowFractions = hasElapsedData && !useIndex)
                            val baseline = xLabelTop - xFontMetrics.ascent
                            drawContext.canvas.nativeCanvas.drawText(label, xPosition, baseline, xAxisPaint)
                        }

                        yTickValues.forEach { tick ->
                            val yPosition = mapY(tick)
                            drawLine(
                                color = axisLabelColor,
                                start = Offset(chartLeft - tickLength, yPosition),
                                end = Offset(chartLeft, yPosition),
                                strokeWidth = axisStrokeWidth,
                            )
                            val label = formatAxisValue(tick, allowFractions = false)
                            val baseline = yPosition - (yFontMetrics.ascent + yFontMetrics.descent) / 2f
                            drawContext.canvas.nativeCanvas.drawText(label, yLabelX, baseline, yAxisPaint)
                        }

                        val zeroLineY = when {
                            yRange > 0f && yMin <= 0f && yMax >= 0f -> mapY(0f)
                            yRange == 0f && yMin == 0f -> chartTopPadding + chartHeight * 0.5f
                            else -> null
                        }
                        if (zeroLineY != null) {
                            drawLine(
                                color = baselineColor,
                                start = Offset(chartLeft, zeroLineY),
                                end = Offset(chartLeft + chartWidth, zeroLineY),
                                strokeWidth = axisStrokeWidth,
                            )
                        }

                        val offsets = points.mapIndexed { index, point ->
                            Offset(
                                x = mapX(index, point.time),
                                y = mapY(point.score),
                            )
                        }
                        if (offsets.size >= 2) {
                            val fillPath = Path().apply {
                                moveTo(offsets.first().x, chartBottom)
                                offsets.forEach { lineTo(it.x, it.y) }
                                lineTo(offsets.last().x, chartBottom)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                                    startY = chartTopPadding,
                                    endY = chartBottom,
                                ),
                            )
                            val strokePath = Path().apply {
                                offsets.forEachIndexed { index, offset ->
                                    if (index == 0) {
                                        moveTo(offset.x, offset.y)
                                    } else {
                                        lineTo(offset.x, offset.y)
                                    }
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
                    Text(
                        text = xAxisTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun timeBetweenWordsGraph(events: List<TimelineEvent>, modifier: Modifier = Modifier) {
    val points = remember(events) { buildTimeBetweenPoints(events) }
    if (points.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val maxSeconds = (points.maxOfOrNull { it.seconds } ?: 0f).coerceAtLeast(0.5f)
    val average = if (points.isNotEmpty()) {
        points.sumOf { it.seconds.toDouble() }.toFloat() / points.size
    } else {
        0f
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.timeline_time_between_graph_label),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        val xAxisTitle = stringResource(R.string.timeline_graph_axis_word_order)
        val yAxisTitle = stringResource(R.string.timeline_graph_axis_seconds_per_word)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yAxisTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .rotate(-90f)
                        .padding(end = 2.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val density = LocalDensity.current
                    val axisLabelStyle = MaterialTheme.typography.labelSmall
                    val axisLabelTextSizePx = remember(axisLabelStyle, density) {
                        with(density) {
                            (
                                if (axisLabelStyle.fontSize != TextUnit.Unspecified) {
                                    axisLabelStyle.fontSize
                                } else {
                                    12.sp
                                }
                                ).toPx()
                        }
                    }
                    val axisLabelColor = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    val xAxisPaint = remember(axisLabelColor, axisLabelTextSizePx) {
                        Paint().apply {
                            color = axisLabelColor.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = axisLabelTextSizePx
                            isAntiAlias = true
                        }
                    }
                    val yAxisPaint = remember(axisLabelColor, axisLabelTextSizePx) {
                        Paint().apply {
                            color = axisLabelColor.toArgb()
                            textAlign = Paint.Align.RIGHT
                            textSize = axisLabelTextSizePx
                            isAntiAlias = true
                        }
                    }
                    val barCount = points.size
                    val xTickValues = remember(barCount) {
                        generateAxisTicks(1f, barCount.toFloat()).filter { tick ->
                            tick >= 1f && tick <= barCount.toFloat()
                        }
                    }
                    val yTickValues = remember(maxSeconds) {
                        generateAxisTicks(0f, maxSeconds).filter { tick -> tick >= 0f && tick <= maxSeconds }
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                    ) {
                        val chartLeftPadding = 2.dp.toPx()
                        val chartRightPadding = 4.dp.toPx()
                        val chartTopPadding = 4.dp.toPx()
                        val chartBottomPadding = 8.dp.toPx()
                        val chartWidth = size.width - chartLeftPadding - chartRightPadding
                        val chartHeight = size.height - chartTopPadding - chartBottomPadding
                        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

                        val chartLeft = chartLeftPadding
                        val chartBottom = chartTopPadding + chartHeight
                        val axisStrokeWidth = 1.dp.toPx()
                        val tickLength = 6.dp.toPx()

                        val slotWidth = if (barCount > 0) chartWidth / barCount else 0f
                        val barWidth = slotWidth * 0.6f
                        val halfSpacing = (slotWidth - barWidth) / 2f

                        fun mapY(seconds: Float): Float {
                            val fraction = (seconds / maxSeconds).coerceIn(0f, 1f)
                            return chartTopPadding + (1f - fraction) * chartHeight
                        }

                        fun barStart(index: Int): Float {
                            return chartLeft + index * slotWidth + halfSpacing
                        }

                        fun mapXValue(value: Float): Float {
                            val clamped = value.coerceIn(1f, barCount.toFloat())
                            val fraction = (clamped - 0.5f) / barCount.toFloat()
                            return chartLeft + fraction.coerceIn(0f, 1f) * chartWidth
                        }

                        drawLine(
                            color = axisLabelColor,
                            start = Offset(chartLeft, chartTopPadding),
                            end = Offset(chartLeft, chartBottom),
                            strokeWidth = axisStrokeWidth,
                        )
                        drawLine(
                            color = axisLabelColor,
                            start = Offset(chartLeft, chartBottom),
                            end = Offset(chartLeft + chartWidth, chartBottom),
                            strokeWidth = axisStrokeWidth,
                        )

                        val xFontMetrics = xAxisPaint.fontMetrics
                        val yFontMetrics = yAxisPaint.fontMetrics
                        val xLabelTop = chartBottom + tickLength + 4.dp.toPx()
                        val yLabelX = chartLeft - tickLength - 4.dp.toPx()

                        xTickValues.forEach { tick ->
                            val xPosition = mapXValue(tick)
                            drawLine(
                                color = axisLabelColor,
                                start = Offset(xPosition, chartBottom),
                                end = Offset(xPosition, chartBottom + tickLength),
                                strokeWidth = axisStrokeWidth,
                            )
                            val label = formatAxisValue(tick, allowFractions = false)
                            val baseline = xLabelTop - xFontMetrics.ascent
                            drawContext.canvas.nativeCanvas.drawText(label, xPosition, baseline, xAxisPaint)
                        }

                        yTickValues.forEach { tick ->
                            val yPosition = mapY(tick)
                            drawLine(
                                color = axisLabelColor,
                                start = Offset(chartLeft - tickLength, yPosition),
                                end = Offset(chartLeft, yPosition),
                                strokeWidth = axisStrokeWidth,
                            )
                            val label = formatAxisValue(tick, allowFractions = true)
                            val baseline = yPosition - (yFontMetrics.ascent + yFontMetrics.descent) / 2f
                            drawContext.canvas.nativeCanvas.drawText(label, yLabelX, baseline, yAxisPaint)
                        }

                        val averageY = mapY(average)
                        drawLine(
                            color = colors.primary.copy(alpha = 0.35f),
                            start = Offset(chartLeft, averageY),
                            end = Offset(chartLeft + chartWidth, averageY),
                            strokeWidth = axisStrokeWidth,
                        )

                        points.forEachIndexed { index, point ->
                            val top = mapY(point.seconds)
                            val barHeight = chartBottom - top
                            val x = barStart(index)
                            drawRoundRect(
                                color = colors.primary,
                                topLeft = Offset(x, top),
                                size = Size(width = barWidth, height = barHeight),
                                cornerRadius = CornerRadius(12f, 12f),
                            )
                        }
                    }
                    Text(
                        text = xAxisTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.timeline_time_between_graph_average, average),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    contentArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    contentPadding: PaddingValues = PaddingValues(top = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "timelineSectionRotation")
    val colors = MaterialTheme.colorScheme
    val stateLabel = if (expanded) {
        stringResource(R.string.timeline_section_state_expanded)
    } else {
        stringResource(R.string.timeline_section_state_collapsed)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = stateLabel }
            .clickable(role = Role.Button) { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant.copy(alpha = if (expanded) 0.6f else 0.45f),
        tonalElevation = if (expanded) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateLabel }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = contentArrangement,
            content = content,
        )
    }
}

@Composable
private fun timelineSegmentHeader(
    segment: TimelineSegment,
    modifier: Modifier = Modifier,
    penaltyPerSkip: Int,
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = timelineColor(segment.type)
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
    val icon = when (segment.type) {
        TimelineSegmentType.CORRECT -> Icons.Filled.Check
        TimelineSegmentType.SKIP -> Icons.Filled.Close
        TimelineSegmentType.PENDING -> Icons.Filled.Schedule
    }
    val deltaColor = if (delta == 0) colors.onSurfaceVariant else accentColor

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                contentColor = accentColor,
                shape = CircleShape,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (showBonus) {
                Surface(
                    color = colors.secondaryContainer.copy(alpha = 0.7f),
                    contentColor = colors.onSecondaryContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        text = stringResource(R.string.timeline_bonus_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.timeline_change, delta),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun timelineEventBlock(
    event: TimelineEvent,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (event.type) {
        TimelineSegmentType.CORRECT -> colors.tertiaryContainer to colors.onTertiaryContainer
        TimelineSegmentType.SKIP -> colors.errorContainer to colors.onErrorContainer
        TimelineSegmentType.PENDING -> colors.surfaceVariant to colors.onSurface
    }
    val borderColor = timelineColor(event.type).copy(alpha = 0.4f)
    val supportColor = contentColor.copy(alpha = 0.8f)
    val stateLabel = when {
        event.outcome.correct -> stringResource(R.string.timeline_word_state_correct)
        event.outcome.skipped -> stringResource(R.string.timeline_word_state_skipped)
        else -> stringResource(R.string.timeline_word_state_pending)
    }
    val stateIcon = when {
        event.outcome.correct -> Icons.Filled.Check
        event.outcome.skipped -> Icons.Filled.Close
        else -> Icons.Filled.Schedule
    }

    val blockShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), blockShape)
            .semantics { stateDescription = stateLabel }
            .clickable(role = Role.Button) {
                val target = !event.outcome.correct
                onToggle(target)
            },
        shape = blockShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = stateLabel }
                .clickable(role = Role.Button) {
                    val target = !event.outcome.correct
                    onToggle(target)
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        event.outcome.word,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.timeline_change, event.change),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.timeline_running_total, event.cumulative),
                        style = MaterialTheme.typography.bodySmall,
                        color = supportColor,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(stateIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
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
