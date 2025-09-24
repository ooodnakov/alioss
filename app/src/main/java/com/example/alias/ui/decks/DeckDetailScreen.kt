package com.example.alias.ui.decks

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alias.R
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.WordClassCount
import com.example.alias.domain.word.WordClassCatalog
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun deckDetailScreen(vm: DecksScreenViewModel, deck: DeckEntity) {
    var count by remember { mutableStateOf<Int?>(null) }
    var categories by remember { mutableStateOf<List<String>?>(null) }
    var wordClassCounts by remember { mutableStateOf<List<WordClassCount>?>(null) }
    var histogram by remember { mutableStateOf<List<DifficultyBucket>>(emptyList()) }
    var histogramLoading by remember { mutableStateOf(true) }
    var recentWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentWordsLoading by remember { mutableStateOf(true) }
    var wordExamples by remember { mutableStateOf<List<String>>(emptyList()) }
    var examplesLoading by remember { mutableStateOf(false) }
    var examplesError by remember { mutableStateOf(false) }
    var examplesRefreshKey by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deck.id) {
        wordClassCounts = null
        wordExamples = emptyList()
        examplesError = false
        examplesLoading = true
        launch { count = vm.getWordCount(deck.id) }
        launch { categories = runCatching { vm.getDeckCategories(deck.id) }.getOrElse { emptyList() } }
        launch { wordClassCounts = runCatching { vm.getDeckWordClassCounts(deck.id) }.getOrElse { emptyList() } }
        launch {
            histogramLoading = true
            try {
                histogram = runCatching { vm.getDeckDifficultyHistogram(deck.id) }.getOrElse { emptyList() }
            } finally {
                histogramLoading = false
            }
        }
        launch {
            recentWordsLoading = true
            try {
                recentWords = runCatching { vm.getDeckRecentWords(deck.id) }.getOrElse { emptyList() }
            } finally {
                recentWordsLoading = false
            }
        }
    }

    LaunchedEffect(deck.id, examplesRefreshKey) {
        examplesLoading = true
        examplesError = false
        val result = runCatching { vm.getDeckWordSamples(deck.id) }
        wordExamples = result.getOrDefault(emptyList())
        examplesError = result.isFailure
        examplesLoading = false
    }

    val downloadDateText = remember(deck.updatedAt) {
        val timestamp = deck.updatedAt
        if (timestamp <= 0L) {
            null
        } else {
            val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        deckDetailHero(deck = deck, count = count, downloadDateText = downloadDateText)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilledTonalButton(onClick = { confirmDelete = true }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.deck_delete_action))
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.deck_delete_dialog_title)) },
                text = { Text(stringResource(R.string.deck_delete_dialog_message, deck.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        vm.deleteDeck(deck)
                    }) {
                        Text(stringResource(R.string.deck_delete_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            val countText = count?.toString() ?: "…"
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.deck_word_count, countText), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.deck_version_label, deck.version))
                Text(
                    downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                        ?: stringResource(R.string.deck_downloaded_unknown),
                )
            }
        }

        detailCard(title = stringResource(R.string.deck_categories_title)) {
            when (val currentCategories = categories) {
                null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                else -> {
                    if (currentCategories.isEmpty()) {
                        Text(stringResource(R.string.deck_categories_empty))
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            currentCategories.forEach { category ->
                                AssistChip(onClick = {}, enabled = false, label = { Text(category) })
                            }
                        }
                    }
                }
            }
        }

        detailCard(title = stringResource(R.string.word_classes_label)) {
            when (val counts = wordClassCounts) {
                null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                else -> {
                    if (counts.isEmpty()) {
                        Text(stringResource(R.string.deck_word_classes_empty))
                    } else {
                        val totalTagged = counts.sumOf { it.count }
                        Text(
                            stringResource(
                                R.string.deck_word_classes_summary,
                                counts.size,
                                totalTagged,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            counts.forEach { entry ->
                                val label = when (entry.wordClass) {
                                    WordClassCatalog.NOUN -> stringResource(R.string.word_class_label_noun)
                                    WordClassCatalog.VERB -> stringResource(R.string.word_class_label_verb)
                                    WordClassCatalog.ADJECTIVE -> stringResource(R.string.word_class_label_adj)
                                    else -> entry.wordClass
                                }
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(stringResource(R.string.deck_word_classes_chip, label, entry.count))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        detailCard(title = stringResource(R.string.deck_difficulty_title)) {
            if (histogramLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                deckDifficultyHistogram(histogram, modifier = Modifier.fillMaxWidth())
            }
        }

        detailCard(title = stringResource(R.string.deck_recent_words_title)) {
            if (recentWordsLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                when {
                    recentWords.isEmpty() -> Text(stringResource(R.string.deck_recent_words_empty))

                    else -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recentWords.forEach { word ->
                                AssistChip(onClick = {}, enabled = false, label = { Text(word) })
                            }
                        }
                    }
                }
            }
        }

        detailCard(title = stringResource(R.string.deck_examples_title)) {
            when {
                examplesLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

                examplesError -> {
                    Text(
                        text = stringResource(R.string.deck_examples_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                wordExamples.isEmpty() -> Text(stringResource(R.string.deck_examples_empty))

                else -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        wordExamples.forEach { example ->
                            AssistChip(onClick = {}, enabled = false, label = { Text(example) })
                        }
                    }
                }
            }
            TextButton(
                onClick = { examplesRefreshKey++ },
                enabled = !examplesLoading,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.deck_examples_reload))
            }
        }
    }
}

@Composable
private fun detailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun deckDetailHero(deck: DeckEntity, count: Int?, downloadDateText: String?) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val coverImage = rememberDeckCoverImage(deck.coverImageBase64)
    val countText = count?.toString() ?: "…"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradient),
        )
        coverImage?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(deck.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deckTag(deck.language.uppercase(Locale.getDefault()))
                if (deck.isOfficial) {
                    deckTag(stringResource(R.string.deck_official_label))
                }
                if (deck.isNSFW) {
                    deckTag(stringResource(R.string.deck_nsfw_label))
                }
            }
            deck.author?.let { author ->
                Text(
                    text = stringResource(R.string.deck_author_label, author),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Text(
                text = stringResource(R.string.deck_word_count, countText),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Text(
                text = downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                    ?: stringResource(R.string.deck_downloaded_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun deckTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun deckDifficultyHistogram(
    buckets: List<DifficultyBucket>,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) {
        Text(stringResource(R.string.deck_difficulty_empty), modifier = modifier)
        return
    }

    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        buckets.forEach { bucket ->
            val fraction = bucket.count.toFloat() / maxCount.toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.word_difficulty_value, bucket.difficulty))
                    Text(bucket.count.toString(), style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
