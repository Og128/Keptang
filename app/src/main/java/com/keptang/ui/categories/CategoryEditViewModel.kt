package com.keptang.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.keptang.data.db.CategoryEntity
import com.keptang.data.repository.CategoryRepository
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryEditViewModel(
    private val categoryName: String?,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    /** Null in add mode; the category being edited otherwise. */
    val existing: StateFlow<CategoryEntity?> = flow { emit(categoryName?.let { categoryRepository.getByName(it) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(name: String, colorHex: String, iconKey: String, onSaved: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val original = categoryName
            if (original == null) {
                categoryRepository.create(name, colorHex, iconKey)
            } else {
                categoryRepository.update(original, name, colorHex, iconKey)
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val name = categoryName ?: return
        viewModelScope.launch {
            categoryRepository.delete(name)
            onDeleted()
        }
    }

    companion object {
        fun factory(categoryName: String?) = viewModelFactory {
            initializer { CategoryEditViewModel(categoryName, ServiceLocator.categoryRepository) }
        }
    }
}
