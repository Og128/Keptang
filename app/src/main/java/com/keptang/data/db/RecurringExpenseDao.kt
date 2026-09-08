package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(recurring: RecurringExpenseEntity)

    @Update
    suspend fun update(recurring: RecurringExpenseEntity)

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: String): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses ORDER BY name ASC")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    /** One-shot read for the app-launch generation check - not observed, run once per launch. */
    @Query("SELECT * FROM recurring_expenses WHERE next_due_at_epoch_millis <= :nowEpochMillis")
    suspend fun getDue(nowEpochMillis: Long): List<RecurringExpenseEntity>

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Cascades a category rename (see [com.keptang.data.repository.CategoryRepository]). */
    @Query("UPDATE recurring_expenses SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)
}
