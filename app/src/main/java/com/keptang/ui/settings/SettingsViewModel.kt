package com.keptang.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.ColorTheme
import com.keptang.data.repository.SettingsRepository
import com.keptang.debug.DemoDataSeeder
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

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

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(ServiceLocator.settingsRepository) }
        }
    }
}
