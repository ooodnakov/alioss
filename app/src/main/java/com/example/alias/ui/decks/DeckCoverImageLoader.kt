package com.example.alias.ui.decks

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.text.Charsets

internal data class DeckCoverCacheEntry(
    val bitmap: ImageBitmap?,
    val sizeBytes: Int,
)

internal interface DeckCoverCache {
    operator fun get(key: String): DeckCoverCacheEntry?
    fun put(key: String, value: DeckCoverCacheEntry): DeckCoverCacheEntry?
    fun evictAll()
}

object DeckCoverImageLoader {
    private const val MAX_COVER_DIMENSION = 2_048
    private const val BYTES_PER_ARGB_8888_PIXEL = 4
    private const val TARGET_COVERS_IN_CACHE = 2

    /**
     * PackValidator guarantees cover images are at most 2048px on each side. Budget for two
     * max-size ARGB_8888 bitmaps (~32 MiB) so we keep typical covers cached while preventing the
     * cache from retaining too many large decoded images at once.
     */
    private const val MAX_CACHE_SIZE_BYTES =
        MAX_COVER_DIMENSION * MAX_COVER_DIMENSION * BYTES_PER_ARGB_8888_PIXEL * TARGET_COVERS_IN_CACHE

    private var cache: DeckCoverCache = createCache()
    private val mutex = Mutex()

    @VisibleForTesting
    internal var decoderOverride: ((String) -> DeckCoverCacheEntry)? = null

    @VisibleForTesting
    internal suspend fun overrideCacheForTest(newCache: DeckCoverCache) {
        mutex.withLock { cache = newCache }
    }

    @VisibleForTesting
    internal suspend fun resetCacheForTest() {
        mutex.withLock { cache = createCache() }
    }

    @VisibleForTesting
    internal fun maxCacheSizeBytes(): Int = MAX_CACHE_SIZE_BYTES

    suspend fun load(base64: String): ImageBitmap? {
        val key = cacheKey(base64)
        val cached = mutex.withLock { cache.get(key) }
        if (cached != null) {
            return cached.bitmap
        }
        val entry = withContext(Dispatchers.IO) {
            runCatching {
                decode(base64)
            }.getOrDefault(DeckCoverCacheEntry(bitmap = null, sizeBytes = 0))
        }
        return mutex.withLock {
            val existing = cache.get(key)
            if (existing != null) {
                existing.bitmap
            } else {
                cache.put(key, entry)
                entry.bitmap
            }
        }
    }

    suspend fun clear() {
        mutex.withLock { cache.evictAll() }
    }

    private fun cacheKey(base64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(base64.toByteArray(Charsets.UTF_8))
        return hashed.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun decode(base64: String): DeckCoverCacheEntry {
        val override = decoderOverride
        if (override != null) {
            return override(base64)
        }
        val decoded = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = if (decoded.isEmpty()) {
            null
        } else {
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
        }
        val sizeBytes = bitmap?.allocationByteCount ?: 0
        return DeckCoverCacheEntry(bitmap = bitmap?.asImageBitmap(), sizeBytes = sizeBytes)
    }

    private fun createCache(): DeckCoverCache = BitmapByteLruCache(MAX_CACHE_SIZE_BYTES)
}

internal class BitmapByteLruCache(maxSize: Int) :
    LruCache<String, DeckCoverCacheEntry>(maxSize), DeckCoverCache {

    override fun sizeOf(key: String, value: DeckCoverCacheEntry): Int = value.sizeBytes
}

@Composable
fun rememberDeckCoverImage(base64: String?): ImageBitmap? {
    val imageState = produceState<ImageBitmap?>(initialValue = null, base64) {
        value = base64?.let { DeckCoverImageLoader.load(it) }
    }
    return imageState.value
}
