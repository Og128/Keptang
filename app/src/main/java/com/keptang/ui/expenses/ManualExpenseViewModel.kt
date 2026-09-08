package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.CategoryEntity
import com.keptang.data.db.ExpenseEntity
import com.keptang.data.repository.AppSettings
import com.keptang.data.db.BudgetPeriodType
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.CategoryRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.RecurringExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import com.keptang.recurring.RecurringExpenseGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManualExpenseViewModel(
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val recurringExpenseGenerator: RecurringExpenseGenerator,
    private val expenseId: String?
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
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
        notes: String?,
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
                        merchant = merchant?.takeIf { it.isNotBlank() },
                        notes = notes?.takeIf { it.isNotBlank() }
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
                    merchant = merchant?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() }
                )
            }
            onSaved()
        }
    }

    /**
     * Creates a recurring definition instead of a one-off expense - used by the add-mode toggle
     * next to the mascot. Runs the generator immediately after so a recurrence due today or
     * yesterday shows up in the ledger right away, instead of waiting for the next cold app start
     * (see [RecurringExpenseGenerator], normally only triggered from [com.keptang.ui.MainActivity]).
     */
    fun saveRecurring(
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
            recurringExpenseRepository.create(name, amountMinorUnits, category, periodType, periodAnchor, timeZoneId)
            recurringExpenseGenerator.generateDueExpenses(timeZoneId, currencyCode)
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
                    ServiceLocator.recurringExpenseRepository,
                    ServiceLocator.recurringExpenseGenerator,
                    expenseId
                )
            }
        }
    }
}
