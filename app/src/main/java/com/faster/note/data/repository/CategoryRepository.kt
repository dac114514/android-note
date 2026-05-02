package com.faster.note.data.repository

import com.faster.note.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.*

class CategoryRepository {
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private var nextId = 1L

    fun getAllCategories(): Flow<List<CategoryEntity>> = _categories.asStateFlow()

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        _categories.value.find { it.id == id }

    suspend fun insert(category: CategoryEntity): Long {
        val id = nextId++
        val newCategory = category.copy(id = id)
        _categories.value = _categories.value + newCategory
        return id
    }

    suspend fun update(category: CategoryEntity) {
        _categories.value = _categories.value.map { if (it.id == category.id) category else it }
    }

    suspend fun delete(category: CategoryEntity) {
        _categories.value = _categories.value.filter { it.id != category.id }
    }

    fun addAll(categories: List<CategoryEntity>) {
        _categories.value = categories
        nextId = (categories.maxOfOrNull { it.id } ?: 0) + 1
    }
}
