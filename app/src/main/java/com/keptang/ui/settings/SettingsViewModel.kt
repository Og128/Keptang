package com.keptang.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.ColorTheme
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.data.repository.TagRepository
import com.keptang.debug.DemoDataSeeder
import com.keptang.di.ServiceLocator
import com.keptang.export.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val expenseRepository: ExpenseRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setProfileName(name: String) = viewModelScope.launch { settingsRepository.setProfileName(name) }
    fun setCurrency(code: String) = viewModelScope.launch { settingsRepository.setCurrency(code) }
    fun setTimeZone(id: String) = viewModelScope.launch { settingsRepository.setTimeZone(id) }
    fun setDefaultAccount(account: String) = viewModelScope.launch { settingsRepository.setDefaultAccount(account) }
    fun setAudioRetentionDays(days: Int) = viewModelScope.launch { settingsRepository.setAudioRetentionDays(days) }
    fun setLanguage(code: String) = viewModelScope.launch { settingsRepository.setLanguage(code) }
    fun setColorTheme(theme: ColorTheme) = viewModelScope.launch { settingsRepository.setColorTheme(theme) }

    private val _isSeedingDemoData = MutableStateFlow(false)
    val isSeedingDemoData: StateFlow<Boolean> = _isSeedingDemoData.asStateFlow()

    /** Debug-only: fills the ledger with [count] fake expenses. See [DemoDataSeeder]. */
    fun seedDemoData(count: Int = 50) {
        viewModelScope.launch {
            _isSeedingDemoData.value = true
            DemoDataSeeder.seed(
                count,
                ServiceLocator.captureRepository,
                ServiceLocator.expenseRepository,
                ServiceLocator.categoryRepository,
                ServiceLocator.settingsRepository
            )
            _isSeedingDemoData.value = false
        }
    }

    /** Builds a CSV of every expense and hands the caller a share [Intent] once it's written to cache. */
    fun exportExpenses(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val expenses = expenseRepository.observeAll().first()
            val tagsByExpenseId = tagRepository.observeTagsByExpenseId().first()
            val csv = CsvExporter.buildCsv(expenses, tagsByExpenseId)
            val file = CsvExporter.writeToCache(context, csv)
            onReady(CsvExporter.shareIntent(context, file))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(ServiceLocator.settingsRepository, ServiceLocator.expenseRepository, ServiceLocator.tagRepository)
            }
        }
    }
}
