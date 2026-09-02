package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /** One-shot read backing the uniqueness check in [com.keptang.data.repository.BudgetRepository]. */
    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    /** SQLite sorts NULL first in ASC order, so the Overall Budget naturally leads the list. */
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)
}
