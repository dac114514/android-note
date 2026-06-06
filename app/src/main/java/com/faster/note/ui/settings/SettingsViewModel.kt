package com.faster.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.repository.AiChatRepository
import com.faster.note.data.repository.AiConfigRepository
import com.faster.note.data.repository.CategoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isDarkMode: Boolean = false,
    val apiKey: String = "",
    val chatMessageCount: Int = 0
)

class SettingsViewModel : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    private val _chatMessageCount = MutableStateFlow(AiChatRepository.getMessageCount())

    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        CategoryRepository.categories,
        _isDarkMode,
        AiConfigRepository.apiKey,
        _chatMessageCount
    ) { categories, darkMode, apiKey, msgCount ->
        SettingsUiState(categories = categories, isDarkMode = darkMode, apiKey = apiKey, chatMessageCount = msgCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun toggleDarkMode(enabled: Boolean) { _isDarkMode.value = enabled }

    fun saveCategory(category: CategoryEntity) {
        CategoryRepository.saveCategory(category)
    }

    fun deleteCategory(category: CategoryEntity) {
        CategoryRepository.deleteCategory(category)
    }

    fun saveApiKey(key: String) {
        AiConfigRepository.saveApiKey(key)
    }

    fun clearAiContext() {
        viewModelScope.launch {
            AiChatRepository.clearMessages()
            _chatMessageCount.value = 0
        }
    }
}
