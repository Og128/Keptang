package com.keptang.data.repository

import com.keptang.data.db.LearnedCategoryDao
import com.keptang.data.db.LearnedCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import java.time.Instant

/**
 * What the parser has been taught by the user's own corrections. See [com.keptang.parser.CategoryVocabulary].
 */
class LearnedCategoryRepository(private val learnedCategoryDao: LearnedCategoryDao) {

    /** keyword -> category, ready to hand to the parser. */
    suspend fun asVocabulary(): Map<String, String> =
        learnedCategoryDao.getAll().associate { it.keyword to it.category }

    fun observeAll(): Flow<List<LearnedCategoryEntity>> = learnedCategoryDao.observeAll()

    fun observeCount(): Flow<Int> = learnedCategoryDao.observeAll().map { it.size }

    /**
     * Teaches that [description] means [category]. Called when the user corrects a category by
     * hand - never when they only change an amount or a date, since re-saving an expense the
     * parser got right must not re-teach what it already knew.
     *
     * Blank descriptions teach nothing: there would be no keyword to match on next time.
     */
    suspend fun learn(description: String?, category: String) {
        val keyword = normalize(description) ?: return
        if (category.isBlank() || category == UNCATEGORIZED) return
        learnedCategoryDao.upsert(
            LearnedCategoryEntity(
                keyword = keyword,
                category = category,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun forget(keyword: String) = learnedCategoryDao.deleteByKeyword(keyword)

    companion object {
        const val UNCATEGORIZED = com.keptang.parser.UNCATEGORIZED

        /**
         * Lowercased, accent-folded and whitespace-collapsed, so "Café" and "cafe " are the same
         * lesson. The parser lowercases what it matches against too.
         */
        fun normalize(raw: String?): String? {
            val trimmed = raw?.trim()?.replace(Regex("""\s+"""), " ") ?: return null
            if (trimmed.isBlank()) return null
            return Normalizer.normalize(trimmed, Normalizer.Form.NFC).lowercase()
        }
    }
}
