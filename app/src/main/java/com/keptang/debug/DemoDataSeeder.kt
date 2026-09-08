package com.keptang.debug

import com.keptang.core.Defaults
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Debug-only helper (see Settings' "Seed demo data" button, hidden behind BuildConfig.DEBUG)
 * that fills the ledger with plausible fake expenses so the ledger, calendar, budgets and
 * Dashboard screens can be exercised without speaking or typing dozens of entries by hand.
 * Every seeded row is a manual entry like any other, so editing/deleting it works exactly the
 * same as expenses added by hand.
 */
object DemoDataSeeder {

    private val MERCHANTS_BY_CATEGORY = mapOf(
        "Transport" to listOf("BTS Skytrain", "Grab", "Taxi", "Gas station"),
        "Dining" to listOf("Som Tam Stall", "Noodle Shop", "Pizza Company", "Night Market"),
        "Coffee" to listOf("Amazon Coffee", "Starbucks", "Local Cafe"),
        "Groceries" to listOf("Big C", "Tesco Lotus", "7-Eleven", "Villa Market"),
        "Housing" to listOf("Landlord", "Condo Fee"),
        "Utilities" to listOf("Electricity Co.", "Water Authority", "True Internet")
    )

    private val PAYMENT_METHODS = listOf("Cash", "Credit Card", "QR Pay", null)
    private const val SPREAD_DAYS = 90L
    private const val MIN_MAJOR_AMOUNT = 20
    private const val MAX_MAJOR_AMOUNT = 3000

    suspend fun seed(
        count: Int,
        captureRepository: CaptureRepository,
        expenseRepository: ExpenseRepository,
        categoryRepository: CategoryRepository,
        settingsRepository: SettingsRepository
    ) {
        val settings = settingsRepository.settings.first()
        val categories = categoryRepository.observeAll().first()
        if (categories.isEmpty()) return

        val exponent = Defaults.minorUnitExponent(settings.currencyCode)
        repeat(count) {
            val category = categories.random()
            val majorAmount = Random.nextInt(MIN_MAJOR_AMOUNT, MAX_MAJOR_AMOUNT)
            val amountMinorUnits = (majorAmount * 10.0.pow(exponent)).roundToLong()
            val occurredAtEpochMillis = Instant.now()
                .minus(Random.nextLong(0, SPREAD_DAYS), ChronoUnit.DAYS)
                .toEpochMilli()

            val captureId = captureRepository.createManualEntry(settings.timeZoneId)
            expenseRepository.createManual(
                captureId = captureId,
                amountMinorUnits = amountMinorUnits,
                currencyCode = settings.currencyCode,
                occurredAtEpochMillis = occurredAtEpochMillis,
                timeZoneId = settings.timeZoneId,
                category = category.name,
                account = settings.defaultAccount,
                paymentMethod = PAYMENT_METHODS.random(),
                merchant = MERCHANTS_BY_CATEGORY[category.name]?.randomOrNull()
            )
        }
    }
}
