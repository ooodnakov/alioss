package com.example.alias.ui.decks

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
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

private data class DeckCoverCacheEntry(val bitmap: ImageBitmap?)

object DeckCoverImageLoader {
    private const val CACHE_SIZE = 24
    private val cache = object : LruCache<String, DeckCoverCacheEntry>(CACHE_SIZE) {}
    private val mutex = Mutex()

    suspend fun load(base64: String): ImageBitmap? {
        val key = cacheKey(base64)
        val cached = mutex.withLock { cache.get(key) }
        if (cached != null) {
            return cached.bitmap
        }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                if (decoded.isEmpty()) {
                    null
                } else {
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                }
            }.getOrNull()
        }
        return mutex.withLock {
            val existing = cache.get(key)
            if (existing != null) {
                existing.bitmap
            } else {
                cache.put(key, DeckCoverCacheEntry(bitmap))
                bitmap
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
}

@Composable
fun rememberDeckCoverImage(base64: String?): ImageBitmap? {
    val imageState = produceState<ImageBitmap?>(initialValue = null, base64) {
        value = base64?.let { DeckCoverImageLoader.load(it) }
    }
    return imageState.value
}
