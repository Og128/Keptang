package com.keptang.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.SettingsRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setCurrency(code: String) = viewModelScope.launch { settingsRepository.setCurrency(code) }
    fun setTimeZone(id: String) = viewModelScope.launch { settingsRepository.setTimeZone(id) }
    fun setDefaultAccount(account: String) = viewModelScope.launch { settingsRepository.setDefaultAccount(account) }
    fun setAudioRetentionDays(days: Int) = viewModelScope.launch { settingsRepository.setAudioRetentionDays(days) }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(ServiceLocator.settingsRepository) }
        }
    }
}
