package com.example.alias.data.pack

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackValidatorTest {

    @Test
    fun `normalize language tag trims and lowercases`() {
        assertEquals("en-us", PackValidator.normalizeLanguageTag(" En-US "))
        assertEquals("mul", PackValidator.normalizeLanguageTag("mul"))
        assertFailsWith<IllegalArgumentException> { PackValidator.normalizeLanguageTag("bad tag!") }
    }

    @Test
    fun `normalize cover image url enforces https`() {
        assertNull(PackValidator.normalizeCoverImageUrl(null))
        assertNull(PackValidator.normalizeCoverImageUrl("   "))

        val normalized = PackValidator.normalizeCoverImageUrl(" https://example.com/images/cover.png ")
        assertEquals("https://example.com/images/cover.png", normalized)

        assertFailsWith<CoverImageException> { PackValidator.normalizeCoverImageUrl("ftp://example.com/image.png") }
        assertFailsWith<CoverImageException> {
            PackValidator.normalizeCoverImageUrl(
                "***example.com/image.png",
            )
        }
        assertFailsWith<CoverImageException> {
            PackValidator.normalizeCoverImageUrl(
                "https://user:pass@example.com/image.png",
            )
        }
        assertFailsWith<CoverImageException> { PackValidator.normalizeCoverImageUrl("/relative/path.png") }
    }

    @Test
    fun `validate deck normalizes embedded cover image`() {
        val normalized = PackValidator.validateDeck(
            id = "deck_1",
            language = "en",
            name = "Deck One",
            version = 1,
            isNSFW = false,
            coverImageBase64 = " data:image/png;base64,${ONE_BY_ONE_PNG_BASE64} ",
        )
        assertEquals(ONE_BY_ONE_PNG_BASE64, normalized)

        assertNull(
            PackValidator.validateDeck(
                id = "deck_2",
                language = "en",
                name = "Deck Two",
                version = 1,
                isNSFW = false,
                coverImageBase64 = "   ",
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateDeck(
                id = "bad id",
                language = "en",
                name = "Deck",
                version = 1,
                isNSFW = false,
                coverImageBase64 = null,
            )
        }
    }

    @Test
    fun `validate cover image bytes accepts png and rejects invalid data`() {
        val bytes = Base64.getMimeDecoder().decode(ONE_BY_ONE_PNG_BASE64)
        val normalized = PackValidator.validateCoverImageBytes(bytes)
        assertEquals(Base64.getEncoder().encodeToString(bytes), normalized)

        assertFailsWith<CoverImageException> { PackValidator.validateCoverImageBytes(ByteArray(0)) }
        assertFailsWith<CoverImageException> {
            PackValidator.validateCoverImageBytes(ByteArray(PackValidator.MAX_COVER_IMAGE_BYTES + 1))
        }

        val zeroDimension = bytes.copyOf()
        zeroDimension[16] = 0
        zeroDimension[17] = 0
        zeroDimension[18] = 0
        zeroDimension[19] = 0
        assertFailsWith<CoverImageException> { PackValidator.validateCoverImageBytes(zeroDimension) }
    }

    @Test
    fun `validate cover image bytes accepts large png under limit`() {
        val largeBytes = fakePngBytes(width = 512, height = 512, totalSize = 3 * 1024 * 1024)
        assertTrue(largeBytes.size > 1_000_000)

        val normalized = PackValidator.validateCoverImageBytes(largeBytes)
        assertEquals(Base64.getEncoder().encodeToString(largeBytes), normalized)
    }

    @Test
    fun `validate word enforces language and metadata`() {
        PackValidator.validateWord(
            text = "Director",
            deckLanguage = "en",
            wordLanguage = "en",
            difficulty = 3,
            category = "movies",
            tabooStems = listOf("direct"),
            wordClass = "noun",
        )

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Hola",
                deckLanguage = "en",
                wordLanguage = "es",
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = null,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Bonjour",
                deckLanguage = PackValidator.MULTI_LANGUAGE_TAG,
                wordLanguage = null,
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = null,
            )
        }

        val longCategory = "a".repeat(65)
        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Overflow",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = longCategory,
                tabooStems = null,
                wordClass = null,
            )
        }

        val stems = List(11) { "stem$it" }
        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "TooMany",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = null,
                tabooStems = stems,
                wordClass = null,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PackValidator.validateWord(
                text = "Verb",
                deckLanguage = "en",
                wordLanguage = "en",
                difficulty = 2,
                category = null,
                tabooStems = null,
                wordClass = "invalid",
            )
        }
    }

    @Test
    fun `validate word count enforces bounds`() {
        PackValidator.validateWordCount(1)
        PackValidator.validateWordCount(200_000)
        assertFailsWith<IllegalArgumentException> { PackValidator.validateWordCount(0) }
        assertFailsWith<IllegalArgumentException> { PackValidator.validateWordCount(200_001) }
    }

    @Test
    fun `validate multi language content requires multiple languages`() {
        PackValidator.validateMultiLanguageContent(setOf("en", "ru"))
        assertFailsWith<IllegalArgumentException> { PackValidator.validateMultiLanguageContent(emptySet()) }
        assertFailsWith<IllegalArgumentException> { PackValidator.validateMultiLanguageContent(setOf("en")) }
    }

    companion object {
        private const val ONE_BY_ONE_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII="
        private val PNG_SIGNATURE = byteArrayOf(
            137.toByte(),
            80,
            78,
            71,
            13,
            10,
            26,
            10,
        )
    }

    private fun fakePngBytes(width: Int, height: Int, totalSize: Int): ByteArray {
        require(totalSize >= 33) { "PNG must be large enough to hold signature and IHDR chunk" }
        val bytes = ByteArray(totalSize) { 0 }
        PNG_SIGNATURE.copyInto(bytes, destinationOffset = 0)
        writeInt(bytes, 8, 13)
        bytes[12] = 'I'.code.toByte()
        bytes[13] = 'H'.code.toByte()
        bytes[14] = 'D'.code.toByte()
        bytes[15] = 'R'.code.toByte()
        writeInt(bytes, 16, width)
        writeInt(bytes, 20, height)
        bytes[24] = 8 // bit depth
        bytes[25] = 2 // color type (RGB)
        bytes[26] = 0 // compression
        bytes[27] = 0 // filter
        bytes[28] = 0 // interlace

        val iendOffset = totalSize - 12
        writeInt(bytes, iendOffset, 0)
        bytes[iendOffset + 4] = 'I'.code.toByte()
        bytes[iendOffset + 5] = 'E'.code.toByte()
        bytes[iendOffset + 6] = 'N'.code.toByte()
        bytes[iendOffset + 7] = 'D'.code.toByte()
        return bytes
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value shr 24) and 0xFF).toByte()
        target[offset + 1] = ((value shr 16) and 0xFF).toByte()
        target[offset + 2] = ((value shr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }
}
