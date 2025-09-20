package com.example.alias.ui.home

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.alias.R
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.data.settings.Settings
import com.example.alias.domain.GameState

private data class HomeHeroState(
    val gameState: GameState?,
    val settings: Settings,
    val decks: List<DeckEntity>,
    val recentHistory: List<TurnHistoryEntity>,
)

private data class HomeHeroActions(
    val onResumeMatch: () -> Unit,
    val onStartNewMatch: () -> Unit,
    val onHistory: () -> Unit,
    val onSettings: () -> Unit,
    val onDecks: () -> Unit,
)

@Composable
fun homeScreen(
    state: HomeViewState,
    actions: HomeActions,
) {
    val heroState = HomeHeroState(
        gameState = gameState,
        settings = settings,
        decks = decks,
        recentHistory = recentHistory,
    )
    val heroActions = HomeHeroActions(
        onResumeMatch = onResumeMatch,
        onStartNewMatch = onStartNewMatch,
        onHistory = onHistory,
        onDecks = onDecks,
    )
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1.4f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                homeHeroSection(state = heroState, actions = heroActions)
                homeActionCard(
                    icon = Icons.Filled.PlayArrow,
                    title = stringResource(R.string.quick_play),
                    subtitle = stringResource(R.string.quick_play_subtitle),
                    onClick = onStartNewMatch,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                )
                quickPlayActionCard(onStartNewMatch = actions.onStartNewMatch)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                navigationActionCards(actions = actions)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            homeHeroSection(state = heroState, actions = heroActions)
            homeActionCard(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.quick_play),
                subtitle = stringResource(R.string.quick_play_subtitle),
                onClick = onStartNewMatch,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            )
            homeActionCard(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = stringResource(R.string.title_decks),
                subtitle = stringResource(R.string.decks_subtitle),
                onClick = onDecks,
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
            )
            homeActionCard(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.title_settings),
                subtitle = stringResource(R.string.settings_subtitle),
                onClick = onSettings,
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
            )
            homeActionCard(
                icon = Icons.Filled.History,
                title = stringResource(R.string.title_history),
                subtitle = stringResource(R.string.history_subtitle),
                onClick = onHistory,
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
            )
            quickPlayActionCard(onStartNewMatch = actions.onStartNewMatch)
            navigationActionCards(actions = actions)
        }
    }
}

@Composable
private fun quickPlayActionCard(onStartNewMatch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    homeActionCard(
        icon = Icons.Filled.PlayArrow,
        title = stringResource(R.string.quick_play),
        subtitle = stringResource(R.string.quick_play_subtitle),
        onClick = onStartNewMatch,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    )
}

@Composable
private fun ColumnScope.navigationActionCards(actions: HomeActions) {
    val colors = MaterialTheme.colorScheme
    homeActionCard(
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        title = stringResource(R.string.title_decks),
        subtitle = stringResource(R.string.decks_subtitle),
        onClick = actions.onDecks,
        containerColor = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    )
    homeActionCard(
        icon = Icons.Filled.Settings,
        title = stringResource(R.string.title_settings),
        subtitle = stringResource(R.string.settings_subtitle),
        onClick = actions.onSettings,
        containerColor = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
    )
    homeActionCard(
        icon = Icons.Filled.History,
        title = stringResource(R.string.title_history),
        subtitle = stringResource(R.string.history_subtitle),
        onClick = actions.onHistory,
        containerColor = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
    )
}

