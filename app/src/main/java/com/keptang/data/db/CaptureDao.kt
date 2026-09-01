package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(capture: CaptureEntity)

    @Update
    suspend fun update(capture: CaptureEntity)

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE id = :id")
    fun observeById(id: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM captures ORDER BY captured_at_epoch_millis DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE status IN (:statuses) ORDER BY captured_at_epoch_millis DESC")
    fun observeByStatuses(statuses: List<CaptureStatus>): Flow<List<CaptureEntity>>

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT * FROM captures WHERE status = :status AND captured_at_epoch_millis < :cutoffEpochMillis " +
            "AND audio_file_path != ''"
    )
    suspend fun getExpired(status: CaptureStatus, cutoffEpochMillis: Long): List<CaptureEntity>

    /**
     * Atomically claims a capture for processing: only succeeds (returns true) if the
     * capture is currently in a retryable state, and immediately moves it to
     * TRANSCRIBING so a concurrent caller can't also claim it. This is what prevents
     * duplicate processing of the same capture.
     */
    @Transaction
    suspend fun claimForProcessing(id: String, updatedAtEpochMillis: Long): Boolean {
        val current = getById(id) ?: return false
        if (!current.status.isRetryable()) return false
        update(current.copy(status = CaptureStatus.TRANSCRIBING, updatedAtEpochMillis = updatedAtEpochMillis, errorMessage = null))
        return true
    }
}
