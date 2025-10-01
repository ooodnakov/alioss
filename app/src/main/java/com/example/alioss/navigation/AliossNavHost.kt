package com.example.alioss.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alioss.MainViewModel
import com.example.alioss.R
import com.example.alioss.data.achievements.AchievementSection
import com.example.alioss.data.achievements.AchievementId
import com.example.alioss.data.settings.Settings
import com.example.alioss.ui.achievements.achievementsScreen
import com.example.alioss.ui.about.aboutScreen
import com.example.alioss.ui.appScaffold
import com.example.alioss.ui.decks.deckDetailScreen
import com.example.alioss.ui.decks.decksScreen
import com.example.alioss.ui.game.gameScreen
import com.example.alioss.ui.historyScreen
import com.example.alioss.ui.home.HomeActions
import com.example.alioss.ui.home.HomeViewState
import com.example.alioss.ui.home.homeScreen
import com.example.alioss.ui.settings.settingsScreen

private const val HISTORY_LIMIT = 50

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun aliossNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    settings: Settings,
    viewModel: MainViewModel,
) {
    val achievements by viewModel.achievements.collectAsState()
    val context = LocalContext.current
    var seenUnlocked by remember { mutableStateOf<Set<AchievementId>?>(null) }

    LaunchedEffect(achievements) {
        val unlocked = achievements
            .filter { it.progress.isUnlocked }
            .map { it.definition.id }
            .toSet()
        val previous = seenUnlocked
        seenUnlocked = unlocked
        if (previous != null) {
            val newlyUnlocked = unlocked - previous
            if (newlyUnlocked.isNotEmpty()) {
                achievements
                    .filter { it.definition.id in newlyUnlocked }
                    .forEach { state ->
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.achievements_snackbar_unlocked,
                                state.definition.title,
                            ),
                            withDismissAction = true,
                        )
                    }
            }
        }
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val section = when (val route = destination.route) {
                "home" -> AchievementSection.HOME
                "game" -> AchievementSection.GAME
                "decks" -> AchievementSection.DECKS
                "deck/{id}" -> AchievementSection.DECKS
                "settings" -> AchievementSection.SETTINGS
                "history" -> AchievementSection.HISTORY
                "about" -> AchievementSection.ABOUT
                "achievements" -> AchievementSection.ACHIEVEMENTS
                else -> if (route != null && route.startsWith("deck/")) {
                    AchievementSection.DECKS
                } else {
                    null
                }
            }
            section?.let(viewModel::onSectionVisited)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { slideInHorizontally() },
        exitTransition = { fadeOut() },
    ) {
        composable("home") {
            val engine by viewModel.engine.collectAsState()
            val gameState = engine?.let { current ->
                val state by current.state.collectAsState()
                state
            }
            val decks by viewModel.decks.collectAsState()
            val recentHistoryFlow = remember { viewModel.recentHistory(12) }
            val recentHistory by recentHistoryFlow.collectAsState(initial = emptyList())
            appScaffold(snackbarHostState = snackbarHostState) {
                homeScreen(
                    state = HomeViewState(
                        gameState = gameState,
                        settings = settings,
                        decks = decks,
                        recentHistory = recentHistory,
                        achievements = achievements,
                    ),
                    actions = HomeActions(
                        onResumeMatch = { navController.navigate("game") },
                        onStartNewMatch = {
                            viewModel.restartMatch()
                            navController.navigate("game")
                        },
                        onDecks = { navController.navigate("decks") },
                        onSettings = { navController.navigate("settings") },
                        onHistory = { navController.navigate("history") },
                        onAchievements = { navController.navigate("achievements") },
                    ),
                )
            }
        }
        composable("game") {
            val engine by viewModel.engine.collectAsState()
            val currentSettings by viewModel.settings.collectAsState()
            appScaffold(snackbarHostState = snackbarHostState) {
                engine?.let { gameEngine ->
                    gameScreen(
                        vm = viewModel,
                        engine = gameEngine,
                        settings = currentSettings,
                        onNavigateHome = { navController.popBackStack() },
                    )
                } ?: Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        composable("decks") {
            appScaffold(snackbarHostState = snackbarHostState) {
                decksScreen(
                    vm = viewModel,
                    onDeckSelected = { navController.navigate("deck/${it.id}") },
                )
            }
        }
        composable(
            route = "deck/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = requireNotNull(backStackEntry.arguments?.getString("id"))
            val decks by viewModel.decks.collectAsState()
            val deck = decks.find { it.id == id }
            if (deck == null) {
                appScaffold(
                    snackbarHostState = snackbarHostState,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.deck_not_found))
                    }
                }
            } else {
                appScaffold(
                    snackbarHostState = snackbarHostState,
                ) {
                    deckDetailScreen(vm = viewModel, deck = deck)
                }
            }
        }
        composable("settings") {
            appScaffold(snackbarHostState = snackbarHostState) {
                settingsScreen(
                    vm = viewModel,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate("about") },
                )
            }
        }
        composable("achievements") {
            appScaffold(snackbarHostState = snackbarHostState) {
                achievementsScreen(
                    achievements = achievements,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable("history") {
            appScaffold(snackbarHostState = snackbarHostState) {
                val historyFlow = remember { viewModel.recentHistory(HISTORY_LIMIT) }
                val history by historyFlow.collectAsState(initial = emptyList())
                historyScreen(
                    history = history,
                    onResetHistory = { viewModel.resetHistory() },
                )
            }
        }
        composable("about") {
            appScaffold(snackbarHostState = snackbarHostState) {
                aboutScreen()
            }
        }
    }
}
