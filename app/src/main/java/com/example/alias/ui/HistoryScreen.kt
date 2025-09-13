package com.example.alias.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.alias.data.db.TurnHistoryEntity
import com.example.alias.R

@Composable
fun HistoryScreen(history: List<TurnHistoryEntity>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_history))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(history) { entry ->
                ListItem(
                    headlineContent = { Text(entry.word) },
                    supportingContent = { Text(entry.team) },
                    trailingContent = {
                        Icon(
                            if (entry.correct) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (entry.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
