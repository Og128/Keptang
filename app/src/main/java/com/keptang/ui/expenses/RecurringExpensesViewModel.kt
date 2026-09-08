package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.RecurringExpenseEntity
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.RecurringExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringExpensesViewModel(
    private val recurringExpenseRepository: RecurringExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> = recurringExpenseRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categoriesByName: StateFlow<Map<String, CategoryEntity>> = categoryRepository.observeAll()
        .map { categories -> categories.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun delete(id: String) {
        viewModelScope.launch { recurringExpenseRepository.delete(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RecurringExpensesViewModel(
                    ServiceLocator.recurringExpenseRepository,
                    ServiceLocator.settingsRepository,
                    ServiceLocator.categoryRepository
                )
            }
        }
    }
}
