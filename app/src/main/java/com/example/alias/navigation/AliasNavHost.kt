package com.example.alias.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
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
import com.example.alias.ui.AppScaffold
import com.example.alias.ui.HistoryScreen
import com.example.alias.ui.about.AboutScreen
import com.example.alias.ui.decks.DeckDetailScreen
import com.example.alias.ui.decks.DecksScreen
import com.example.alias.ui.game.GameScreen
import com.example.alias.ui.home.HomeScreen
import com.example.alias.ui.settings.SettingsScreen
import com.google.accompanist.placeholder.material3.placeholder

private const val HISTORY_LIMIT = 50

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AliasNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    settings: Settings,
    viewModel: MainViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { slideInHorizontally() },
        exitTransition = { fadeOut() }
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
            AppScaffold(snackbarHostState = snackbarHostState) {
                HomeScreen(
                    gameState = gameState,
                    settings = settings,
                    decks = decks,
                    recentHistory = recentHistory,
                    onResumeMatch = { navController.navigate("game") },
                    onStartNewMatch = {
                        viewModel.restartMatch()
                        navController.navigate("game")
                    },
                    onDecks = { navController.navigate("decks") },
                    onSettings = { navController.navigate("settings") },
                    onHistory = { navController.navigate("history") }
                )
            }
        }
        composable("game") {
            val engine by viewModel.engine.collectAsState()
            val currentSettings by viewModel.settings.collectAsState()
            AppScaffold(snackbarHostState = snackbarHostState) {
                if (engine == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {}
                } else {
                    GameScreen(viewModel, engine!!, currentSettings)
                }
            }
        }
        composable("decks") {
            AppScaffold(snackbarHostState = snackbarHostState) {
                DecksScreen(
                    vm = viewModel,
                    onDeckSelected = { navController.navigate("deck/${it.id}") }
                )
            }
        }
        composable(
            route = "deck/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = requireNotNull(backStackEntry.arguments?.getString("id"))
            val decks by viewModel.decks.collectAsState()
            val deck = decks.find { it.id == id }
            if (deck == null) {
                AppScaffold(
                    snackbarHostState = snackbarHostState
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.deck_not_found))
                    }
                }
            } else {
                AppScaffold(
                    snackbarHostState = snackbarHostState
                ) {
                    DeckDetailScreen(vm = viewModel, deck = deck)
                }
            }
        }
        composable("settings") {
            AppScaffold(snackbarHostState = snackbarHostState) {
                SettingsScreen(
                    vm = viewModel,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate("about") }
                )
            }
        }
        composable("history") {
            AppScaffold(snackbarHostState = snackbarHostState) {
                val historyFlow = remember { viewModel.recentHistory(HISTORY_LIMIT) }
                val history by historyFlow.collectAsState(initial = emptyList())
                HistoryScreen(history)
            }
        }
        composable("about") {
            AppScaffold(snackbarHostState = snackbarHostState) {
                AboutScreen()
            }
        }
    }
}
