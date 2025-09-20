package com.example.alias

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.pack.PackParser
import com.example.alias.data.pack.ParsedPack
import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.domain.word.WordClassCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class DeckManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepository: DeckRepository,
    private val wordDao: WordDao,
    private val deckDao: DeckDao,
    private val turnHistoryDao: TurnHistoryDao,
    private val settingsRepository: SettingsRepository,
    private val downloader: PackDownloader,
) {
    data class InitialLoadResult(
        val words: List<String>,
        val settings: Settings,
    )

    data class WordQueryFilters(
        val deckIds: List<String>,
        val language: String,
        val allowNSFW: Boolean,
        val minDifficulty: Int,
        val maxDifficulty: Int,
        val categories: List<String>,
        val categoryFilterEnabled: Int,
        val wordClasses: List<String>,
        val wordClassFilterEnabled: Int,
    )

    data class PackImportResult(
        val deckId: String,
        val language: String,
        val coverImageError: Throwable?,
    )

    sealed class DeleteDeckResult {
        data class Success(val message: String) : DeleteDeckResult()
        data class Failure(val errorMessage: String) : DeleteDeckResult()
    }

    data class WordClassAvailabilityKey(
        val deckIds: Set<String>,
        val language: String,
        val allowNSFW: Boolean,
    )

    fun observeDecks(): Flow<List<DeckEntity>> {
        return deckRepository.getDecks()
            .combine(settingsRepository.settings.map { it.deletedBundledDeckIds }) { allDecks, deletedIds ->
                val filtered = allDecks.filter { deck -> !deletedIds.contains(deck.id) }
                Log.d(TAG, "Filtered decks: ${filtered.size} (removed ${allDecks.size - filtered.size})")
                filtered
            }
    }

    fun observeEnabledDeckIds(): Flow<Set<String>> = settingsRepository.settings.map { it.enabledDeckIds }

    fun observeTrustedSources(): Flow<Set<String>> = settingsRepository.settings.map { it.trustedSources }

    fun observeSettings(): Flow<Settings> = settingsRepository.settings

    suspend fun prepareInitialLoad(): InitialLoadResult {
        return withContext(Dispatchers.IO) {
            val assetFiles = context.assets.list("decks")?.filter { it.endsWith(".json") } ?: emptyList()
            val previousHashes = settingsRepository.readBundledDeckHashes()
            val digest = MessageDigest.getInstance("SHA-256")
            fun sha256(bytes: ByteArray): String = digest.digest(bytes).joinToString("") { b -> "%02x".format(b) }

            val assetContents = mutableMapOf<String, String>()
            val currentDeckEntries = mutableSetOf<String>()
            val toImport = mutableListOf<String>()
            val currentBundledDeckIds = mutableSetOf<String>()
            val deletedBundledDeckIds = settingsRepository.readDeletedBundledDeckIds()

            assetFiles.forEach { file ->
                val content = runCatching {
                    context.assets.open("decks/$file").bufferedReader().use { it.readText() }
                }.getOrNull()
                if (content != null) {
                    assetContents[file] = content
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    val fileHash = sha256(bytes)
                    val deckIds = parseDeckIdsFromContent(content)
                    currentBundledDeckIds += deckIds

                    deckIds.forEach { deckId ->
                        currentDeckEntries += "$deckId:$fileHash"
                    }
                    currentDeckEntries += "$file:$fileHash"

                    val fileNeedsImport = previousHashes.none { it.startsWith("$file:") } || !previousHashes.contains("$file:$fileHash")
                    val decksNeedImport = deckIds.any { deckId ->
                        val deckEntry = "$deckId:$fileHash"
                        !deletedBundledDeckIds.contains(deckId) &&
                            (previousHashes.none { it.startsWith("$deckId:") } || !previousHashes.contains(deckEntry))
                    }
                    if (fileNeedsImport || decksNeedImport) {
                        toImport += file
                    }
                } else {
                    Log.e(TAG, "Failed to read bundled deck $file")
                }
            }

            val allDecks = deckRepository.getDecks().first()
            val decksToPrune = allDecks.filter { deck ->
                deck.isOfficial && currentBundledDeckIds.contains(deck.id) && !deletedBundledDeckIds.contains(deck.id)
            }
            decksToPrune.forEach { deck ->
                runCatching { deckRepository.deleteDeck(deck.id) }
                    .onFailure { Log.e(TAG, "Failed to prune deck ${deck.id}", it) }
            }

            if (previousHashes.isEmpty() && currentDeckEntries.isNotEmpty()) {
                toImport.clear()
                toImport.addAll(assetFiles)
            }
            val hadDecks = allDecks.isNotEmpty()
            if (!hadDecks && assetFiles.isNotEmpty()) {
                toImport.clear()
                toImport.addAll(assetFiles)
            }

            toImport.forEach { file ->
                runCatching {
                    val content = assetContents[file]
                        ?: context.assets.open("decks/$file").bufferedReader().use { it.readText() }
                    val sanitized = parseAndSanitizePack(content, isBundledAsset = true)
                    deckRepository.importPack(sanitized.pack)
                    sanitized.coverImageError?.let {
                        Log.w(TAG, "Bundled deck $file cover art discarded", it)
                    }
                }.onFailure { Log.e(TAG, "Failed to import bundled deck $file", it) }
            }

            runCatching { settingsRepository.writeBundledDeckHashes(currentDeckEntries) }
                .onFailure { Log.e(TAG, "Failed to persist bundled deck hashes", it) }

            val baseSettings = settingsRepository.settings.first()
            val allDecksAfterImport = deckRepository.getDecks().first()
            val availableDecks = allDecksAfterImport.filter { !deletedBundledDeckIds.contains(it.id) }
            val preferredIds = availableDecks
                .filter { it.language == baseSettings.languagePreference }
                .map { it.id }
                .toSet()
            val fallbackIds = availableDecks.map { it.id }.toSet()
            val resolvedEnabled = if (baseSettings.enabledDeckIds.isEmpty()) {
                if (preferredIds.isNotEmpty()) preferredIds else fallbackIds
            } else {
                baseSettings.enabledDeckIds.filterNot { deletedBundledDeckIds.contains(it) }.toSet()
            }

            if (baseSettings.enabledDeckIds.isEmpty()) {
                runCatching { settingsRepository.setEnabledDeckIds(resolvedEnabled) }
                    .onFailure { Log.e(TAG, "Failed to persist enabled deck ids", it) }
            }

            val filters = buildWordQueryFilters(baseSettings, resolvedEnabled)
            val words = loadWords(filters)

            InitialLoadResult(
                words = words,
                settings = baseSettings.copy(enabledDeckIds = resolvedEnabled),
            )
        }
    }

    fun buildWordQueryFilters(settings: Settings, deckIdsOverride: Set<String>? = null): WordQueryFilters {
        val deckIds = (deckIdsOverride ?: settings.enabledDeckIds).toList()
        val categories = settings.selectedCategories.toList()
        val classes = canonicalizeWordClassFilters(settings.selectedWordClasses)
        return WordQueryFilters(
            deckIds = deckIds,
            language = settings.languagePreference,
            allowNSFW = settings.allowNSFW,
            minDifficulty = settings.minDifficulty,
            maxDifficulty = settings.maxDifficulty,
            categories = categories,
            categoryFilterEnabled = if (categories.isEmpty()) 0 else 1,
            wordClasses = classes,
            wordClassFilterEnabled = if (classes.isEmpty()) 0 else 1,
        )
    }

    suspend fun loadWords(filters: WordQueryFilters): List<String> {
        if (filters.deckIds.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            wordDao.getWordTextsForDecks(
                filters.deckIds,
                filters.language,
                filters.allowNSFW,
                filters.minDifficulty,
                filters.maxDifficulty,
                filters.categories,
                filters.categoryFilterEnabled,
                filters.wordClasses,
                filters.wordClassFilterEnabled,
            )
        }
    }

    suspend fun loadWordMetadata(filters: WordQueryFilters): WordMetadata {
        if (filters.deckIds.isEmpty()) {
            return WordMetadata(emptyMap(), emptyList(), emptyList())
        }
        return withContext(Dispatchers.IO) {
            coroutineScope {
                val briefsDeferred = async {
                    wordDao.getWordBriefsForDecks(
                        filters.deckIds,
                        filters.language,
                        filters.allowNSFW,
                        filters.minDifficulty,
                        filters.maxDifficulty,
                        filters.categories,
                        filters.categoryFilterEnabled,
                        filters.wordClasses,
                        filters.wordClassFilterEnabled,
                    )
                }
                val categoriesDeferred = async {
                    wordDao.getAvailableCategories(filters.deckIds, filters.language, filters.allowNSFW)
                }
                val classesDeferred = async {
                    wordDao.getAvailableWordClasses(filters.deckIds, filters.language, filters.allowNSFW)
                }
                val briefs = briefsDeferred.await()
                val categories = categoriesDeferred.await().sorted()
                val classes = classesDeferred.await()
                val infoByWord = briefs.associateBy({ it.text }) { brief ->
                    WordInfo(brief.difficulty, brief.category, parsePrimaryWordClass(brief.wordClass))
                }
                WordMetadata(
                    infoByWord = infoByWord,
                    categories = categories,
                    wordClasses = canonicalizeWordClassFilters(classes),
                )
            }
        }
    }

    fun canonicalizeWordClassFilters(raw: Collection<String>): List<String> {
        val normalized = raw
            .asSequence()
            .mapNotNull { value ->
                val trimmed = value.trim()
                if (trimmed.isEmpty()) {
                    null
                } else {
                    trimmed.uppercase(Locale.ROOT)
                }
            }
            .toList()
        if (normalized.isEmpty()) return emptyList()
        val orderedKnown = WordClassCatalog.order(normalized)
        val knownSet = orderedKnown.toSet()
        val extras = normalized
            .asSequence()
            .filterNot { knownSet.contains(it) }
            .distinct()
            .sorted()
            .toList()
        return orderedKnown + extras
    }

    fun parsePrimaryWordClass(raw: String?): String? {
        return raw
            ?.split(',')
            ?.asSequence()
            ?.mapNotNull { WordClassCatalog.normalizeOrNull(it) }
            ?.firstOrNull()
    }

    suspend fun downloadPack(
        url: String,
        expectedSha256: String?,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit,
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            downloader.download(
                url.trim(),
                expectedSha256?.trim().takeUnless { it.isNullOrEmpty() },
            ) { bytesRead, totalBytes ->
                onProgress(bytesRead, totalBytes)
            }
        }
    }

    suspend fun importDeckFromUri(uri: Uri): PackImportResult {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Empty file")
        }
        return importPackFromJson(text)
    }

    suspend fun importPackFromJson(content: String, isBundledAsset: Boolean = false): PackImportResult {
        val sanitized = parseAndSanitizePack(content, isBundledAsset)
        withContext(Dispatchers.IO) { deckRepository.importPack(sanitized.pack) }
        return PackImportResult(
            deckId = sanitized.pack.deck.id,
            language = sanitized.pack.deck.language,
            coverImageError = sanitized.coverImageError,
        )
    }

    suspend fun setDeckEnabled(id: String, enabled: Boolean) {
        val current = settingsRepository.settings.first().enabledDeckIds.toMutableSet()
        if (enabled) current += id else current -= id
        settingsRepository.setEnabledDeckIds(current)
    }

    suspend fun setAllDecksEnabled(enableAll: Boolean) {
        val all = deckRepository.getDecks().first().map { it.id }.toSet()
        val target = if (enableAll) all else emptySet()
        settingsRepository.setEnabledDeckIds(target)
    }

    suspend fun deleteDeck(deck: DeckEntity): DeleteDeckResult {
        val settingsSnapshot = settingsRepository.settings.first()
        val updatedIds = settingsSnapshot.enabledDeckIds - deck.id
        val isBundledDeck = deck.isOfficial
        val result = runCatching {
            withContext(Dispatchers.IO) {
                if (isBundledDeck) {
                    settingsRepository.addDeletedBundledDeckId(deck.id)
                    settingsRepository.setEnabledDeckIds(updatedIds)
                } else {
                    deckRepository.deleteDeck(deck.id)
                }
            }
        }
        if (result.isFailure) {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            return DeleteDeckResult.Failure(error)
        }
        if (updatedIds != settingsSnapshot.enabledDeckIds) {
            settingsRepository.setEnabledDeckIds(updatedIds)
        }
        val message = if (isBundledDeck) {
            "Hidden deck: ${deck.name}"
        } else {
            "Deleted deck: ${deck.name}"
        }
        return DeleteDeckResult.Success(message)
    }

    suspend fun permanentlyDeleteImportedDeck(deck: DeckEntity): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) { deckRepository.deleteDeck(deck.id) }
        }
    }

    suspend fun restoreDeletedBundledDeck(deckId: String): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                settingsRepository.removeDeletedBundledDeckId(deckId)
            }
        }
    }

    suspend fun getWordCount(deckId: String): Int = deckRepository.getWordCount(deckId)

    suspend fun getDeckCategories(deckId: String): List<String> = withContext(Dispatchers.IO) { wordDao.getDeckCategories(deckId) }

    suspend fun getDeckWordSamples(deckId: String, limit: Int = 5): List<String> =
        withContext(Dispatchers.IO) { wordDao.getRandomWordSamples(deckId, limit) }

    suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket> =
        withContext(Dispatchers.IO) { deckRepository.getDifficultyHistogram(deckId) }

    suspend fun getDeckRecentWords(deckId: String, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) { deckRepository.getRecentWords(deckId, limit) }

    suspend fun getDeckWordClassCounts(deckId: String): List<WordClassCount> =
        withContext(Dispatchers.IO) {
            val counts = wordDao.getWordClassCounts(deckId)
            val knownOrder = WordClassCatalog.allowed.withIndex().associate { it.value to it.index }
            counts.sortedWith(
                compareBy(
                    { knownOrder[it.wordClass] ?: Int.MAX_VALUE },
                    { it.wordClass },
                ),
            )
        }

    suspend fun resetLocalData() {
        withContext(Dispatchers.IO) {
            turnHistoryDao.deleteAll()
            deckDao.deleteAll()
            settingsRepository.clearAll()
        }
    }

    fun buildWordClassAvailabilityKey(settings: Settings): WordClassAvailabilityKey =
        WordClassAvailabilityKey(
            deckIds = settings.enabledDeckIds,
            language = settings.languagePreference,
            allowNSFW = settings.allowNSFW,
        )

    suspend fun loadAvailableWordClasses(key: WordClassAvailabilityKey): List<String> {
        val ids = key.deckIds.toList()
        if (ids.isEmpty()) return emptyList()
        val classes = withContext(Dispatchers.IO) {
            wordDao.getAvailableWordClasses(ids, key.language, key.allowNSFW)
        }
        return canonicalizeWordClassFilters(classes)
    }

    private data class SanitizedPack(
        val pack: ParsedPack,
        val coverImageError: Throwable?,
    )

    private fun parseDeckIdsFromContent(content: String): List<String> {
        return try {
            val root = JSONObject(content)
            val deck = root.optJSONObject("deck")
            if (deck != null && deck.has("id")) {
                listOf(deck.getString("id"))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse deck IDs from content", e)
            emptyList()
        }
    }

    private suspend fun sanitizeCoverImage(pack: ParsedPack): SanitizedPack {
        val coverError = withContext(Dispatchers.IO) {
            pack.deck.coverImageBase64?.let { encoded ->
                try {
                    val decoded = Base64.decode(encoded, Base64.DEFAULT)
                    require(decoded.isNotEmpty()) { "Cover image is empty" }
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size, opts)
                    require(opts.outWidth > 0 && opts.outHeight > 0) { "Cover image has invalid dimensions" }
                    null
                } catch (c: CancellationException) {
                    throw c
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to decode deck cover image for ${pack.deck.id}", t)
                    t
                }
            }
        }
        val sanitized = if (coverError != null) {
            pack.copy(deck = pack.deck.copy(coverImageBase64 = null))
        } else {
            pack
        }
        return SanitizedPack(sanitized, coverError)
    }

    private suspend fun parseAndSanitizePack(content: String, isBundledAsset: Boolean = false): SanitizedPack {
        return try {
            val parsed = PackParser.fromJson(content, isBundledAsset)
            sanitizeCoverImage(parsed)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (!error.isCoverImageError()) throw error
            val sanitizedJson = removeCoverImageField(content) ?: throw error
            val parsed = PackParser.fromJson(sanitizedJson, isBundledAsset)
            val sanitized = sanitizeCoverImage(parsed)
            sanitized.copy(coverImageError = sanitized.coverImageError ?: error)
        }
    }

    private fun removeCoverImageField(content: String): String? {
        return try {
            val root = JSONObject(content)
            val deck = root.optJSONObject("deck") ?: return null
            if (!deck.has("coverImage")) {
                null
            } else {
                deck.remove("coverImage")
                root.toString()
            }
        } catch (error: Exception) {
            null
        }
    }

    private fun Throwable.isCoverImageError(): Boolean {
        if (this is IllegalArgumentException && message?.contains("cover image", ignoreCase = true) == true) {
            return true
        }
        val cause = cause
        return cause != null && cause !== this && cause.isCoverImageError()
    }

    companion object {
        private const val TAG = "DeckManager"
    }
}
