package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One of the profile's accounts: "K-bank", "TTBK", "HSBC", or the cash wallet. This is what the
 * user picks when entering an expense - the account, not the payment method - and
 * [ExpenseEntity.accountId] points here.
 *
 * Unlike [CategoryEntity], which expenses reference loosely by name, accounts are referenced by
 * [id] so renaming "K-bank" to "Kasikorn" does not orphan a year of history.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "kind") val kind: AccountKind,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    /**
     * Which of card / QR / transfer this account offers, in the order the picker shows them.
     * Empty for a [AccountKind.CASH] wallet. Stored comma-encoded rather than in a junction
     * table: at most three values on a handful of accounts, and the order is free that way.
     */
    @ColumnInfo(name = "payment_methods") val paymentMethods: List<PaymentMethod>,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)
