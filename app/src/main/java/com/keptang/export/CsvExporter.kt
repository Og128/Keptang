package com.keptang.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.keptang.data.db.ExpenseEntity
import com.keptang.ui.common.formatMoneyInput
import com.keptang.ui.common.localDateOf
import java.io.File
import java.time.format.DateTimeFormatter

private val EXPORT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

private val CSV_HEADER = listOf(
    "Date", "Amount", "Currency", "Category", "Tags", "Description", "Account", "Payment method", "Notes"
)

/** One-shot CSV export of every expense, sharing rather than saving directly - see [com.keptang.ui.settings.SettingsViewModel.exportExpenses]. */
object CsvExporter {

    fun buildCsv(
        expenses: List<ExpenseEntity>,
        tagsByExpenseId: Map<String, List<String>>
    ): String {
        val rows = expenses
            .sortedByDescending { it.occurredAtEpochMillis }
            .map { expense ->
                listOf(
                    localDateOf(expense.occurredAtEpochMillis, expense.timeZoneId).format(EXPORT_DATE_FORMATTER),
                    formatMoneyInput(expense.amountMinorUnits, expense.currencyCode),
                    expense.currencyCode,
                    expense.category,
                    tagsByExpenseId[expense.id].orEmpty().joinToString(";"),
                    expense.merchant.orEmpty(),
                    expense.account.orEmpty(),
                    expense.paymentMethod.orEmpty(),
                    expense.notes.orEmpty()
                )
            }
        return (listOf(CSV_HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { csvEscape(it) } }
    }

    private fun csvEscape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Writes [csv] under the cache dir declared in `file_paths.xml`, so [shareIntent] can hand it to another app via FileProvider. */
    fun writeToCache(context: Context, csv: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "keptang_export.csv")
        file.writeText(csv)
        return file
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
