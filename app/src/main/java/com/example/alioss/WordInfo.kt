package com.example.alioss

/** Metadata associated with a single word surfaced to the UI. */
data class WordInfo(
    val difficulty: Int,
    val category: String?,
    val wordClass: String?,
)

/** Aggregated metadata describing the available word set for active filters. */
data class WordMetadata(
    val infoByWord: Map<String, WordInfo>,
    val categories: List<String>,
    val wordClasses: List<String>,
)
