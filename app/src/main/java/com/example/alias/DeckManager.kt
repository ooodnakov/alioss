package com.example.alias

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.alias.data.DeckRepository
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.DeckEntity
import com.example.alias.data.db.DifficultyBucket
import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.WordClassCount
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.pack.CoverImageException
import com.example.alias.data.pack.PackParser
import com.example.alias.data.pack.PackValidator
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.text.Charsets

@Singleton
class DeckManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val deckRepository: DeckRepository,
        private val wordDao: WordDao,
        private val deckDao: DeckDao,
        private val turnHistoryDao: TurnHistoryDao,
        private val settingsRepository: SettingsRepository,
        private val downloader: PackDownloader,
        private val bundledDeckProvider: BundledDeckProvider,
        private val logger: DeckManagerLogger,
    ) {
        data class InitialLoadResult(
            val words: List<String>,
            val settings: Settings,
        )

        data class WordQueryFilters(
            val deckIds: List<String>,
            val allowNSFW: Boolean,
            val minDifficulty: Int,
            val maxDifficulty: Int,
            val categories: List<String>?,
            val categoryFilterEnabled: Int,
            val wordClasses: List<String>?,
            val wordClassFilterEnabled: Int,
            val languages: List<String>,
            val languageFilterEnabled: Int,
        )

        data class PackImportResult(
            val deckId: String,
            val language: String,
            val isNsfw: Boolean,
            val coverImageError: Throwable?,
        )

        sealed class DeleteDeckResult {
            data class Success(val message: String) : DeleteDeckResult()
            data class Failure(val errorMessage: String) : DeleteDeckResult()
        }

        private data class BundledDeckScanResult(
            val assetContents: Map<String, String>,
            val toImport: List<String>,
            val currentDeckEntries: Set<String>,
            val currentBundledDeckIds: Set<String>,
        )

        private fun determineBundledDecksToImport(
            assetFiles: List<String>,
            previousHashes: Set<String>,
            deletedBundledDeckIds: Set<String>,
            hadDecks: Boolean,
        ): BundledDeckScanResult {
            val digest = MessageDigest.getInstance("SHA-256")
            val assetContents = mutableMapOf<String, String>()
            val currentDeckEntries = mutableSetOf<String>()
            val currentBundledDeckIds = mutableSetOf<String>()
            val toImport = mutableListOf<String>()
            val deckIdsByFile = mutableMapOf<String, List<String>>()

            assetFiles.forEach { file ->
                val content = bundledDeckProvider.readDeckAsset(file)
                if (content != null) {
                    assetContents[file] = content
                    val bytes = content.toByteArray(Charsets.UTF_8)
                    val fileHash = digest.digest(bytes).joinToString("") { b -> "%02x".format(b) }
                    digest.reset()
                    val deckIds = parseDeckIdsFromContent(content)
                    deckIdsByFile[file] = deckIds
                    currentBundledDeckIds += deckIds

                    deckIds.forEach { deckId ->
                        currentDeckEntries += "$deckId:$fileHash"
                    }
                    currentDeckEntries += "$file:$fileHash"

                    val hasRestorableDeck = deckIds.any { deckId -> !deletedBundledDeckIds.contains(deckId) }
                    if (!hasRestorableDeck) {
                        return@forEach
                    }

                    val fileNeedsImport = previousHashes.none { it.startsWith("$file:") } || !previousHashes.contains("$file:$fileHash")
                    val decksNeedImport = deckIds.any { deckId ->
                        if (deletedBundledDeckIds.contains(deckId)) {
                            false
                        } else {
                            val deckEntry = "$deckId:$fileHash"
                            previousHashes.none { it.startsWith("$deckId:") } || !previousHashes.contains(deckEntry)
                        }
                    }
                    if (fileNeedsImport || decksNeedImport) {
                        toImport += file
                    }
                } else {
                    logger.error("Failed to read bundled deck $file")
                }
            }

            val restorableFiles = assetFiles.filter { file ->
                val deckIds = deckIdsByFile[file].orEmpty()
                deckIds.any { deckId -> !deletedBundledDeckIds.contains(deckId) }
            }
            if ((previousHashes.isEmpty() || !hadDecks) && restorableFiles.isNotEmpty()) {
                toImport.clear()
                toImport.addAll(restorableFiles)
            }

            return BundledDeckScanResult(
                assetContents = assetContents,
                toImport = toImport,
                currentDeckEntries = currentDeckEntries,
                currentBundledDeckIds = currentBundledDeckIds,
            )
        }

        private suspend fun pruneOrphanedBundledDecks(
            existingDecks: List<DeckEntity>,
            currentBundledDeckIds: Set<String>,
            deletedBundledDeckIds: Set<String>,
            previousBundledDeckIds: Set<String>,
        ) {
            existingDecks
                .asSequence()
                .filter { it.isOfficial }
                .filter { previousBundledDeckIds.contains(it.id) }
                .filterNot { currentBundledDeckIds.contains(it.id) }
                .filterNot { deletedBundledDeckIds.contains(it.id) }
                .forEach { deck ->
                    runCatching { deckRepository.deleteDeck(deck.id) }
                        .onFailure { logger.error("Failed to prune deck ${deck.id}", it) }
                }
        }

        private suspend fun importBundledDecks(filesToImport: List<String>, assetContents: Map<String, String>) {
            filesToImport.forEach { file ->
                runCatching {
                    val content = assetContents[file] ?: bundledDeckProvider.readDeckAsset(file)
                    if (content == null) {
                        logger.error("Missing bundled deck content for $file")
                        return@forEach
                    }
                    val sanitized = parseAndSanitizePack(content, isBundledAsset = true)
                    deckRepository.importPack(sanitized.pack)
                    sanitized.coverImageError?.let {
                        logger.warn("Bundled deck $file cover art discarded", it)
                    }
                }.onFailure { logger.error("Failed to import bundled deck $file", it) }
            }
        }

        private suspend fun persistBundledDeckHashes(entries: Set<String>) {
            runCatching { settingsRepository.writeBundledDeckHashes(entries) }
                .onFailure { logger.error("Failed to persist bundled deck hashes", it) }
        }

        private fun resolveInitialEnabledDecks(
            baseSettings: Settings,
            availableDecks: List<DeckEntity>,
            deletedBundledDeckIds: Set<String>,
        ): Set<String> {
            val availableIds = availableDecks.map { it.id }.toSet()
            val retainedIds = baseSettings.enabledDeckIds
                .filterNot { deletedBundledDeckIds.contains(it) }
                .filter { availableIds.contains(it) }
                .toSet()
            return when {
                baseSettings.enabledDeckIds.isEmpty() -> availableIds
                retainedIds.isEmpty() -> availableIds
                else -> retainedIds
            }
        }

        data class WordClassAvailabilityKey(
            val deckIds: Set<String>,
            val allowNSFW: Boolean,
            val languages: Set<String>,
        )

        fun observeDecks(): Flow<List<DeckEntity>> {
            return deckRepository.getDecks()
                .combine(settingsRepository.settings.map { it.deletedBundledDeckIds }) { allDecks, deletedIds ->
                    val filtered = allDecks.filter { deck -> !deletedIds.contains(deck.id) }
                    logger.debug("Filtered decks: ${filtered.size} (removed ${allDecks.size - filtered.size})")
                    filtered
                }
        }

        fun observeEnabledDeckIds(): Flow<Set<String>> = settingsRepository.settings.map { it.enabledDeckIds }

        fun observeTrustedSources(): Flow<Set<String>> = settingsRepository.settings.map { it.trustedSources }

        fun observeSettings(): Flow<Settings> = settingsRepository.settings

        suspend fun prepareInitialLoad(): InitialLoadResult {
            return withContext(Dispatchers.IO) {
                val assetFiles = bundledDeckProvider.listBundledDeckFiles()
                val previousHashes = settingsRepository.readBundledDeckHashes()
                val previousBundledDeckIds = previousHashes.map { it.substringBefore(':') }.filterNot { it.endsWith(".json") }.toSet()
                val deletedBundledDeckIds = settingsRepository.readDeletedBundledDeckIds()

                val existingDecks = deckRepository.getDecks().first()
                val hadDecks = existingDecks.isNotEmpty()

                val scanResult = determineBundledDecksToImport(
                    assetFiles = assetFiles,
                    previousHashes = previousHashes,
                    deletedBundledDeckIds = deletedBundledDeckIds,
                    hadDecks = hadDecks,
                )

                pruneOrphanedBundledDecks(existingDecks, scanResult.currentBundledDeckIds, deletedBundledDeckIds, previousBundledDeckIds)

                importBundledDecks(scanResult.toImport, scanResult.assetContents)

                persistBundledDeckHashes(scanResult.currentDeckEntries)

                val baseSettings = settingsRepository.settings.first()
                val allDecksAfterImport = deckRepository.getDecks().first()
                val availableDecks = allDecksAfterImport.filter { !deletedBundledDeckIds.contains(it.id) }
                val resolvedEnabled = resolveInitialEnabledDecks(baseSettings, availableDecks, deletedBundledDeckIds)

                if (baseSettings.enabledDeckIds.isEmpty()) {
                    runCatching { settingsRepository.setEnabledDeckIds(resolvedEnabled) }
                        .onFailure { logger.error("Failed to persist enabled deck ids", it) }
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
            val categories = settings.selectedCategories
                .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                .takeIf { it.isNotEmpty() }
            val classes = canonicalizeWordClassFilters(settings.selectedWordClasses).takeIf { it.isNotEmpty() }
            val languages = settings.selectedDeckLanguages.toList()
            return WordQueryFilters(
                deckIds = deckIds,
                allowNSFW = settings.allowNSFW,
                minDifficulty = settings.minDifficulty,
                maxDifficulty = settings.maxDifficulty,
                categories = categories,
                categoryFilterEnabled = if ((categories == null) || categories.isEmpty()) 0 else 1,
                wordClasses = classes,
                wordClassFilterEnabled = if ((classes == null) || classes.isEmpty()) 0 else 1,
                languages = languages,
                languageFilterEnabled = if ((languages == null) || languages.isEmpty()) 0 else 1,
            )
        }

        suspend fun loadWords(filters: WordQueryFilters): List<String> {
            if (filters.deckIds.isEmpty()) return emptyList()
            return withContext(Dispatchers.IO) {
                wordDao.getWordTextsForDecks(
                    filters.deckIds,
                    filters.allowNSFW,
                    filters.minDifficulty,
                    filters.maxDifficulty,
                    filters.categories,
                    filters.categoryFilterEnabled,
                    filters.wordClasses,
                    filters.wordClassFilterEnabled,
                    filters.languages,
                    filters.languageFilterEnabled,
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
                            filters.allowNSFW,
                            filters.minDifficulty,
                            filters.maxDifficulty,
                            filters.categories,
                            filters.categoryFilterEnabled,
                            filters.wordClasses,
                            filters.wordClassFilterEnabled,
                            filters.languages,
                            filters.languageFilterEnabled,
                        )
                    }
                    val categoriesDeferred = async {
                        wordDao.getAvailableCategories(
                            filters.deckIds,
                            filters.allowNSFW,
                            filters.languages,
                            filters.languageFilterEnabled,
                        )
                    }
                    val classesDeferred = async {
                        wordDao.getAvailableWordClasses(
                            filters.deckIds,
                            filters.allowNSFW,
                            filters.languages,
                            filters.languageFilterEnabled,
                        )
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
                isNsfw = sanitized.pack.deck.isNSFW,
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
            val isBundledDeck = deck.isOfficial
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    if (isBundledDeck) {
                        settingsRepository.addDeletedBundledDeckId(deck.id)
                    } else {
                        deckRepository.deleteDeck(deck.id)
                    }
                }
                settingsRepository.removeEnabledDeckId(deck.id)
            }
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                return DeleteDeckResult.Failure(error)
            }
            val message = if (isBundledDeck) {
                "Hidden deck: ${deck.name}"
            } else {
                "Deleted deck: ${deck.name}"
            }
            return DeleteDeckResult.Success(message)
        }

        suspend fun permanentlyDeleteImportedDeck(deck: DeckEntity): Result<Unit> =
            runCatching {
                withContext(Dispatchers.IO) { deckRepository.deleteDeck(deck.id) }
                settingsRepository.removeEnabledDeckId(deck.id)
            }

        suspend fun restoreDeletedBundledDeck(deckId: String): Result<Unit> {
            return runCatching {
                withContext(Dispatchers.IO) {
                    settingsRepository.removeDeletedBundledDeckId(deckId)
                }
            }
        }

        suspend fun getWordCount(deckId: String): Int = deckRepository.getWordCount(deckId)

        suspend fun getDeckCategories(deckId: String): List<String> = withContext(Dispatchers.IO) {
            wordDao.getDeckCategories(deckId)
        }

        suspend fun getDeckWordSamples(deckId: String, limit: Int = 5): List<String> =
            withContext(Dispatchers.IO) { wordDao.getRandomWordSamples(deckId, limit) }

        suspend fun getDeckDifficultyHistogram(deckId: String): List<DifficultyBucket> =
            withContext(Dispatchers.IO) { deckRepository.getDifficultyHistogram(deckId) }

        suspend fun getDeckRecentWords(deckId: String, limit: Int = 8): List<String> =
            withContext(Dispatchers.IO) { turnHistoryDao.getRecentWordsForDeck(deckId, limit) }

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
                allowNSFW = settings.allowNSFW,
                languages = settings.selectedDeckLanguages,
            )

        suspend fun loadAvailableWordClasses(key: WordClassAvailabilityKey): List<String> {
            val ids = key.deckIds.toList()
            if (ids.isEmpty()) return emptyList()
            val languages = key.languages.toList()
            val classes = withContext(Dispatchers.IO) {
                wordDao.getAvailableWordClasses(
                    ids,
                    key.allowNSFW,
                    languages,
                    if (languages.isEmpty()) 0 else 1,
                )
            }
            return canonicalizeWordClassFilters(classes)
        }

        private data class SanitizedPack(
            val pack: ParsedPack,
            val coverImageError: Throwable?,
        )

        private fun parseDeckIdsFromContent(content: String): List<String> {
            return try {
                val root = bundleJson.parseToJsonElement(content)
                val deckId = root
                    .jsonObject["deck"]
                    ?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.contentOrNull
                if (deckId.isNullOrBlank()) emptyList() else listOf(deckId)
            } catch (e: Exception) {
                logger.error("Failed to parse deck IDs from content", e)
                emptyList()
            }
        }

        private suspend fun sanitizeCoverImage(pack: ParsedPack): SanitizedPack {
            var coverImageError: Throwable? = null
            val sanitizedDeck = when {
                pack.coverImageUrl != null -> {
                    val coverUrl = checkNotNull(pack.coverImageUrl)
                    val result = fetchCoverImageFromUrl(coverUrl)
                    if (result.isSuccess) {
                        pack.deck.copy(coverImageBase64 = result.getOrThrow())
                    } else {
                        val error = result.exceptionOrNull()
                        if (error is CancellationException) throw error
                        val wrapped = wrapCoverImageError("Failed to download cover image", error)
                        logger.error(
                            "Failed to download deck cover image for ${pack.deck.id}",
                            wrapped,
                        )
                        coverImageError = wrapped
                        pack.deck.copy(coverImageBase64 = null)
                    }
                }
                pack.deck.coverImageBase64 != null -> {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val decoded = Base64.decode(pack.deck.coverImageBase64, Base64.DEFAULT)
                            PackValidator.validateCoverImageBytes(decoded)
                        }
                    }
                    if (result.isSuccess) {
                        pack.deck.copy(coverImageBase64 = result.getOrThrow())
                    } else {
                        val error = result.exceptionOrNull()
                        if (error is CancellationException) throw error
                        val wrapped = wrapCoverImageError("Failed to decode cover image", error)
                        logger.error("Failed to decode deck cover image for ${pack.deck.id}", wrapped)
                        coverImageError = wrapped
                        pack.deck.copy(coverImageBase64 = null)
                    }
                }
                else -> pack.deck
            }
            val sanitized = pack.copy(deck = sanitizedDeck, coverImageUrl = null)
            return SanitizedPack(sanitized, coverImageError)
        }

        private suspend fun fetchCoverImageFromUrl(url: String): Result<String> {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = downloader.download(
                        url.trim(),
                        expectedSha256 = null,
                        maxBytes = PackValidator.MAX_COVER_IMAGE_BYTES.toLong(),
                    )
                    PackValidator.validateCoverImageBytes(bytes)
                }
            }
        }

        private fun wrapCoverImageError(message: String, error: Throwable?): Throwable {
            return when (error) {
                null -> CoverImageException(message)
                is CoverImageException -> error
                else -> CoverImageException(message, error)
            }
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
                val root = bundleJson.parseToJsonElement(content).jsonObject
                val deck = root["deck"]?.jsonObject ?: return null
                if (!deck.containsKey("coverImage") && !deck.containsKey("coverImageUrl")) {
                    return null
                }
                val sanitizedDeck = buildJsonObject {
                    deck.forEach { (key, value) ->
                        if (key != "coverImage" && key != "coverImageUrl") {
                            put(key, value)
                        }
                    }
                }
                val sanitizedRoot = buildJsonObject {
                    root.forEach { (key, value) ->
                        if (key == "deck") {
                            put(key, sanitizedDeck)
                        } else {
                            put(key, value)
                        }
                    }
                }
                bundleJson.encodeToString(JsonObject.serializer(), sanitizedRoot)
            } catch (error: Exception) {
                null
            }
        }

        private fun Throwable.isCoverImageError(): Boolean {
            if (this is CoverImageException) {
                return true
            }
            val cause = cause
            return cause != null && cause !== this && cause.isCoverImageError()
        }

        private companion object {
            val bundleJson: Json = Json { ignoreUnknownKeys = true }
        }
    }
