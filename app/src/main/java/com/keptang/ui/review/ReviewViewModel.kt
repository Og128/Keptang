package com.keptang.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.CaptureEntity
import com.keptang.data.db.CaptureStatus
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val expenseRepository: ExpenseRepository,
    private val captureRepository: CaptureRepository
) : ViewModel() {

    val needsReview: StateFlow<List<ExpenseEntity>> = expenseRepository.observeNeedsReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedCaptures: StateFlow<List<CaptureEntity>> = captureRepository.observeByStatuses(listOf(CaptureStatus.FAILED))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Captures flagged needs-review that produced zero expenses (an ambiguous/empty transcript) -
     * these never show up in [needsReview] since there's no [ExpenseEntity] to review, but the
     * user still needs to open, fix or delete the transcript itself.
     */
    val emptyReviewCaptures: StateFlow<List<CaptureEntity>> = combine(
        captureRepository.observeByStatuses(listOf(CaptureStatus.NEEDS_REVIEW)),
        expenseRepository.observeAll()
    ) { captures, expenses ->
        val capturesWithExpenses = expenses.mapTo(HashSet()) { it.captureId }
        captures.filter { it.id !in capturesWithExpenses }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approve(expenseId: String) {
        viewModelScope.launch { expenseRepository.approve(expenseId) }
    }

    fun reject(expenseId: String) {
        viewModelScope.launch { expenseRepository.reject(expenseId) }
    }

    fun update(expense: ExpenseEntity) {
        viewModelScope.launch { expenseRepository.update(expense) }
    }

    fun deleteCapture(captureId: String) {
        viewModelScope.launch { captureRepository.delete(captureId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ReviewViewModel(ServiceLocator.expenseRepository, ServiceLocator.captureRepository) }
        }
    }
}
