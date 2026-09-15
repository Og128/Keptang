package com.keptang.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every schema migration the app ships: v1 -> v2 (`budgets` table), v2 -> v3 (`categories`),
 * v3 -> v4 (`captures.is_manual`), v4 -> v5 (`expenses.notes`), v5 -> v6 (`recurring_expenses`),
 * v6 -> v7 (`expenses.recurring_expense_id`), v7 -> v8 (indices), v8 -> v9 (`tags` +
 * `expense_tags`) and v9 -> v10 (`learned_categories`).
 *
 * Real captures, expenses and budgets already exist on devices running earlier versions, so these
 * must actually preserve that data, not just avoid crashing on a fresh install - and
 * `runMigrationsAndValidate` additionally proves each migration leaves a schema Room accepts,
 * which is what stops an upgrade from bricking the app on launch.
 */
@RunWith(AndroidJUnit4::class)
class KeptangDatabaseMigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KeptangDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesExistingCapturesAndExpenses_andAddsBudgetsTable() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO captures
                    (id, captured_at_epoch_millis, time_zone_id, audio_file_path, duration_millis,
                     raw_transcript, status, error_message, created_at_epoch_millis, updated_at_epoch_millis)
                VALUES
                    ('c1', 1000, 'Asia/Bangkok', '/data/captures/c1.wav', 2000,
                     'fifty baht for coffee', 'PROCESSED', NULL, 1000, 1000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO expenses
                    (id, capture_id, amount_minor_units, currency_code, occurred_at_epoch_millis, time_zone_id,
                     category, account, payment_method, merchant, confidence, review_status,
                     created_at_epoch_millis, updated_at_epoch_millis)
                VALUES
                    ('e1', 'c1', 5000, 'THB', 1000, 'Asia/Bangkok',
                     'Coffee', NULL, NULL, 'Coffee shop', 1.0, 'APPROVED', 1000, 1000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, KeptangDatabase.MIGRATION_1_2)

        val budgetsTable = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'budgets'")
        assertTrue("budgets table must exist after migration", budgetsTable.moveToFirst())
        budgetsTable.close()

        val captureCursor = db.query("SELECT status FROM captures WHERE id = 'c1'")
        assertTrue(captureCursor.moveToFirst())
        assertEquals("PROCESSED", captureCursor.getString(captureCursor.getColumnIndexOrThrow("status")))
        captureCursor.close()

        val expenseCursor = db.query("SELECT amount_minor_units, category FROM expenses WHERE id = 'e1'")
        assertTrue(expenseCursor.moveToFirst())
        assertEquals(5000L, expenseCursor.getLong(expenseCursor.getColumnIndexOrThrow("amount_minor_units")))
        assertEquals("Coffee", expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("category")))
        expenseCursor.close()

        db.close()
    }

    @Test
    fun migrate2To3_addsCategoriesTable_seededWithSixDefaults() {
        helper.createDatabase(testDbName, 2).apply { close() }

        val db = helper.runMigrationsAndValidate(testDbName, 3, true, KeptangDatabase.MIGRATION_2_3)

        val categoriesTable = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'categories'")
        assertTrue("categories table must exist after migration", categoriesTable.moveToFirst())
        categoriesTable.close()

        val countCursor = db.query("SELECT COUNT(*) FROM categories")
        assertTrue(countCursor.moveToFirst())
        assertEquals(6, countCursor.getInt(0))
        countCursor.close()

        val coffeeCursor = db.query("SELECT color_hex, icon_key FROM categories WHERE name = 'Coffee'")
        assertTrue(coffeeCursor.moveToFirst())
        assertEquals("#1baf7a", coffeeCursor.getString(coffeeCursor.getColumnIndexOrThrow("color_hex")))
        assertEquals("cafe", coffeeCursor.getString(coffeeCursor.getColumnIndexOrThrow("icon_key")))
        coffeeCursor.close()

        db.close()
    }

    @Test
    fun migrate3To4_addsIsManualColumn_defaultingExistingCapturesToFalse() {
        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                """
                INSERT INTO captures
                    (id, captured_at_epoch_millis, time_zone_id, audio_file_path, duration_millis,
                     raw_transcript, status, error_message, created_at_epoch_millis, updated_at_epoch_millis)
                VALUES
                    ('c1', 1000, 'Asia/Bangkok', '/data/captures/c1.wav', 2000,
                     'fifty baht for coffee', 'PROCESSED', NULL, 1000, 1000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 4, true, KeptangDatabase.MIGRATION_3_4)

        val cursor = db.query("SELECT is_manual FROM captures WHERE id = 'c1'")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_manual")))
        cursor.close()

        db.close()
    }

    @Test
    fun migrate4To5_addsNotesColumn_defaultingExistingExpensesToNull() {
        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                """
                INSERT INTO captures
                    (id, captured_at_epoch_millis, time_zone_id, audio_file_path, duration_millis,
                     raw_transcript, status, error_message, created_at_epoch_millis, updated_at_epoch_millis, is_manual)
                VALUES
                    ('c1', 1000, 'Asia/Bangkok', '/data/captures/c1.wav', 2000,
                     'fifty baht for coffee', 'PROCESSED', NULL, 1000, 1000, 0)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO expenses
                    (id, capture_id, amount_minor_units, currency_code, occurred_at_epoch_millis, time_zone_id,
                     category, account, payment_method, merchant, confidence, review_status,
                     created_at_epoch_millis, updated_at_epoch_millis)
                VALUES
                    ('e1', 'c1', 5000, 'THB', 1000, 'Asia/Bangkok',
                     'Coffee', NULL, NULL, 'Coffee shop', 1.0, 'APPROVED', 1000, 1000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, KeptangDatabase.MIGRATION_4_5)

        val cursor = db.query("SELECT notes FROM expenses WHERE id = 'e1'")
        assertTrue(cursor.moveToFirst())
        assertTrue("existing rows should default notes to NULL", cursor.isNull(cursor.getColumnIndexOrThrow("notes")))
        cursor.close()

        db.close()
    }

    @Test
    fun migrate5To6_addsRecurringExpensesTable() {
        helper.createDatabase(testDbName, 5).apply { close() }

        val db = helper.runMigrationsAndValidate(testDbName, 6, true, KeptangDatabase.MIGRATION_5_6)

        val table = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'recurring_expenses'")
        assertTrue("recurring_expenses table must exist after migration", table.moveToFirst())
        table.close()

        db.close()
    }

    @Test
    fun migrate6To7_addsRecurringExpenseIdColumn_defaultingExistingExpensesToNull() {
        helper.createDatabase(testDbName, 6).apply {
            execSQL(seedCapture("c1"))
            execSQL(seedExpense("e1", "c1"))
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 7, true, KeptangDatabase.MIGRATION_6_7)

        val cursor = db.query("SELECT recurring_expense_id FROM expenses WHERE id = 'e1'")
        assertTrue(cursor.moveToFirst())
        assertTrue(
            "expenses that predate recurrences should not be linked to one",
            cursor.isNull(cursor.getColumnIndexOrThrow("recurring_expense_id"))
        )
        cursor.close()

        db.close()
    }

    @Test
    fun migrate7To8_addsIndices_andKeepsExistingRows() {
        helper.createDatabase(testDbName, 7).apply {
            execSQL(seedCapture("c1"))
            execSQL(seedExpense("e1", "c1"))
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 8, true, KeptangDatabase.MIGRATION_7_8)

        for (index in listOf(
            "index_expenses_category",
            "index_expenses_review_status",
            "index_recurring_expenses_category",
            "index_budgets_category"
        )) {
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = '$index'")
            assertTrue("$index must exist after migration", cursor.moveToFirst())
            cursor.close()
        }

        val expense = db.query("SELECT category FROM expenses WHERE id = 'e1'")
        assertTrue(expense.moveToFirst())
        assertEquals("Coffee", expense.getString(expense.getColumnIndexOrThrow("category")))
        expense.close()

        db.close()
    }

    /**
     * The riskiest migration so far: two brand-new tables, a foreign key and two indices, which
     * Room validates column-by-column against [ExpenseTagCrossRef]/[TagEntity] on every upgrade -
     * a mismatch here bricks the app on launch rather than failing quietly.
     */
    @Test
    fun migrate8To9_addsTagTables_withCascadeFromExpenses() {
        helper.createDatabase(testDbName, 8).apply {
            execSQL(seedCapture("c1"))
            execSQL(seedExpense("e1", "c1"))
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 9, true, KeptangDatabase.MIGRATION_8_9)

        db.execSQL("INSERT INTO tags (name) VALUES ('Japan trip')")
        db.execSQL("INSERT INTO expense_tags (expense_id, tag) VALUES ('e1', 'Japan trip')")

        val tagged = db.query("SELECT tag FROM expense_tags WHERE expense_id = 'e1'")
        assertTrue(tagged.moveToFirst())
        assertEquals("Japan trip", tagged.getString(tagged.getColumnIndexOrThrow("tag")))
        tagged.close()

        // The cross-ref's ON DELETE CASCADE only fires with foreign keys enabled, which Room does
        // for the real database but MigrationTestHelper's handle does not.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM expenses WHERE id = 'e1'")

        val orphans = db.query("SELECT COUNT(*) FROM expense_tags WHERE expense_id = 'e1'")
        assertTrue(orphans.moveToFirst())
        assertEquals("deleting an expense must take its tag links with it", 0, orphans.getInt(0))
        orphans.close()

        db.close()
    }

    @Test
    fun migrate9To10_addsLearnedCategoriesTable_keyedByKeywordSoRelearningOverwrites() {
        helper.createDatabase(testDbName, 9).apply { close() }

        val db = helper.runMigrationsAndValidate(testDbName, 10, true, KeptangDatabase.MIGRATION_9_10)

        db.execSQL("INSERT OR REPLACE INTO learned_categories (keyword, category, updated_at_epoch_millis) VALUES ('massage', 'Entertainment', 1000)")
        db.execSQL("INSERT OR REPLACE INTO learned_categories (keyword, category, updated_at_epoch_millis) VALUES ('massage', 'Health', 2000)")

        val cursor = db.query("SELECT category, COUNT(*) AS rows FROM learned_categories WHERE keyword = 'massage'")
        assertTrue(cursor.moveToFirst())
        assertEquals("changing your mind must replace the lesson, not add a second one", 1, cursor.getInt(cursor.getColumnIndexOrThrow("rows")))
        assertEquals("Health", cursor.getString(cursor.getColumnIndexOrThrow("category")))
        cursor.close()

        db.close()
    }

    private fun seedCapture(id: String) =
        """
        INSERT INTO captures
            (id, captured_at_epoch_millis, time_zone_id, audio_file_path, duration_millis,
             raw_transcript, status, error_message, created_at_epoch_millis, updated_at_epoch_millis, is_manual)
        VALUES
            ('$id', 1000, 'Asia/Bangkok', '/data/captures/$id.wav', 2000,
             'fifty baht for coffee', 'PROCESSED', NULL, 1000, 1000, 0)
        """.trimIndent()

    private fun seedExpense(id: String, captureId: String) =
        """
        INSERT INTO expenses
            (id, capture_id, amount_minor_units, currency_code, occurred_at_epoch_millis, time_zone_id,
             category, account, payment_method, merchant, notes, confidence, review_status,
             created_at_epoch_millis, updated_at_epoch_millis)
        VALUES
            ('$id', '$captureId', 5000, 'THB', 1000, 'Asia/Bangkok',
             'Coffee', NULL, NULL, 'Coffee shop', NULL, 1.0, 'APPROVED', 1000, 1000)
        """.trimIndent()
}
