package com.keptang.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CaptureEntity::class, ExpenseEntity::class, BudgetEntity::class, CategoryEntity::class,
        RecurringExpenseEntity::class, TagEntity::class, ExpenseTagCrossRef::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KeptangDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` TEXT NOT NULL,
                        `category` TEXT,
                        `amount_minor_units` INTEGER NOT NULL,
                        `period_type` TEXT NOT NULL,
                        `period_anchor` INTEGER NOT NULL,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Replaces the free-text category list previously stored in DataStore
         * ([com.keptang.data.repository.SettingsRepository]) with a proper table carrying a
         * color + icon per category, seeded with the same six defaults so existing
         * expenses/budgets referencing them by name keep working unchanged.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `name` TEXT NOT NULL,
                        `color_hex` TEXT NOT NULL,
                        `icon_key` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`name`)
                    )
                    """.trimIndent()
                )
                seedDefaultCategories(db)
            }
        }

        /**
         * Adds the `is_manual` flag distinguishing hand-typed captures (created by
         * [com.keptang.data.repository.CaptureRepository.createManualEntry]) from real voice
         * captures, so editing an Expense can warn when it would leave a linked transcript
         * behind unchanged.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `captures` ADD COLUMN `is_manual` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds a free-text `notes` field to expenses, editable from the Add/Edit Expense screen. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `notes` TEXT")
            }
        }

        /** Adds the `recurring_expenses` table backing subscriptions (Netflix, rent, etc.) that auto-generate an [ExpenseEntity] each time they come due. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount_minor_units` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `period_type` TEXT NOT NULL,
                        `period_anchor` INTEGER NOT NULL,
                        `next_due_at_epoch_millis` INTEGER NOT NULL,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** Tags an [ExpenseEntity] with the [RecurringExpenseEntity] that generated it, so the ledger can badge it as recurring. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `recurring_expense_id` TEXT")
            }
        }

        /** Adds indices on the columns most frequently filtered on (category rename cascades, review queues), which had none before. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_review_status` ON `expenses` (`review_status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_category` ON `recurring_expenses` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_category` ON `budgets` (`category`)")
            }
        }

        /** Adds free-form tags: a [TagEntity] name table plus the [ExpenseTagCrossRef] many-to-many join to expenses. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`name`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_tags` (
                        `expense_id` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        PRIMARY KEY(`expense_id`, `tag`),
                        FOREIGN KEY(`expense_id`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tags_expense_id` ON `expense_tags` (`expense_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tags_tag` ON `expense_tags` (`tag`)")
            }
        }

        /**
         * Seeds the same six default categories as [MIGRATION_2_3], for a brand-new install:
         * migrations only run when upgrading an *existing* database file, so a fresh install
         * (Room creates the schema straight at the current version) would otherwise end up
         * with an empty categories table.
         */
        private val SEED_CATEGORIES_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDefaultCategories(db)
            }
        }

        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            val defaults = listOf(
                Quadruple("Transport", "#2a78d6", "car", 0),
                Quadruple("Dining", "#eb6834", "restaurant", 1),
                Quadruple("Coffee", "#1baf7a", "cafe", 2),
                Quadruple("Groceries", "#eda100", "cart", 3),
                Quadruple("Housing", "#e87ba4", "home", 4),
                Quadruple("Utilities", "#008300", "bolt", 5)
            )
            for ((name, colorHex, iconKey, sortOrder) in defaults) {
                db.execSQL(
                    "INSERT INTO `categories` (`name`, `color_hex`, `icon_key`, `sort_order`) VALUES (?, ?, ?, ?)",
                    arrayOf(name, colorHex, iconKey, sortOrder)
                )
            }
        }

        @Volatile
        private var instance: KeptangDatabase? = null

        fun getInstance(context: Context): KeptangDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeptangDatabase::class.java,
                    "keptang.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .addCallback(SEED_CATEGORIES_CALLBACK)
                    .build().also { instance = it }
            }
    }
}

private data class Quadruple(val name: String, val colorHex: String, val iconKey: String, val sortOrder: Int)
