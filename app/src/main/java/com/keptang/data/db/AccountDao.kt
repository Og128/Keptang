package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sort_order ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE kind = :kind ORDER BY sort_order ASC LIMIT 1")
    suspend fun getFirstOfKind(kind: AccountKind): AccountEntity?

    /** Guards deletion the way categories are guarded: an account still carrying history is not silently dropped. */
    @Query("SELECT COUNT(*) FROM expenses WHERE account = :accountId")
    suspend fun countExpenses(accountId: String): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}
