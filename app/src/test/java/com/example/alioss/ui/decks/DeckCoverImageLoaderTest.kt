package com.example.alioss.ui.decks

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeckCoverImageLoaderTest {
    private lateinit var decodeCounts: MutableMap<String, Int>

    @Before
    fun setUp() = runBlocking {
        decodeCounts = mutableMapOf()
        DeckCoverImageLoader.overrideCacheForTest(
            BitmapByteLruCache(DeckCoverImageLoader.maxCacheSizeBytes()),
        )
        DeckCoverImageLoader.decoderOverride = { key ->
            decodeCounts[key] = decodeCounts.getOrDefault(key, 0) + 1
            createLargeEntry()
        }
    }

    @After
    fun tearDown() = runBlocking {
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
    private companion object {
        const val DIMENSION = 2_048
        const val BYTES_PER_PIXEL = 4
        const val SIMULATED_SIZE_BYTES = DIMENSION * DIMENSION * BYTES_PER_PIXEL
    }
}
