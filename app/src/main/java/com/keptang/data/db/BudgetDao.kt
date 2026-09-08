package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?

    /** `IS` (rather than `=`) so a null [category] (the Overall Budget) matches other null rows instead of matching nothing. */
    @Query("SELECT COUNT(*) FROM budgets WHERE category IS :category")
    suspend fun countForCategory(category: String?): Int

    /**
     * Atomically checks-then-inserts within one transaction, closing the race a separate
     * check+insert from [com.keptang.data.repository.BudgetRepository] had: two concurrent calls
     * could both pass the check before either insert committed, creating two Budgets for the same
     * category.
     */
    @Transaction
    suspend fun insertUnique(budget: BudgetEntity) {
        check(countForCategory(budget.category) == 0) {
            "A budget already exists for ${budget.category ?: "Overall"}"
        }
        insert(budget)
    }

    /** SQLite sorts NULL first in ASC order, so the Overall Budget naturally leads the list. */
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Cascades a category rename (see [com.keptang.data.repository.CategoryRepository]). */
    @Query("UPDATE budgets SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)

    /** Backs the in-use guard in [com.keptang.data.repository.CategoryRepository.delete]. */
    @Query("SELECT COUNT(*) FROM budgets WHERE category = :category")
    suspend fun countByCategory(category: String): Int
}
