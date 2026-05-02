package com.faster.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isDarkMode: Boolean = false
)

class SettingsViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getAllCategories(),
        _isDarkMode
    ) { categories, darkMode ->
        SettingsUiState(categories = categories, isDarkMode = darkMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleDarkMode(enabled: Boolean) { _isDarkMode.value = enabled }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.id == 0L) categoryRepository.insert(category)
            else categoryRepository.update(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    class Factory(
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(categoryRepository) as T
        }
    }
}
