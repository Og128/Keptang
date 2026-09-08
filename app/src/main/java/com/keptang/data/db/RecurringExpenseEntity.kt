package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring expense definition (e.g. a subscription): generates a real [ExpenseEntity] every
 * time [nextDueAtEpochMillis] is reached, then advances that date by one more period. Carries no
 * currency of its own - generated expenses always use the Default Currency at generation time,
 * same as [BudgetEntity] (see ADR-0002).
 */
@Entity(tableName = "recurring_expenses", indices = [Index(value = ["category"], name = "index_recurring_expenses_category")])
data class RecurringExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount_minor_units") val amountMinorUnits: Long,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "period_type") val periodType: BudgetPeriodType,
    @ColumnInfo(name = "period_anchor") val periodAnchor: Int,
    @ColumnInfo(name = "next_due_at_epoch_millis") val nextDueAtEpochMillis: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