@Composable
private fun homeHeroSection(
    state: HomeHeroState,
    actions: HomeHeroActions,
) {
    val (gameState, settings, decks, recentHistory) = state
    val colors = MaterialTheme.colorScheme
    val contentColor = colors.onPrimaryContainer
    val gradient = remember(colors) {
        Brush.verticalGradient(
            0f to colors.primary.copy(alpha = 0.35f),
            1f to Color.Transparent,
        )
    }
    val gameState = state.gameState
    val settings = state.settings
    val decks = state.decks
    val recentHistory = state.recentHistory
    val liveScores = when (gameState) {
        is GameState.TurnPending -> gameState.scores
        is GameState.TurnFinished -> gameState.scores
        is GameState.MatchFinished -> gameState.scores
        else -> null
    }
    val scoreboardState = rememberSaveable(settings.teams, saver = ScoreboardSaver) {
        mutableStateMapOf<String, Int>().apply {
            settings.teams.forEach { team -> this[team] = 0 }
        }
    }
    LaunchedEffect(liveScores) {
        if (liveScores != null) {
            scoreboardState.clear()
            settings.teams.forEach { team ->
                scoreboardState[team] = liveScores[team] ?: 0
            }
        }
    }
    val scoreboard = liveScores ?: scoreboardState.toMap()
    val hasProgress = recentHistory.isNotEmpty() || scoreboard.values.any { it != 0 }
    val heroTitle = when (gameState) {
        is GameState.MatchFinished -> stringResource(R.string.home_hero_title_victory)
        is GameState.TurnFinished -> if (gameState.matchOver) {
            stringResource(R.string.home_hero_title_victory)
        } else {
            stringResource(R.string.home_hero_title_ready)
        }
        is GameState.TurnActive -> stringResource(R.string.home_hero_title_playing)
        is GameState.TurnPending -> stringResource(R.string.home_hero_title_ready)
        else -> stringResource(R.string.home_hero_title_idle)
    }
    val heroSubtitle = when (gameState) {
        null, GameState.Idle -> stringResource(R.string.home_hero_idle_subtitle, settings.teams.size)
        is GameState.TurnPending -> stringResource(R.string.home_hero_pending_subtitle, gameState.team)
        is GameState.TurnActive -> stringResource(
            R.string.home_hero_active_subtitle,
            gameState.team,
            gameState.timeRemaining,
        )
        is GameState.TurnFinished -> if (gameState.matchOver) {
            stringResource(R.string.home_match_point, gameState.team)
        } else {
            stringResource(R.string.home_hero_finished_subtitle, gameState.team, gameState.deltaScore)
        }
        is GameState.MatchFinished -> {
            val maxScore = scoreboard.maxOfOrNull { it.value }
            if (maxScore == null) {
                stringResource(R.string.home_match_finished_tie, 0)
            } else {
                val winners = scoreboard.filterValues { it == maxScore }.keys
                if (winners.size > 1) {
                    stringResource(R.string.home_match_finished_tie, maxScore)
                } else {
                    stringResource(R.string.home_match_finished_winner, winners.first(), maxScore)
                }
            }
        }
    }
    val favoriteDecks = remember(settings.enabledDeckIds, decks) {
        val enabled = settings.enabledDeckIds
        decks.filter { enabled.contains(it.id) }
            .sortedBy { it.name }
            .take(3)
    }
    val extraDecks = (settings.enabledDeckIds.size - favoriteDecks.size).coerceAtLeast(0)
    val highlight = recentHistory.firstOrNull()
    val highlightText = when {
        highlight == null -> stringResource(R.string.home_highlight_empty)
        highlight.correct -> stringResource(R.string.home_highlight_correct, highlight.team, highlight.word)
        else -> stringResource(R.string.home_highlight_skip, highlight.team, highlight.word)
    }
    val highlightIcon = when {
        highlight == null -> null
        highlight.correct -> Icons.Filled.Check
        else -> Icons.Filled.Close
    }
    val highlightTint = when {
        highlight == null -> contentColor.copy(alpha = 0.7f)
        highlight.correct -> colors.tertiary
        else -> colors.error
    }
    val showResume = gameState != null && gameState !is GameState.Idle

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.primaryContainer,
            contentColor = contentColor,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    homeLogo(size = 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(heroTitle, style = MaterialTheme.typography.headlineSmall, color = contentColor)
                        Text(
                            heroSubtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor.copy(alpha = 0.9f),
                        )
                    }
                }
                homeScoreboardSection(scoreboard = scoreboard, hasProgress = hasProgress, contentColor = contentColor)
                favoriteDecksSection(
                    favorites = favoriteDecks,
                    extra = extraDecks,
                    onDecks = actions.onDecks,
                    contentColor = contentColor,
                )
                recentHighlightSection(
                    text = highlightText,
                    icon = highlightIcon,
                    iconTint = highlightTint,
                    contentColor = contentColor,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showResume) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = actions.onResumeMatch,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.resume_match))
                            }
                            OutlinedButton(
                                onClick = actions.onStartNewMatch,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.start_new_game))
                            }
                        }
                    } else {
                        Button(
                            onClick = actions.onStartNewMatch,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.start_new_game))
                        }
                    }
                    TextButton(
                        onClick = actions.onHistory,
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                    ) {
                        Text(stringResource(R.string.view_history))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun homeScoreboardSection(
    scoreboard: Map<String, Int>,
    hasProgress: Boolean,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.scoreboard),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f),
        )
        if (!hasProgress) {
            Text(
                text = stringResource(R.string.home_scoreboard_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scoreboard.entries.sortedByDescending { it.value }.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = contentColor.copy(alpha = 0.1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.key, style = MaterialTheme.typography.bodyMedium, color = contentColor)
                            Text(
                                entry.value.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = contentColor.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun favoriteDecksSection(
    favorites: List<DeckEntity>,
    extra: Int,
    onDecks: () -> Unit,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_favorite_decks),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f),
        )
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.home_empty_favorites),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                favorites.forEach { deck ->
                    AssistChip(
                        onClick = onDecks,
                        label = { Text(deck.name) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = contentColor.copy(alpha = 0.08f),
                            labelColor = contentColor,
                            leadingIconContentColor = contentColor,
                        ),
                    )
                }
                if (extra > 0) {
                    AssistChip(
                        onClick = onDecks,
                        label = { Text("+$extra") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = contentColor.copy(alpha = 0.08f),
                            labelColor = contentColor,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun recentHighlightSection(
    text: String,
    icon: ImageVector?,
    iconTint: Color,
    contentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_recent_highlight),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor.copy(alpha = 0.85f),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = contentColor.copy(alpha = 0.08f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            }
        }
    }
}

@Composable
private fun homeLogo(size: Dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(0.6f),
            )
        }
    }
}

@Composable
private fun homeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java) }
    ElevatedCard(
        onClick = {
            vibrator?.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            onClick()
        },
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center,
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

private const val SCOREBOARD_TEAMS_KEY = "teams"
private const val SCOREBOARD_SCORES_KEY = "scores"

private val ScoreboardSaver: Saver<SnapshotStateMap<String, Int>, Bundle> = Saver(
    save = { state ->
        Bundle().apply {
            putStringArrayList(SCOREBOARD_TEAMS_KEY, ArrayList(state.keys))
            putIntegerArrayList(SCOREBOARD_SCORES_KEY, ArrayList(state.values))
        }
    },
    restore = { bundle ->
        val teams = bundle.getStringArrayList(SCOREBOARD_TEAMS_KEY).orEmpty()
        val scores = bundle.getIntegerArrayList(SCOREBOARD_SCORES_KEY).orEmpty()
        mutableStateMapOf<String, Int>().apply {
            teams.forEachIndexed { index, team ->
                this[team] = scores.getOrNull(index) ?: 0
            }
        }
    },
)
