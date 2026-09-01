package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.ExpenseRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ExpensesViewModel(expenseRepository: ExpenseRepository) : ViewModel() {

    val expenses: StateFlow<List<ExpenseEntity>> = expenseRepository.observeApproved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { ExpensesViewModel(ServiceLocator.expenseRepository) }
        }
    }
}
