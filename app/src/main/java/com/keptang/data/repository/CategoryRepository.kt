package com.keptang.data.repository

import com.keptang.data.db.BudgetDao
import com.keptang.data.db.CategoryDao
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseDao
import com.keptang.data.db.RecurringExpenseDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
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
     * every expense and budget referencing the old name, since they hold the category as a plain
     * string rather than a foreign key (matching how budgets already reference categories).
     */
    suspend fun update(oldName: String, newName: String, colorHex: String, iconKey: String) {
        val current = categoryDao.getByName(oldName) ?: return
        if (newName != oldName) {
            expenseDao.renameCategory(oldName, newName)
            budgetDao.renameCategory(oldName, newName)
            recurringExpenseDao.renameCategory(oldName, newName)
            categoryDao.deleteByName(oldName)
        }
        categoryDao.upsert(current.copy(name = newName, colorHex = colorHex, iconKey = iconKey))
    }

    suspend fun delete(name: String) = categoryDao.deleteByName(name)
}
