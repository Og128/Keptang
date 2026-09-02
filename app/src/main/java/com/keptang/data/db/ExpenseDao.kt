package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE capture_id = :captureId")
    suspend fun getByCaptureId(captureId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE capture_id = :captureId")
    fun observeByCaptureId(captureId: String): Flow<List<ExpenseEntity>>

    @Query("DELETE FROM expenses WHERE capture_id = :captureId")
    suspend fun deleteByCaptureId(captureId: String)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM expenses WHERE review_status = :status ORDER BY occurred_at_epoch_millis DESC")
    fun observeByReviewStatus(status: ReviewStatus): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurred_at_epoch_millis DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    /** Feeds the "pick a category to budget" UI, which only offers categories actually in use. */
    @Query("SELECT DISTINCT category FROM expenses ORDER BY category ASC")
    fun observeDistinctCategories(): Flow<List<String>>

    /**
     * Replaces every expense derived from [captureId] with [expenses] in one transaction, so
     * retrying a capture's processing never leaves duplicate rows behind.
     */
    @Transaction
    suspend fun replaceForCapture(captureId: String, expenses: List<ExpenseEntity>) {
        deleteByCaptureId(captureId)
        if (expenses.isNotEmpty()) insertAll(expenses)
    }
}
