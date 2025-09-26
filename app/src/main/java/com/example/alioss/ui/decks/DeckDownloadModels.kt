package com.example.alioss.ui.decks

/** Indicates the current step of a deck download operation. */
enum class DeckDownloadStep { DOWNLOADING, IMPORTING }

/**
 * Progress payload emitted while downloading or importing a deck.
 *
 * @property step current step of the workflow.
 * @property bytesRead number of bytes processed so far.
 * @property totalBytes total bytes expected, if known.
 */
data class DeckDownloadProgress(
    val step: DeckDownloadStep,
    val bytesRead: Long = 0L,
    val totalBytes: Long? = null,
)
