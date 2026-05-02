package com.faster.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.ui.day.DayViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isDarkMode: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _categories = MutableStateFlow(DayViewModel.mockCategories())
    private val _isDarkMode = MutableStateFlow(false)

    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        _categories,
        _isDarkMode
    ) { categories, darkMode ->
        SettingsUiState(categories = categories, isDarkMode = darkMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleDarkMode(enabled: Boolean) { _isDarkMode.value = enabled }

    fun saveCategory(category: CategoryEntity) {
        _categories.value = if (category.id == 0L) {
            val newId = (_categories.value.maxOfOrNull { it.id } ?: 0) + 1
            _categories.value + category.copy(id = newId)
        } else {
            _categories.value.map { if (it.id == category.id) category else it }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!category.isPreset) {
            _categories.value = _categories.value.filter { it.id != category.id }
        }
    }
}
