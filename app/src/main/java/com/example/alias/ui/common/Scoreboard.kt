package com.example.alias.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alias.R

@Immutable
data class ScoreEntry(
    val team: String,
    val score: Int,
    val isLeader: Boolean,
    val isTiedLeader: Boolean,
)

fun Map<String, Int>.toScoreboardEntries(): List<ScoreEntry> {
    if (isEmpty()) return emptyList()
    val sorted = entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key },
        )
    val leaderScore = sorted.first().value
    val leaderCount = sorted.count { it.value == leaderScore }
    return sorted.map { (team, score) ->
        val isLeader = score == leaderScore
        ScoreEntry(
            team = team,
            score = score,
            isLeader = isLeader,
            isTiedLeader = isLeader && leaderCount > 1,
        )
    }
}

@Composable
fun scoreBoard(
    scores: Map<String, Int>,
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int? = R.string.scoreboard,
    showLeaderIndicator: Boolean = true,
) {
    val entries = scores.toScoreboardEntries()
    Column(modifier = modifier.fillMaxWidth()) {
        titleResId?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        entries.forEach { entry ->
            scoreboardRow(
                entry = entry,
                showIndicator = showLeaderIndicator,
            )
        }
    }
}

@Composable
private fun scoreboardRow(
    entry: ScoreEntry,
    showIndicator: Boolean,
) {
    val textStyle = if (entry.isLeader) {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val textColor = if (entry.isLeader) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.isLeader && showIndicator) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
            )
        } else if (showIndicator) {
            Spacer(modifier = Modifier.width(28.dp))
        }
        Text(
            text = entry.team,
            modifier = Modifier.weight(1f),
            style = textStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entry.score.toString(),
            style = textStyle,
            color = textColor,
        )
        if (entry.isTiedLeader) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tie_suffix).trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
