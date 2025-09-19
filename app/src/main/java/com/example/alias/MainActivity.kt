package com.example.alias

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.alias.MainViewModel.UiEvent
import com.example.alias.navigation.AliasNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            AliasAppTheme {
                val nav = rememberNavController()
                val vm: MainViewModel = hiltViewModel()
                val snack = remember { SnackbarHostState() }
                val settings by vm.settings.collectAsState()

                LaunchedEffect(settings.uiLanguage) {
                    applyLocalePreference(settings.uiLanguage)
                }

                LaunchedEffect(Unit) {
                    var currentSnackbarJob: Job? = null
                    vm.uiEvents.collect { ev: UiEvent ->
                        if (ev.dismissCurrent) {
                            snack.currentSnackbarData?.dismiss()
                            currentSnackbarJob?.cancelAndJoin()
                            currentSnackbarJob = null
                        }
                        if (ev.message.isBlank()) {
                            return@collect
                        }
                        val duration = if (ev.actionLabel != null && ev.duration == SnackbarDuration.Short) {
                            SnackbarDuration.Long
                        } else {
                            ev.duration
                        }
                        val job = launch {
                            val result = snack.showSnackbar(
                                message = ev.message,
                                actionLabel = ev.actionLabel,
                                withDismissAction = ev.actionLabel == null,
                                duration = duration
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                ev.onAction?.invoke()
                            }
                        }
                        currentSnackbarJob = job
                        job.invokeOnCompletion {
                            if (currentSnackbarJob === job) {
                                currentSnackbarJob = null
                            }
                        }
                    }
                }

                AliasNavHost(
                    navController = nav,
                    snackbarHostState = snack,
                    settings = settings,
                    viewModel = vm
                )
            }
        }
    }
}
