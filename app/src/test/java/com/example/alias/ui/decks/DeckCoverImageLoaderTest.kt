package com.example.alias.ui.decks

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import java.util.UUID

class DeckCoverImageLoaderTest {
    private lateinit var decodeCounts: MutableMap<String, Int>

    @Before
    fun setUp() {
        decodeCounts = mutableMapOf()
        DeckCoverImageLoader.overrideCacheForTest(
            FakeByteCountingLruCache(DeckCoverImageLoader.maxCacheSizeBytes()),
        )
        DeckCoverImageLoader.decoderOverride = { key ->
            decodeCounts[key] = decodeCounts.getOrDefault(key, 0) + 1
            createLargeEntry()
        }
    }

    @After
    fun tearDown() {
        DeckCoverImageLoader.decoderOverride = null
        DeckCoverImageLoader.resetCacheForTest()
    }

    @Test
    fun evictsLargeBitmapsWhenCacheBudgetExceeded() = runBlocking {
        val keyPrefix = UUID.randomUUID().toString()
        val firstKey = "$keyPrefix-first"
        val secondKey = "$keyPrefix-second"
        val thirdKey = "$keyPrefix-third"
        DeckCoverImageLoader.load(firstKey)
        DeckCoverImageLoader.load(secondKey)
        DeckCoverImageLoader.load(thirdKey)

        DeckCoverImageLoader.load(firstKey)
        DeckCoverImageLoader.load(secondKey)

        assertEquals(
            "first cover should be decoded again after eviction (counts=$decodeCounts)",
            2,
            decodeCounts[firstKey] ?: 0,
        )
        assertEquals("third cover should decode once (counts=$decodeCounts)", 1, decodeCounts[thirdKey] ?: 0)
        val totalDecodes = decodeCounts.values.sum()
        assertTrue(
            "at least one cached entry should be evicted when size is exceeded (counts=$decodeCounts)",
            totalDecodes >= 4,
        )
    }

    private fun createLargeEntry(): DeckCoverCacheEntry = DeckCoverCacheEntry(
        bitmap = null,
        sizeBytes = SIMULATED_SIZE_BYTES,
    )

    private class FakeByteCountingLruCache(maxSize: Int) : DeckCoverCache {
        private val maxSizeBytes = maxSize
        private val entries = LinkedHashMap<String, DeckCoverCacheEntry>(0, 0.75f, true)
        private var currentSize = 0

        override fun get(key: String): DeckCoverCacheEntry? = entries[key]

        override fun put(key: String, value: DeckCoverCacheEntry): DeckCoverCacheEntry? {
            val previous = entries.put(key, value)
            if (previous != null) {
                currentSize -= previous.sizeBytes
            }
            currentSize += value.sizeBytes
            trimToSize(maxSizeBytes)
            return previous
        }

        private fun trimToSize(maxSize: Int) {
            if (maxSize < 0) {
                entries.clear()
                currentSize = 0
                return
            }
            val iterator = entries.entries.iterator()
            while (currentSize > maxSize && iterator.hasNext()) {
                val entry = iterator.next()
                currentSize -= entry.value.sizeBytes
                iterator.remove()
            }
        }

        override fun evictAll() {
            entries.clear()
            currentSize = 0
        }
    }

    private companion object {
        const val DIMENSION = 2_048
        const val BYTES_PER_PIXEL = 4
        const val SIMULATED_SIZE_BYTES = DIMENSION * DIMENSION * BYTES_PER_PIXEL
    }
}
