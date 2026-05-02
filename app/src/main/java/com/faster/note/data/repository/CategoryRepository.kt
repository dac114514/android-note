package com.faster.note.data.repository

import com.faster.note.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CategoryRepository {
    private val _categories = MutableStateFlow(mockCategories())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

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

    private fun mockCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(id = 1, name = "工作", color = 0xFF1565C0.toInt(), isPreset = true, sortOrder = 1),
        CategoryEntity(id = 2, name = "个人", color = 0xFF43A047.toInt(), isPreset = true, sortOrder = 2),
        CategoryEntity(id = 3, name = "学习", color = 0xFFE53935.toInt(), isPreset = true, sortOrder = 3),
        CategoryEntity(id = 4, name = "健康", color = 0xFFFB8C00.toInt(), isPreset = true, sortOrder = 4),
    )
}
