package com.example.alias.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.alias.MainViewModel
import com.example.alias.R
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.WordClassCount
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckDetailScreen(vm: MainViewModel, deck: DeckEntity) {
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
    val scope = rememberCoroutineScope()

    suspend fun refreshExamples() {
        examplesLoading = true
        examplesError = false
        val result = runCatching { vm.getDeckWordSamples(deck.id) }
        wordExamples = result.getOrDefault(emptyList())
        examplesError = result.isFailure
        examplesLoading = false
    }

    LaunchedEffect(deck.id) {
        wordClassCounts = null
        scope.launch { count = vm.getWordCount(deck.id) }
        scope.launch { categories = runCatching { vm.getDeckCategories(deck.id) }.getOrElse { emptyList() } }
        scope.launch { wordClassCounts = runCatching { vm.getDeckWordClassCounts(deck.id) }.getOrElse { emptyList() } }
        scope.launch {
            histogramLoading = true
            try {
                histogram = runCatching { vm.getDeckDifficultyHistogram(deck.id) }.getOrElse { emptyList() }
            } finally {
                histogramLoading = false
            }
        }
        scope.launch {
            recentWordsLoading = true
            try {
                recentWords = runCatching { vm.getDeckRecentWords(deck.id) }.getOrElse { emptyList() }
            } finally {
                recentWordsLoading = false
            }
        }
        scope.launch { refreshExamples() }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeckDetailHero(deck = deck, count = count, downloadDateText = downloadDateText)

        ElevatedCard(Modifier.fillMaxWidth()) {
            val countText = count?.toString() ?: "…"
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.deck_word_count, countText), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.deck_version_label, deck.version))
                Text(
                    downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                        ?: stringResource(R.string.deck_downloaded_unknown)
                )
            }
        }

        DetailCard(title = stringResource(R.string.deck_categories_title)) {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategories.forEach { category ->
                                AssistChip(onClick = {}, enabled = false, label = { Text(category) })
                            }
                        }
                    }
                }
            }
        }

        DetailCard(title = stringResource(R.string.word_classes_label)) {
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
                                totalTagged
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            counts.forEach { entry ->
                                val label = stringResource(
                                    R.string.deck_word_class_entry,
                                    entry.wordClass,
                                    entry.count
                                )
                                AssistChip(onClick = {}, enabled = false, label = { Text(label) })
                            }
                        }
                    }
                }
            }
        }

        DetailCard(title = stringResource(R.string.deck_difficulty_title)) {
            if (histogramLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                DeckDifficultyHistogram(histogram, modifier = Modifier.fillMaxWidth())
            }
        }

        DetailCard(title = stringResource(R.string.deck_recent_words_title)) {
            if (recentWordsLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                if (recentWords.isEmpty()) {
                    Text(stringResource(R.string.deck_recent_words_empty))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        recentWords.forEach { word ->
                            Text(word, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        DetailCard(title = stringResource(R.string.deck_word_examples_title)) {
            when {
                examplesLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                examplesError -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.deck_examples_error))
                        TextButton(onClick = { scope.launch { refreshExamples() } }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                wordExamples.isEmpty() -> Text(stringResource(R.string.deck_examples_empty))
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        wordExamples.forEach { example ->
                            Text(example, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckDetailHero(deck: DeckEntity, count: Int?, downloadDateText: String?) {
    val gradient = rememberDeckCoverBrush(deck.id)
    val countText = count?.toString() ?: "…"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(deck.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeckTag(deck.language.uppercase(Locale.getDefault()))
            if (deck.isOfficial) {
                DeckTag(stringResource(R.string.deck_official_label))
            }
            if (deck.isNSFW) {
                DeckTag(stringResource(R.string.deck_nsfw_label))
            }
        }
        Text(
            text = stringResource(R.string.deck_word_count, countText),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Text(
            text = downloadDateText?.let { stringResource(R.string.deck_downloaded_label, it) }
                ?: stringResource(R.string.deck_downloaded_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun DeckTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeckDifficultyHistogram(
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.word_difficulty_value, bucket.difficulty))
                    Text(bucket.count.toString(), style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
