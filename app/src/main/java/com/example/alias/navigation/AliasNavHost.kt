package com.example.alias.navigation

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.settings.Settings
import com.example.alias.ui.about.aboutScreen
import com.example.alias.ui.appScaffold
import com.example.alias.ui.decks.deckDetailScreen
import com.example.alias.ui.decks.decksScreen
import com.example.alias.ui.game.gameScreen
import com.example.alias.ui.historyScreen
import com.example.alias.ui.home.HomeActions
import com.example.alias.ui.home.HomeViewState
import com.example.alias.ui.home.homeScreen
import com.example.alias.ui.settings.settingsScreen

private const val HISTORY_LIMIT = 50

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun aliasNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    settings: Settings,
    viewModel: MainViewModel,
) {
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
                    ),
                )
            }
        }
        composable("game") {
            val engine by viewModel.engine.collectAsState()
            val currentSettings by viewModel.settings.collectAsState()
            appScaffold(snackbarHostState = snackbarHostState) {
                engine?.let { gameEngine ->
                    gameScreen(viewModel, gameEngine, currentSettings)
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
