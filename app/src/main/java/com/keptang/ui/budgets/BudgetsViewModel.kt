package com.keptang.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.budget.BudgetCalculator
import com.keptang.budget.BudgetSnapshot
import com.keptang.data.repository.BudgetRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import com.keptang.core.Defaults
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class BudgetsViewModel(
    budgetRepository: BudgetRepository,
    expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val snapshot: StateFlow<BudgetSnapshot> = combine(
        budgetRepository.observeAll(),
        expenseRepository.observeApproved(),
        settingsRepository.settings
    ) { budgets, expenses, settings ->
        BudgetCalculator.compute(
            budgets = budgets,
            approvedExpenses = expenses,
            defaultCurrencyCode = settings.currencyCode,
            today = LocalDate.now(ZoneId.of(settings.timeZoneId))
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetSnapshot(null, null, emptyList(), Defaults.CURRENCY_CODE)
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                BudgetsViewModel(
                    ServiceLocator.budgetRepository,
                    ServiceLocator.expenseRepository,
                    ServiceLocator.settingsRepository
                )
            }
        }
    }
}
