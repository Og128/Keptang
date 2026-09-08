package com.keptang.data.repository

import androidx.room.withTransaction
import com.keptang.data.db.BudgetDao
import com.keptang.data.db.CategoryDao
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseDao
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.db.RecurringExpenseDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val database: KeptangDatabase,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun getByName(name: String): CategoryEntity? = categoryDao.getByName(name)

    suspend fun create(name: String, colorHex: String, iconKey: String) {
        val sortOrder = (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        categoryDao.upsert(CategoryEntity(name, colorHex, iconKey, sortOrder))
    }

    /**
     * Updates [oldName]'s color/icon, and its name to [newName] if changed. A rename cascades to
     * every expense, budget and recurring expense referencing the old name, since they hold the
     * category as a plain string rather than a foreign key. Wrapped in one transaction so a
     * process death mid-rename can't leave some tables on the old name and others on the new one.
     */
    suspend fun update(oldName: String, newName: String, colorHex: String, iconKey: String) = database.withTransaction {
        val current = categoryDao.getByName(oldName) ?: return@withTransaction
        if (newName != oldName) {
            expenseDao.renameCategory(oldName, newName)
            budgetDao.renameCategory(oldName, newName)
            recurringExpenseDao.renameCategory(oldName, newName)
            categoryDao.deleteByName(oldName)
        }
        categoryDao.upsert(current.copy(name = newName, colorHex = colorHex, iconKey = iconKey))
    }

    /**
     * Refuses to delete a category still referenced by any expense, budget or recurring expense -
     * they hold it as a plain string, so deleting it out from under them would leave a "ghost"
     * category name with no color/icon rather than cascading cleanly.
     */
    suspend fun delete(name: String) = database.withTransaction {
        val usageCount = expenseDao.countByCategory(name) + budgetDao.countByCategory(name) + recurringExpenseDao.countByCategory(name)
        check(usageCount == 0) { "Category \"$name\" is still used by $usageCount item(s)" }
        categoryDao.deleteByName(name)
    }
}
