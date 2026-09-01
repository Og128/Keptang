package com.keptang.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.CaptureEntity
import com.keptang.data.repository.CaptureRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(private val captureRepository: CaptureRepository) : ViewModel() {

    val captures: StateFlow<List<CaptureEntity>> = captureRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(captureId: String) {
        viewModelScope.launch { captureRepository.delete(captureId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { InboxViewModel(ServiceLocator.captureRepository) }
        }
    }
}
