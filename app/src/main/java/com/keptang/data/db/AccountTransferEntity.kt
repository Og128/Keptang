package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Money moving between accounts, kept apart from `expenses` on purpose: a transfer must never
 * reach a category total, a budget or the spending charts. See [TransferKind].
 *
 * In practice only the cash wallet is ever the destination today, since it is the only account
 * carrying a balance, but the table is shaped for any pair so a second tracked account later
 * needs no migration.
 */
@Entity(
    tableName = "account_transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("from_account_id"), Index("to_account_id")]
)
data class AccountTransferEntity(
    @PrimaryKey val id: String,
    /** Null for a [TransferKind.ADJUSTMENT], which corrects a balance rather than moving money from somewhere. */
    @ColumnInfo(name = "from_account_id") val fromAccountId: String?,
    @ColumnInfo(name = "to_account_id") val toAccountId: String,
    /** Signed, so a [TransferKind.ADJUSTMENT] can take money off a wallet that was overstated. */
    @ColumnInfo(name = "amount_minor_units") val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "occurred_at_epoch_millis") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "kind") val kind: TransferKind,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long
)
