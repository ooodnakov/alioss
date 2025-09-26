package com.example.alioss.domain

import com.example.alioss.domain.word.WordClassCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordClassCatalogTest {
    @Test
    fun `normalize or null handles case and blanks`() {
        assertEquals("NOUN", WordClassCatalog.normalizeOrNull(" noun "))
        assertEquals("VERB", WordClassCatalog.normalizeOrNull("verb"))
        assertEquals("ADJ", WordClassCatalog.normalizeOrNull("Adj"))
        assertNull(WordClassCatalog.normalizeOrNull(""))
        assertNull(WordClassCatalog.normalizeOrNull("invalid"))
        assertNull(WordClassCatalog.normalizeOrNull(null))
    }

    @Test
    fun `filter allowed normalizes input and removes duplicates`() {
        val filtered = WordClassCatalog.filterAllowed(listOf("verb", "NOUN", "invalid", "verb", "adj"))
        assertEquals(listOf("VERB", "NOUN", "ADJ"), filtered)
    }

    @Test
    fun `order returns canonical order for allowed values`() {
        val ordered = WordClassCatalog.order(listOf("noun", "adj", "verb", "unknown"))
        assertEquals(listOf("ADJ", "VERB", "NOUN"), ordered)
    }

    @Test
    fun `allowed catalog remains unique and stable`() {
        val allowed = WordClassCatalog.allowed
        assertEquals(allowed.toSet().size, allowed.size)
        assertTrue(allowed.containsAll(listOf("ADJ", "VERB", "NOUN")))
    }
}
