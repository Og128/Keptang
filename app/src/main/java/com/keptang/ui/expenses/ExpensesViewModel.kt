package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.core.Defaults
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ExpensesViewModel(
    expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseEntity>> = expenseRepository.observeApproved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Category name -> its color/icon, for the ledger's per-category tinting. */
    val categoriesByName: StateFlow<Map<String, CategoryEntity>> = categoryRepository.observeAll()
        .map { categories -> categories.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val timeZoneId: StateFlow<String> = settingsRepository.settings
        .map { it.timeZoneId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Defaults.TIME_ZONE_ID)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ExpensesViewModel(
                    ServiceLocator.expenseRepository,
                    ServiceLocator.categoryRepository,
                    ServiceLocator.settingsRepository
                )
            }
        }
    }
}
