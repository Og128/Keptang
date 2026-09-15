package com.keptang.data.repository

import com.keptang.data.db.TagDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(private val tagDao: TagDao) {

    /** Every tag currently attached to at least one expense - backs both the filter row and the form's suggestions. */
    fun observeAllNames(): Flow<List<String>> = tagDao.observeNamesInUse()

    fun observeTagsForExpense(expenseId: String): Flow<List<String>> = tagDao.observeTagsForExpense(expenseId)

    suspend fun getTagsForExpense(expenseId: String): List<String> = tagDao.getTagsForExpense(expenseId)

    /** Expense id -> its tags, for the Expenses screen's tag filter (needs every expense's tags at once). */
    fun observeTagsByExpenseId(): Flow<Map<String, List<String>>> =
        tagDao.observeAllCrossRefs().map { refs -> refs.groupBy({ it.expenseId }, { it.tag }) }

    /** Replaces every tag on [expenseId] with [tags]; blank/duplicate names are dropped first. */
    suspend fun setTagsForExpense(expenseId: String, tags: List<String>) {
        val cleaned = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        tagDao.setTagsForExpense(expenseId, cleaned)
    }
}
