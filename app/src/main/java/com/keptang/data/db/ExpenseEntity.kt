package com.keptang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["capture_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("capture_id"), Index(value = ["category"], name = "index_expenses_category"), Index(value = ["review_status"], name = "index_expenses_review_status"), Index(value = ["account"], name = "index_expenses_account")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "capture_id") val captureId: String,
    @ColumnInfo(name = "amount_minor_units") val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "occurred_at_epoch_millis") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "category") val category: String,
    /**
     * The [AccountEntity] this was paid from. The column is still called `account` from when it
     * held the account's name as free text - the same reason `merchant` below keeps its name -
     * and [KeptangDatabase.MIGRATION_10_11] rewrote those names into ids in place.
     *
     * No foreign key: adding one to an existing column means recreating the whole table, which
     * would cascade into `expense_tags` and drop every tag link. Deletion is guarded in
     * [com.keptang.data.repository.AccountRepository] instead, the same way categories are.
     */
    @ColumnInfo(name = "account") val accountId: String?,
    /** Null on a cash-wallet expense, and on anything predating accounts. See [PaymentMethod]. */
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod?,
    /**
     * What the expense was for, as shown in the ledger: a merchant ("Starbucks"), a thing
     * ("Massage"), or whatever the user typed. The column is still called `merchant` from when
     * this only ever held one - renaming it would mean recreating the table, which would cascade
     * into `expense_tags` and drop every tag link, so the name stays put at the SQL level only.
     */
    @ColumnInfo(name = "merchant") val description: String?,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "recurring_expense_id") val recurringExpenseId: String? = null,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "review_status") val reviewStatus: ReviewStatus,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
