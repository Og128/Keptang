package com.keptang.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.BudgetEntity
import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.BudgetRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Budget targets still available to create a new Budget for: Overall (if not already set) plus any category without a Budget yet. */
data class AvailableTargets(val overallAvailable: Boolean, val availableCategories: List<String>)

class BudgetFormViewModel(
    private val budgetId: String?,
    private val budgetRepository: BudgetRepository,
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /** Null in add mode; the Budget being edited otherwise. */
    val existing: StateFlow<BudgetEntity?> = flow { emit(budgetId?.let { budgetRepository.getById(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableTargets: StateFlow<AvailableTargets> = combine(
        budgetRepository.observeAll(),
        expenseRepository.observeDistinctCategories(),
        settingsRepository.settings
    ) { budgets, usedCategories, settings ->
        val budgetedCategories = budgets.mapNotNull { it.category }.toSet()
        val allCategories = (settings.categories + usedCategories).distinct().sorted()
        AvailableTargets(
            overallAvailable = budgets.none { it.category == null },
            availableCategories = allCategories.filter { it !in budgetedCategories }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AvailableTargets(false, emptyList()))

    fun save(
        target: String?,
        amountMinorUnits: Long,
        periodType: BudgetPeriodType,
        periodAnchor: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val id = budgetId
            if (id == null) {
                budgetRepository.create(target, amountMinorUnits, periodType, periodAnchor)
            } else {
                budgetRepository.updateAmountAndPeriod(id, amountMinorUnits, periodType, periodAnchor)
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = budgetId ?: return
        viewModelScope.launch {
            budgetRepository.delete(id)
            onDeleted()
        }
    }

    companion object {
        fun factory(budgetId: String?) = viewModelFactory {
            initializer {
                BudgetFormViewModel(
                    budgetId,
                    ServiceLocator.budgetRepository,
                    ServiceLocator.expenseRepository,
                    ServiceLocator.settingsRepository
                )
            }
        }
    }
}
