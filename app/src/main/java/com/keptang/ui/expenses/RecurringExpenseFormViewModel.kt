package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.RecurringExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import com.keptang.recurring.RecurringExpenseGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Edit-only: new recurring definitions are created from [ManualExpenseScreen]'s add-mode toggle instead. */
class RecurringExpenseFormViewModel(
    private val recurringId: String,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    private val recurringExpenseGenerator: RecurringExpenseGenerator
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val existing: StateFlow<RecurringExpenseEntity?> = flow { emit(recurringExpenseRepository.getById(recurringId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(
        name: String,
        amountMinorUnits: Long,
        category: String,
        periodType: BudgetPeriodType,
        periodAnchor: Int,
        timeZoneId: String,
        currencyCode: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            recurringExpenseRepository.update(recurringId, name, amountMinorUnits, category, periodType, periodAnchor, timeZoneId)
            recurringExpenseGenerator.generateDueExpenses(timeZoneId, currencyCode)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            recurringExpenseRepository.delete(recurringId)
            onDeleted()
        }
    }

    companion object {
        fun factory(recurringId: String) = viewModelFactory {
            initializer {
                RecurringExpenseFormViewModel(
                    recurringId,
                    ServiceLocator.recurringExpenseRepository,
                    ServiceLocator.settingsRepository,
                    ServiceLocator.categoryRepository,
                    ServiceLocator.recurringExpenseGenerator
                )
            }
        }
    }
}
