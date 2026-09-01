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
    indices = [Index("capture_id")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "capture_id") val captureId: String,
    @ColumnInfo(name = "amount_minor_units") val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "occurred_at_epoch_millis") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "account") val account: String?,
    @ColumnInfo(name = "payment_method") val paymentMethod: String?,
    @ColumnInfo(name = "merchant") val merchant: String?,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "review_status") val reviewStatus: ReviewStatus,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long
)
