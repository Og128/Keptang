package com.keptang.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class ManualExpenseViewModel(
    private val captureRepository: CaptureRepository,
    private val expenseRepository: ExpenseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /** Creates a placeholder capture plus the expense itself, then invokes [onSaved]. */
    fun save(
        amountMinorUnits: Long,
        currencyCode: String,
        category: String,
        account: String?,
        paymentMethod: String?,
        merchant: String?,
        timeZoneId: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val captureId = captureRepository.createManualEntry(timeZoneId)
            expenseRepository.createManual(
                captureId = captureId,
                amountMinorUnits = amountMinorUnits,
                currencyCode = currencyCode,
                occurredAtEpochMillis = Instant.now().toEpochMilli(),
                timeZoneId = timeZoneId,
                category = category,
                account = account?.takeIf { it.isNotBlank() },
                paymentMethod = paymentMethod?.takeIf { it.isNotBlank() },
                merchant = merchant?.takeIf { it.isNotBlank() }
            )
            onSaved()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ManualExpenseViewModel(
                    ServiceLocator.captureRepository,
                    ServiceLocator.expenseRepository,
                    ServiceLocator.settingsRepository
                )
            }
        }
    }
}
