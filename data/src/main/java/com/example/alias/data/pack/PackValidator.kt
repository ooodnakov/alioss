package com.example.alias.data.pack

import com.example.alias.domain.word.WordClassCatalog
import java.util.Base64
import java.util.Locale

/**
 * Validates pack metadata and words to enforce basic constraints.
 */
object PackValidator {
    const val MULTI_LANGUAGE_TAG = "mul"
    private const val MAX_WORDS = 200_000
    const val MAX_COVER_IMAGE_BYTES = 256_000
    private const val MAX_COVER_IMAGE_DIMENSION = 2_048
    private const val MAX_COVER_IMAGE_URL_LENGTH = 2_048
    private val ID_REGEX = Regex("^[a-z0-9_-]{1,64}$")
    private val LANG_REGEX = Regex("^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$")
    private val base64Encoder = Base64.getEncoder()
    private val base64MimeDecoder = Base64.getMimeDecoder()

    fun validateFormat(format: String) {
        require(format == "alias-deck@1") { "Unsupported pack format: $format" }
    }

    fun validateDeck(
        id: String,
        language: String,
        name: String,
        version: Int,
        @Suppress("UNUSED_PARAMETER") isNSFW: Boolean,
        coverImageBase64: String?,
    ): String? {
        require(ID_REGEX.matches(id)) { "Invalid deck id" }
        require(name.isNotBlank() && name.length <= 100) { "Invalid deck name" }
        require(LANG_REGEX.matches(language)) { "Invalid language tag" }
        require(version >= 1) { "Invalid version" }
        // isNSFW: no constraint (boolean)
        return normalizeCoverImage(coverImageBase64)
    }

    fun normalizeCoverImageUrl(coverImageUrl: String?): String? {
        if (coverImageUrl == null) {
            return null
        }
        val trimmed = coverImageUrl.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        if (trimmed.length > MAX_COVER_IMAGE_URL_LENGTH) {
            throw CoverImageException("Cover image URL too long")
        }
        val uri = try {
            java.net.URI(trimmed)
        } catch (error: java.net.URISyntaxException) {
            throw CoverImageException("Invalid cover image URL", error)
        }
        if (!uri.isAbsolute) {
            throw CoverImageException("Cover image URL must be absolute")
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw CoverImageException("Cover image URL must use HTTPS")
        }
        if (uri.host.isNullOrBlank()) {
            throw CoverImageException("Cover image URL missing host")
        }
        if (uri.userInfo != null) {
            throw CoverImageException("Cover image URL must not contain credentials")
        }
        return uri.toString()
    }

    fun normalizeLanguageTag(language: String): String {
        val trimmed = language.trim()
        require(LANG_REGEX.matches(trimmed)) { "Invalid language tag" }
        return trimmed.lowercase(Locale.ROOT)
    }

    private fun normalizeCoverImage(coverImageBase64: String?): String? {
        if (coverImageBase64 == null) {
            return null
        }
        val trimmed = coverImageBase64.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val dataPortion = trimmed.substringAfter(',', trimmed)
        val decoded = try {
            base64MimeDecoder.decode(dataPortion)
        } catch (error: IllegalArgumentException) {
            throw CoverImageException("Invalid cover image encoding", error)
        }
        return validateCoverImageBytes(decoded)
    }

    fun validateCoverImageBytes(data: ByteArray): String {
        if (data.isEmpty()) {
            throw CoverImageException("Cover image is empty")
        }
        if (data.size > MAX_COVER_IMAGE_BYTES) {
            throw CoverImageException("Cover image too large")
        }
        val (width, height) = decodeImageDimensions(data)
            ?: throw CoverImageException("Cover image has invalid dimensions")
        if (width <= 0 || height <= 0) {
            throw CoverImageException("Cover image has invalid dimensions")
        }
        if (width > MAX_COVER_IMAGE_DIMENSION || height > MAX_COVER_IMAGE_DIMENSION) {
            throw CoverImageException("Cover image dimensions too large")
        }
        return base64Encoder.encodeToString(data)
    }

    private fun decodeImageDimensions(data: ByteArray): Pair<Int, Int>? {
        if (data.size >= 24 && hasPngSignature(data)) {
            val width = readUInt32(data, 16)
            val height = readUInt32(data, 20)
            return width to height
        }
        if (data.size >= 4 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte()) {
            var offset = 2
            while (offset + 4 < data.size) {
                if (data[offset] != 0xFF.toByte()) {
                    offset++
                    continue
                }
                val marker = data[offset + 1].toInt() and 0xFF
                if (marker == 0xFF) {
                    offset++
                    continue
                }
                if (marker == 0xD9 || marker == 0xDA) {
                    break
                }
                if (offset + 3 >= data.size) {
                    return null
                }
                val length = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
                if (length < 2 || offset + 2 + length > data.size) {
                    return null
                }
                if (isStartOfFrame(marker)) {
                    if (length < 7) {
                        return null
                    }
                    val height = ((data[offset + 5].toInt() and 0xFF) shl 8) or (data[offset + 6].toInt() and 0xFF)
                    val width = ((data[offset + 7].toInt() and 0xFF) shl 8) or (data[offset + 8].toInt() and 0xFF)
                    return width to height
                }
                offset += 2 + length
            }
        }
        return null
    }

    private fun readUInt32(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    private fun isStartOfFrame(marker: Int): Boolean {
        return when (marker) {
            0xC0, 0xC1, 0xC2, 0xC3,
            0xC5, 0xC6, 0xC7,
            0xC9, 0xCA, 0xCB,
            0xCD, 0xCE, 0xCF,
            -> true
            else -> false
        }
    }

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

    private fun hasPngSignature(data: ByteArray): Boolean {
        if (data.size < PNG_SIGNATURE.size) {
            return false
        }
        PNG_SIGNATURE.forEachIndexed { index, value ->
            if (data[index] != value) {
                return false
            }
        }
        return true
    }

    fun validateWordCount(count: Int) {
        require(count in 1..MAX_WORDS) { "Invalid word count: $count" }
    }

    fun validateWord(
        text: String,
        deckLanguage: String,
        wordLanguage: String?,
        difficulty: Int,
        category: String?,
        tabooStems: List<String>?,
        wordClass: String?,
    ) {
        require(text.isNotBlank() && text.trim().length <= 120) { "Invalid word text" }
        require(difficulty in 1..5) { "Invalid difficulty: $difficulty" }
        if (deckLanguage == MULTI_LANGUAGE_TAG) {
            require(!wordLanguage.isNullOrBlank()) { "Multi-language decks require per-word language" }
        } else if (wordLanguage != null) {
            require(wordLanguage == deckLanguage) { "Word language must match deck language" }
        }
        if (category != null) {
            require(category.trim().length <= 64) { "Invalid category length" }
        }
        if (tabooStems != null) {
            require(tabooStems.size <= 10) { "Too many taboo stems" }
            tabooStems.forEach { stem ->
                require(stem.isNotBlank() && stem.length <= 32) { "Invalid taboo stem" }
            }
        }
        if (wordClass != null) {
            val normalized = WordClassCatalog.normalizeOrNull(wordClass)
            require(normalized != null) { "Unsupported word class: $wordClass" }
        }
    }

    fun validateMultiLanguageContent(languages: Set<String>) {
        require(languages.isNotEmpty()) { "Multi-language deck must include languages" }
        require(languages.size >= 2) { "Multi-language deck must include at least two languages" }
    }
}
