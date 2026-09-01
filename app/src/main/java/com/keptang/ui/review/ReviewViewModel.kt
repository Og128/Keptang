package com.keptang.ui.review

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
import kotlinx.coroutines.launch

class ReviewViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {

    val needsReview: StateFlow<List<ExpenseEntity>> = expenseRepository.observeNeedsReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approve(expenseId: String) {
        viewModelScope.launch { expenseRepository.approve(expenseId) }
    }

    fun reject(expenseId: String) {
        viewModelScope.launch { expenseRepository.reject(expenseId) }
    }

    fun update(expense: ExpenseEntity) {
        viewModelScope.launch { expenseRepository.update(expense) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReviewViewModel(ServiceLocator.expenseRepository) }
        }
    }
}
