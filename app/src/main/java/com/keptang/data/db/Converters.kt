package com.keptang.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromCaptureStatus(value: CaptureStatus): String = value.name

    @TypeConverter
    fun toCaptureStatus(value: String): CaptureStatus = CaptureStatus.valueOf(value)

    @TypeConverter
    fun fromReviewStatus(value: ReviewStatus): String = value.name

    @TypeConverter
    fun toReviewStatus(value: String): ReviewStatus = ReviewStatus.valueOf(value)

    @TypeConverter
    fun fromBudgetPeriodType(value: BudgetPeriodType): String = value.name

    @TypeConverter
    fun toBudgetPeriodType(value: String): BudgetPeriodType = BudgetPeriodType.valueOf(value)

    @TypeConverter
    fun fromAccountKind(value: AccountKind): String = value.name

    @TypeConverter
    fun toAccountKind(value: String): AccountKind = AccountKind.valueOf(value)

    @TypeConverter
    fun fromTransferKind(value: TransferKind): String = value.name

    @TypeConverter
    fun toTransferKind(value: String): TransferKind = TransferKind.valueOf(value)

    /**
     * Tolerant on the way in, unlike the [valueOf] converters above: the column used to hold
     * free text the parser wrote ("PromptPay", "Bank Transfer"), and [com.keptang.data.db.KeptangDatabase.MIGRATION_10_11]
     * maps the shapes it knows and nulls the rest - but a value it never anticipated must read
     * back as "no method", not crash the ledger.
     */
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { stored -> PaymentMethod.entries.firstOrNull { it.name == stored } }

    @TypeConverter
    fun fromPaymentMethods(value: List<PaymentMethod>): String = PaymentMethod.encodeList(value)

    @TypeConverter
    fun toPaymentMethods(value: String): List<PaymentMethod> = PaymentMethod.parseList(value)
}
