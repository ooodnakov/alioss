package com.example.alias.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import com.example.alias.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
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
        topBar = {
            if (title.isNotBlank()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = ""
                                )
                            }
                        }
                    },
                    actions = { actions?.invoke() }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) { content() }
    }
}
