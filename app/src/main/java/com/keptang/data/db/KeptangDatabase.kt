package com.keptang.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        CaptureEntity::class, ExpenseEntity::class, BudgetEntity::class, CategoryEntity::class,
        RecurringExpenseEntity::class, TagEntity::class, ExpenseTagCrossRef::class,
        LearnedCategoryEntity::class, AccountEntity::class, AccountTransferEntity::class
    ],
    version = 11,
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
    abstract fun learnedCategoryDao(): LearnedCategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun accountTransferDao(): AccountTransferDao

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
         * Adds [LearnedCategoryEntity]: the keyword -> category lessons the parser picks up from
         * the user correcting a category by hand.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `learned_categories` (
                        `keyword` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`keyword`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Turns accounts into first-class records. Before this, an expense carried an account as
         * free text ("K-bank") next to a payment method as free text ("Cash", "PromptPay"), and
         * nothing tied either to anything.
         *
         * Three things happen, in order:
         * 1. `accounts` and `account_transfers` appear, with a cash wallet always seeded - it is
         *    the account that carries a balance, so it has to exist even on a database where
         *    nobody ever typed the word.
         * 2. Every distinct account name already on an expense becomes a [AccountKind.BANK]
         *    account, and the expenses referencing it by name are rewritten to its id in place.
         *    Nothing is dropped; a name that turns out to be junk is deleted from the Profile
         *    screen afterwards.
         * 3. Payment methods are normalised onto [PaymentMethod]. "Cash" is the interesting case:
         *    it is not a method at all under the new model, so those expenses move onto the cash
         *    wallet and lose the method instead.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `color_hex` TEXT NOT NULL,
                        `payment_methods` TEXT NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `account_transfers` (
                        `id` TEXT NOT NULL,
                        `from_account_id` TEXT,
                        `to_account_id` TEXT NOT NULL,
                        `amount_minor_units` INTEGER NOT NULL,
                        `currency_code` TEXT NOT NULL,
                        `occurred_at_epoch_millis` INTEGER NOT NULL,
                        `time_zone_id` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `note` TEXT,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`from_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`to_account_id`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_account_transfers_from_account_id` ON `account_transfers` (`from_account_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_account_transfers_to_account_id` ON `account_transfers` (`to_account_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_account` ON `expenses` (`account`)")

                val cashId = seedCashWallet(db)

                // The bank accounts the user has been typing by hand all along. Anything spelled
                // like the wallet is folded into it rather than becoming a second "cash".
                var sortOrder = 1
                db.query("SELECT DISTINCT `account` FROM `expenses` WHERE `account` IS NOT NULL AND TRIM(`account`) <> ''")
                    .use { cursor ->
                        val names = mutableListOf<String>()
                        while (cursor.moveToNext()) names += cursor.getString(0)
                        for (name in names) {
                            val targetId = if (name.trim().equals(CASH_WALLET_NAME, ignoreCase = true)) {
                                cashId
                            } else {
                                UUID.randomUUID().toString().also { id ->
                                    db.execSQL(
                                        "INSERT INTO `accounts` (`id`, `name`, `kind`, `color_hex`, `payment_methods`, `sort_order`) VALUES (?, ?, ?, ?, ?, ?)",
                                        arrayOf(
                                            id,
                                            name.trim(),
                                            AccountKind.BANK.name,
                                            ACCOUNT_COLORS[sortOrder % ACCOUNT_COLORS.size],
                                            PaymentMethod.encodeList(PaymentMethod.BANK_DEFAULTS),
                                            sortOrder++
                                        )
                                    )
                                }
                            }
                            db.execSQL("UPDATE `expenses` SET `account` = ? WHERE `account` = ?", arrayOf(targetId, name))
                        }
                    }

                // Cash was never a payment method under the new model - it is where the money is.
                db.execSQL(
                    "UPDATE `expenses` SET `account` = ? WHERE `payment_method` = 'Cash' AND (`account` IS NULL OR TRIM(`account`) = '')",
                    arrayOf(cashId)
                )
                db.execSQL("UPDATE `expenses` SET `payment_method` = 'CARD' WHERE `payment_method` = 'Card'")
                db.execSQL("UPDATE `expenses` SET `payment_method` = 'QR' WHERE `payment_method` = 'PromptPay'")
                db.execSQL("UPDATE `expenses` SET `payment_method` = 'TRANSFER' WHERE `payment_method` = 'Bank Transfer'")
                db.execSQL("UPDATE `expenses` SET `payment_method` = NULL WHERE `payment_method` NOT IN ('CARD', 'QR', 'TRANSFER')")
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
                seedCashWallet(db)
            }
        }

        /** The wallet's display name, and the spelling [MIGRATION_10_11] folds old free-text accounts into. */
        const val CASH_WALLET_NAME = "Cash"

        /**
         * Colours handed to accounts created without the user picking one - on a fresh install and
         * during [MIGRATION_10_11]. Duplicated from [com.keptang.ui.theme.CategoryColors] rather
         * than imported: the data layer does not depend on the UI layer anywhere else, and a
         * migration must keep producing the same bytes even if the palette is later restyled.
         */
        private val ACCOUNT_COLORS = listOf("#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300", "#4a3aa7", "#e34948")

        /**
         * Creates the cash wallet if it is missing and returns its id. Idempotent, because it runs
         * both from [MIGRATION_10_11] and from the fresh-install callback, and a database created
         * at the current version never sees the migration.
         */
        private fun seedCashWallet(db: SupportSQLiteDatabase): String {
            db.query("SELECT `id` FROM `accounts` WHERE `kind` = '${AccountKind.CASH.name}' LIMIT 1").use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }
            val id = UUID.randomUUID().toString()
            db.execSQL(
                "INSERT INTO `accounts` (`id`, `name`, `kind`, `color_hex`, `payment_methods`, `sort_order`) VALUES (?, ?, ?, ?, '', 0)",
                arrayOf(id, CASH_WALLET_NAME, AccountKind.CASH.name, ACCOUNT_COLORS[3])
            )
            return id
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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
                    )
                    .addCallback(SEED_CATEGORIES_CALLBACK)
                    .build().also { instance = it }
            }
    }
}

private data class Quadruple(val name: String, val colorHex: String, val iconKey: String, val sortOrder: Int)
