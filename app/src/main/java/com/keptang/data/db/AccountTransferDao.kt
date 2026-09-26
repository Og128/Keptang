package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountTransferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: AccountTransferEntity)

    /** Everything touching [accountId] in either direction - the wallet's side of its own balance. */
    @Query(
        """
        SELECT * FROM account_transfers
        WHERE from_account_id = :accountId OR to_account_id = :accountId
        ORDER BY occurred_at_epoch_millis DESC
        """
    )
    fun observeForAccount(accountId: String): Flow<List<AccountTransferEntity>>

    @Query("SELECT * FROM account_transfers ORDER BY occurred_at_epoch_millis DESC")
    fun observeAll(): Flow<List<AccountTransferEntity>>

    @Query("SELECT * FROM account_transfers WHERE id = :id")
    suspend fun getById(id: String): AccountTransferEntity?

    @Query("DELETE FROM account_transfers WHERE id = :id")
    suspend fun deleteById(id: String)
}
