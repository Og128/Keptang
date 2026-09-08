package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsert(tag: TagEntity)

    @Query("SELECT tag FROM expense_tags WHERE expense_id = :expenseId ORDER BY tag ASC")
    fun observeTagsForExpense(expenseId: String): Flow<List<String>>

    @Query("SELECT tag FROM expense_tags WHERE expense_id = :expenseId ORDER BY tag ASC")
    suspend fun getTagsForExpense(expenseId: String): List<String>

    /** Backs the Expenses screen's tag filter, which needs every expense's tags at once rather than one at a time. */
    @Query("SELECT * FROM expense_tags")
    fun observeAllCrossRefs(): Flow<List<ExpenseTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<ExpenseTagCrossRef>)

    @Query("DELETE FROM expense_tags WHERE expense_id = :expenseId")
    suspend fun clearTagsForExpense(expenseId: String)

    /** Replaces every tag on [expenseId] with [tags] in one transaction, registering any brand-new tag name for future autocomplete. */
    @Transaction
    suspend fun setTagsForExpense(expenseId: String, tags: List<String>) {
        clearTagsForExpense(expenseId)
        tags.forEach { upsert(TagEntity(it)) }
        insertCrossRefs(tags.map { ExpenseTagCrossRef(expenseId, it) })
    }
}
