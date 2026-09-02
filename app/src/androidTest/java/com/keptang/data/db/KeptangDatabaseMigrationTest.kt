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
 * The app's first-ever schema migration (v1 -> v2, adding the `budgets` table). Real captures
 * and expenses already exist on developer devices running v1, so this must actually preserve
 * that data, not just avoid crashing on a fresh install.
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
}
