package com.example.alias.domain.word

/**
 * Centralized catalog of supported word classes.
 *
 * Updating [allowed] is enough to tweak the recognized values across the
 * application without requiring a database migration.
 */
object WordClassCatalog {
    const val ADJECTIVE = "ADJ"
    const val VERB = "VERB"
    const val NOUN = "NOUN"

    /** Ordered list of allowed classes for stable presentation. */
    val allowed: List<String> = listOf(ADJECTIVE, VERB, NOUN)

    private val allowedSet: Set<String> = allowed.toSet()

    /**
     * Normalize [raw] input to an allowed code or return null if unsupported.
     */
    fun normalizeOrNull(raw: String?): String? {
        return raw
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
            ?.takeIf { allowedSet.contains(it) }
    }

    /**
     * Filter [values] down to the allowed set while preserving the provided order.
     */
    fun filterAllowed(values: Iterable<String>): List<String> {
        val seen = LinkedHashSet<String>()
        values.forEach { value ->
            val normalized = normalizeOrNull(value)
            if (normalized != null) {
                seen.add(normalized)
            }
        }
        return seen.toList()
    }

    /**
     * Return the subset of [values] that is supported, ordered according to [allowed].
     */
    fun order(values: Collection<String>): List<String> {
        if (values.isEmpty()) return emptyList()
        val normalizedSet = filterAllowed(values).toSet()
        return if (normalizedSet.isEmpty()) {
            emptyList()
        } else {
            allowed.filter { normalizedSet.contains(it) }
        }
    }
}
