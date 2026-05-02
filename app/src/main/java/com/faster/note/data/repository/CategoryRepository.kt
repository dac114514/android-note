package com.faster.note.data.repository

import com.faster.note.data.db.dao.CategoryDao
import com.faster.note.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun getAllCategories(): Flow<List<CategoryEntity>> = dao.getAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? = dao.getCategoryById(id)

    suspend fun insert(category: CategoryEntity): Long = dao.insertCategory(category)

    suspend fun update(category: CategoryEntity) = dao.updateCategory(category)

    suspend fun delete(category: CategoryEntity) = dao.deleteCategory(category)
}
