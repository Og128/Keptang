package com.keptang.data.repository

import com.keptang.data.db.BudgetDao
import com.keptang.data.db.BudgetEntity
import com.keptang.data.db.BudgetPeriodType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun observeAll(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    suspend fun getById(id: String): BudgetEntity? = budgetDao.getById(id)

    /**
     * Creates a Budget for [category] (null = Overall). Only one Budget may exist per target -
     * enforced here rather than via a DB constraint, since SQLite's UNIQUE index treats distinct
     * NULLs as non-duplicates, which would let multiple Overall Budgets slip through.
     */
    suspend fun create(
        category: String?,
        amountMinorUnits: Long,
        periodType: BudgetPeriodType,
        periodAnchor: Int
    ): BudgetEntity {
        check(budgetDao.getAll().none { it.category == category }) {
            "A budget already exists for ${category ?: "Overall"}"
        }
        val now = Instant.now().toEpochMilli()
        val entity = BudgetEntity(
            id = UUID.randomUUID().toString(),
            category = category,
            amountMinorUnits = amountMinorUnits,
            periodType = periodType,
            periodAnchor = periodAnchor,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        budgetDao.insert(entity)
        return entity
    }

    suspend fun updateAmountAndPeriod(
        id: String,
        amountMinorUnits: Long,
        periodType: BudgetPeriodType,
        periodAnchor: Int
    ) {
        val current = budgetDao.getById(id) ?: return
        budgetDao.update(
            current.copy(
                amountMinorUnits = amountMinorUnits,
                periodType = periodType,
                periodAnchor = periodAnchor,
                updatedAtEpochMillis = Instant.now().toEpochMilli()
            )
        )
    }

    suspend fun delete(id: String) = budgetDao.deleteById(id)
}
