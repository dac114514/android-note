package com.faster.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*

data class SettingsUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isDarkMode: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)

    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        CategoryRepository.categories,
        _isDarkMode
    ) { categories, darkMode ->
        SettingsUiState(categories = categories, isDarkMode = darkMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleDarkMode(enabled: Boolean) { _isDarkMode.value = enabled }

    fun saveCategory(category: CategoryEntity) {
        CategoryRepository.saveCategory(category)
    }

    fun deleteCategory(category: CategoryEntity) {
        CategoryRepository.deleteCategory(category)
    }
}
