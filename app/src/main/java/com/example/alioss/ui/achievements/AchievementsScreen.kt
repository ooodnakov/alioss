package com.example.alioss.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alioss.R
import com.example.alioss.data.achievements.AchievementState
import kotlin.math.min

@Composable
fun achievementsScreen(
    achievements: List<AchievementState>,
    onBack: () -> Unit,
) {
    val ordered = remember(achievements) {
        achievements.sortedWith(
            compareByDescending<AchievementState> { it.progress.isUnlocked }
                .thenByDescending { state ->
                    val target = state.progress.target.takeIf { it > 0 } ?: 1
                    state.progress.current.toFloat() / target
                }
                .thenBy { it.definition.title },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.title_achievements),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }

        if (ordered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.achievements_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ordered) { state ->
                    AchievementDetailCard(state = state)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun AchievementDetailCard(state: AchievementState) {
    val isUnlocked = state.progress.isUnlocked
    val colors = MaterialTheme.colorScheme
    val ratio = state.progress.target.takeIf { it > 0 }?.let {
        (state.progress.current.toFloat() / it).coerceIn(0f, 1f)
    } ?: 0f
    val progressLabel = stringResource(
        R.string.achievements_progress,
        min(state.progress.current, state.progress.target),
        state.progress.target,
    )
    val statusLabel = if (isUnlocked) {
        stringResource(R.string.achievements_unlocked_badge)
    } else {
        stringResource(R.string.achievements_locked_badge)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = if (isUnlocked) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents
                val iconTint = if (isUnlocked) colors.primary else colors.onSurfaceVariant
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.definition.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.definition.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Surface(
                    color = if (isUnlocked) colors.primaryContainer else colors.surfaceVariant,
                    contentColor = if (isUnlocked) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
