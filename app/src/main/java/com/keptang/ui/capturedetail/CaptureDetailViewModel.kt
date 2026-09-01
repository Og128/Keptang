package com.keptang.ui.capturedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.capture.CaptureProcessor
import com.keptang.data.db.CaptureEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CaptureDetailState(
    val capture: CaptureEntity? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val isRetrying: Boolean = false
)

class CaptureDetailViewModel(
    captureId: String,
    private val captureRepository: CaptureRepository,
    private val captureProcessor: CaptureProcessor
) : ViewModel() {

    private val captureIdValue = captureId
    private val isRetrying = MutableStateFlow(false)

    val state: StateFlow<CaptureDetailState> = combine(
        captureRepository.observeById(captureId),
        ServiceLocator.expenseRepository.observeByCaptureId(captureId),
        isRetrying
    ) { capture, expenses, retrying ->
        CaptureDetailState(capture = capture, expenses = expenses, isRetrying = retrying)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaptureDetailState())

    fun retry() {
        viewModelScope.launch {
            isRetrying.value = true
            captureProcessor.retry(captureIdValue)
            isRetrying.value = false
        }
    }

    fun delete() {
        viewModelScope.launch { captureRepository.delete(captureIdValue) }
    }

    companion object {
        fun factory(captureId: String) = viewModelFactory {
            initializer {
                CaptureDetailViewModel(captureId, ServiceLocator.captureRepository, ServiceLocator.captureProcessor)
            }
        }
    }
}
