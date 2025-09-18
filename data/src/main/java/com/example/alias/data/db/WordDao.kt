package com.example.alias.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordClasses(entries: List<WordClassEntity>)

    @Query("SELECT text FROM words WHERE deckId = :deckId")
    suspend fun getWordTexts(deckId: String): List<String>

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId")
    suspend fun getWordCount(deckId: String): Int

    @Query("DELETE FROM words WHERE deckId = :deckId")
    suspend fun deleteByDeck(deckId: String)

    @Query(
        "SELECT DISTINCT w.text FROM words w " +
            "WHERE w.deckId IN (:deckIds) " +
            "AND w.language = :language " +
            "AND (:allowNSFW = 1 OR w.isNSFW = 0) " +
            "AND w.difficulty BETWEEN :minDifficulty AND :maxDifficulty " +
            "AND (:hasCategories = 0 OR w.category IN (:categories)) " +
            "AND (:hasClasses = 0 OR EXISTS (" +
            "    SELECT 1 FROM word_classes wc " +
            "    WHERE wc.deckId = w.deckId AND wc.wordText = w.text AND UPPER(wc.wordClass) IN (:classes)" +
            "))"
    )
    suspend fun getWordTextsForDecks(
        deckIds: List<String>,
        language: String,
        allowNSFW: Boolean,
        minDifficulty: Int,
        maxDifficulty: Int,
        categories: List<String>,
        hasCategories: Int,
        classes: List<String>,
        hasClasses: Int,
    ): List<String>

    @Query(
        "SELECT w.text, w.difficulty, w.category, GROUP_CONCAT(DISTINCT UPPER(wc.wordClass)) AS wordClass " +
            "FROM words w " +
            "LEFT JOIN word_classes wc ON wc.deckId = w.deckId AND wc.wordText = w.text " +
            "WHERE w.deckId IN (:deckIds) " +
            "AND w.language = :language " +
            "AND (:allowNSFW = 1 OR w.isNSFW = 0) " +
            "AND w.difficulty BETWEEN :minDifficulty AND :maxDifficulty " +
            "AND (:hasCategories = 0 OR w.category IN (:categories)) " +
            "AND (:hasClasses = 0 OR EXISTS (" +
            "    SELECT 1 FROM word_classes wc2 " +
            "    WHERE wc2.deckId = w.deckId AND wc2.wordText = w.text AND UPPER(wc2.wordClass) IN (:classes)" +
            ")) " +
            "GROUP BY w.text, w.difficulty, w.category"
    )
    suspend fun getWordBriefsForDecks(
        deckIds: List<String>,
        language: String,
        allowNSFW: Boolean,
        minDifficulty: Int,
        maxDifficulty: Int,
        categories: List<String>,
        hasCategories: Int,
        classes: List<String>,
        hasClasses: Int,
    ): List<WordBrief>

    @Query(
        "SELECT DISTINCT category FROM words " +
            "WHERE deckId IN (:deckIds) " +
            "AND language = :language " +
            "AND category IS NOT NULL AND TRIM(category) != '' " +
            "AND (:allowNSFW = 1 OR isNSFW = 0)"
    )
    suspend fun getAvailableCategories(
        deckIds: List<String>,
        language: String,
        allowNSFW: Boolean,
    ): List<String>

    @Query(
        "SELECT DISTINCT UPPER(wc.wordClass) FROM word_classes wc " +
            "JOIN words w ON w.deckId = wc.deckId AND w.text = wc.wordText " +
            "WHERE w.deckId IN (:deckIds) " +
            "AND w.language = :language " +
            "AND (:allowNSFW = 1 OR w.isNSFW = 0)"
    )
    suspend fun getAvailableWordClasses(
        deckIds: List<String>,
        language: String,
        allowNSFW: Boolean,
    ): List<String>

    @Query(
        "SELECT DISTINCT category FROM words " +
            "WHERE deckId = :deckId " +
            "AND category IS NOT NULL AND TRIM(category) != '' " +
            "ORDER BY category COLLATE NOCASE"
    )
    suspend fun getDeckCategories(deckId: String): List<String>

    @Query(
        "SELECT text FROM words " +
            "WHERE deckId = :deckId " +
            "ORDER BY RANDOM() " +
            "LIMIT :limit"
    )
    suspend fun getRandomWordSamples(deckId: String, limit: Int): List<String>
}

/** Lightweight projection for word metadata shown in UI. */
data class WordBrief(
    val text: String,
    val difficulty: Int,
    val category: String?,
    val wordClass: String?,
)
