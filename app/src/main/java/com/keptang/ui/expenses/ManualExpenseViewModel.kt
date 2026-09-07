package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManualExpenseViewModel(
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    private val expenseId: String?
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categoryNames: StateFlow<List<String>> = categoryRepository.observeAll()
        .map { categories -> categories.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _existingExpense = MutableStateFlow<ExpenseEntity?>(null)
    val existingExpense: StateFlow<ExpenseEntity?> = _existingExpense.asStateFlow()

    /** True when the expense being edited was produced by an actual voice capture, not typed by hand. */
    private val _isFromVoiceCapture = MutableStateFlow(false)
    val isFromVoiceCapture: StateFlow<Boolean> = _isFromVoiceCapture.asStateFlow()

    init {
        if (expenseId != null) {
            viewModelScope.launch {
                val expense = expenseRepository.getById(expenseId)
                _existingExpense.value = expense
                if (expense != null) {
                    val capture = captureRepository.getById(expense.captureId)
                    _isFromVoiceCapture.value = capture != null && !capture.isManual
                }
            }
        }
    }

    /**
     * Creates a new manual entry (placeholder capture + expense), or - when [expenseId] was
     * supplied - updates that existing expense in place, leaving its originating capture
     * untouched.
     */
    fun save(
        amountMinorUnits: Long,
        currencyCode: String,
        category: String,
        account: String?,
        paymentMethod: String?,
        merchant: String?,
        timeZoneId: String,
        occurredAtEpochMillis: Long,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = _existingExpense.value
            if (existing != null) {
                expenseRepository.update(
                    existing.copy(
                        amountMinorUnits = amountMinorUnits,
                        currencyCode = currencyCode,
                        occurredAtEpochMillis = occurredAtEpochMillis,
                        timeZoneId = timeZoneId,
                        category = category,
                        account = account?.takeIf { it.isNotBlank() },
                        paymentMethod = paymentMethod?.takeIf { it.isNotBlank() },
                        merchant = merchant?.takeIf { it.isNotBlank() }
                    )
                )
            } else {
                val captureId = captureRepository.createManualEntry(timeZoneId)
                expenseRepository.createManual(
                    captureId = captureId,
                    amountMinorUnits = amountMinorUnits,
                    currencyCode = currencyCode,
                    occurredAtEpochMillis = occurredAtEpochMillis,
                    timeZoneId = timeZoneId,
                    category = category,
                    account = account?.takeIf { it.isNotBlank() },
                    paymentMethod = paymentMethod?.takeIf { it.isNotBlank() },
                    merchant = merchant?.takeIf { it.isNotBlank() }
                )
            }
            onSaved()
        }
    }

    companion object {
        fun factory(expenseId: String? = null) = viewModelFactory {
            initializer {
                ManualExpenseViewModel(
                    ServiceLocator.captureRepository,
                    ServiceLocator.expenseRepository,
                    ServiceLocator.settingsRepository,
                    ServiceLocator.categoryRepository,
                    expenseId
                )
            }
        }
    }
}
