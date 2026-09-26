package com.keptang.export

import com.keptang.data.db.ExpenseEntity
import com.keptang.data.db.PaymentMethod
import com.keptang.data.db.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * [CsvExporter.buildCsv] is the whole export apart from writing the file, and the file lands in
 * a spreadsheet the user reads by hand - so escaping and column order matter more here than in
 * UI code, where a mistake is at least visible.
 */
class CsvExporterTest {

    private val zone = "Asia/Bangkok"

    private fun epochOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun expense(
        id: String,
        date: LocalDate = LocalDate.of(2026, 3, 14),
        amountMinorUnits: Long = 5000,
        currencyCode: String = "THB",
        category: String = "Coffee",
        description: String? = "Coffee shop",
        accountId: String? = "acc-cash",
        paymentMethod: PaymentMethod? = null,
        notes: String? = null
    ) = ExpenseEntity(
        id = id,
        captureId = "c-$id",
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        occurredAtEpochMillis = epochOf(date),
        timeZoneId = zone,
        category = category,
        accountId = accountId,
        paymentMethod = paymentMethod,
        description = description,
        notes = notes,
        confidence = 1.0f,
        reviewStatus = ReviewStatus.APPROVED,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0
    )

    @Test
    fun buildsHeaderAndOneRowPerExpense() {
        // The export names the account rather than printing its id, which is meaningless to
        // whoever opens the file.
        val csv = CsvExporter.buildCsv(listOf(expense("e1")), emptyMap(), mapOf("acc-cash" to "Cash"))

        val lines = csv.split("\r\n")
        assertEquals(2, lines.size)
        assertEquals("Date,Amount,Currency,Category,Tags,Description,Account,Payment method,Notes", lines[0])
        assertEquals("2026-03-14,50.00,THB,Coffee,,Coffee shop,Cash,,", lines[1])
    }

    @Test
    fun ordersRowsNewestFirst() {
        val csv = CsvExporter.buildCsv(
            listOf(
                expense("older", date = LocalDate.of(2026, 1, 1)),
                expense("newer", date = LocalDate.of(2026, 6, 30))
            ),
            emptyMap()
        )

        val dates = csv.split("\r\n").drop(1).map { it.substringBefore(',') }
        assertEquals(listOf("2026-06-30", "2026-01-01"), dates)
    }

    @Test
    fun joinsMultipleTagsWithSemicolons_soCommasStayColumnSeparators() {
        val csv = CsvExporter.buildCsv(
            listOf(expense("e1")),
            mapOf("e1" to listOf("Japan trip", "Work"))
        )

        assertTrue(csv, csv.contains(",Japan trip;Work,"))
    }

    @Test
    fun quotesAndDoublesUpFieldsContainingCommasOrQuotes() {
        val csv = CsvExporter.buildCsv(
            listOf(expense("e1", description = "Bread, Butter & Co", notes = "said \"keep the change\"")),
            emptyMap()
        )

        val row = csv.split("\r\n")[1]
        assertTrue(row, row.contains("\"Bread, Butter & Co\""))
        assertTrue(row, row.contains("\"said \"\"keep the change\"\"\""))
    }

    @Test
    fun keepsRowsAlignedWhenOptionalFieldsAreMissing() {
        val csv = CsvExporter.buildCsv(
            listOf(expense("e1", description = null, accountId = null, paymentMethod = null, notes = null)),
            emptyMap()
        )

        val row = csv.split("\r\n")[1]
        assertEquals("every column must still be present, just empty", 8, row.count { it == ',' })
    }

    @Test
    fun formatsZeroDecimalCurrenciesWithoutDecimals() {
        val csv = CsvExporter.buildCsv(
            listOf(expense("e1", amountMinorUnits = 1200, currencyCode = "JPY")),
            emptyMap()
        )

        assertTrue(csv, csv.contains(",1200,JPY,"))
    }

    @Test
    fun emitsHeaderOnlyWhenThereIsNothingToExport() {
        val csv = CsvExporter.buildCsv(emptyList(), emptyMap())

        assertEquals("Date,Amount,Currency,Category,Tags,Description,Account,Payment method,Notes", csv)
    }
}
