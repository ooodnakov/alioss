package com.example.alias.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppScaffold(
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = {
            snackbarHostState?.let { state ->
                SnackbarHost(hostState = state) { data ->
                    val msg = data.visuals.message
                    val isError = msg.startsWith("Failed", ignoreCase = true) || msg.contains("error", ignoreCase = true)
                    val container = if (isError) MaterialTheme.colorScheme.errorContainer else SnackbarDefaults.color
                    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.contentColor
                    Snackbar(snackbarData = data, containerColor = container, contentColor = content)
                }
            }
        },
    ) { scaffoldPadding ->
        Box(Modifier.padding(scaffoldPadding)) { content() }
    }
}
