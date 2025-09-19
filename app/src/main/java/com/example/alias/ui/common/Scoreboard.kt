package com.example.alias.ui.common

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alias.R

@Composable
fun Scoreboard(scores: Map<String, Int>) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLeader) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
                Text(
                    text = "$team: $score$suffix",
                    style = textStyle,
                    color = textColor,
                )
            }
        }
    }
}
