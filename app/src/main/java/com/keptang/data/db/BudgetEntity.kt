package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Budget: either the Overall Budget ([category] null) or a Category Budget. Always compared
 * against the live Default Currency setting at read time - deliberately has no currency of its
 * own (see ADR-0002).
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "amount_minor_units") val amountMinorUnits: Long,
    @ColumnInfo(name = "period_type") val periodType: BudgetPeriodType,
    @ColumnInfo(name = "period_anchor") val periodAnchor: Int,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
