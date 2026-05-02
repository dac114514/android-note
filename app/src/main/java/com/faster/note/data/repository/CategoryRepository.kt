package com.faster.note.data.repository

import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.local.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CategoryRepository {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _categories = MutableStateFlow(mockCategories())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private fun categoriesFile(): File =
        File(DataStore.appContext.filesDir, "categories.json")

    suspend fun loadAll() {
        val result = withContext(Dispatchers.IO) {
            val file = categoriesFile()
            if (!file.exists()) {
                persistAllSync(file)
                _categories.value
            } else {
                try {
                    val text = file.readText()
                    if (text.isBlank()) mockCategories()
                    else JSONArray(text).let { arr ->
                        (0 until arr.length()).map { categoryFromJson(arr.getJSONObject(it)) }
                    }
                } catch (_: Exception) { _categories.value }
            }
        }
        _categories.value = result
    }

    private fun persistAllSync(file: File) {
        file.writeText(JSONArray().apply {
            _categories.value.forEach { put(categoryToJson(it)) }
        }.toString(2))
    }

    private fun persistAll() {
        ioScope.launch {
            val file = categoriesFile()
            file.writeText(JSONArray().apply {
                _categories.value.forEach { put(categoryToJson(it)) }
            }.toString(2))
        }
    }

    fun saveCategory(category: CategoryEntity) {
        _categories.value = if (category.id == 0L) {
            val newId = (_categories.value.maxOfOrNull { it.id } ?: 0) + 1
            _categories.value + category.copy(id = newId)
        } else {
            _categories.value.map { if (it.id == category.id) category else it }
        }
        persistAll()
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!category.isPreset) {
            _categories.value = _categories.value.filter { it.id != category.id }
            persistAll()
        }
    }

    // === JSON ===

    private fun categoryToJson(c: CategoryEntity): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("color", c.color)
        put("isPreset", c.isPreset)
        put("sortOrder", c.sortOrder)
    }

    private fun categoryFromJson(obj: JSONObject): CategoryEntity = CategoryEntity(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        color = obj.getInt("color"),
        isPreset = obj.optBoolean("isPreset", false),
        sortOrder = obj.optInt("sortOrder", 0)
    )

    // === Mock Data ===

    private fun mockCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(id = 1, name = "工作", color = 0xFF1565C0.toInt(), isPreset = true, sortOrder = 1),
        CategoryEntity(id = 2, name = "个人", color = 0xFF43A047.toInt(), isPreset = true, sortOrder = 2),
        CategoryEntity(id = 3, name = "学习", color = 0xFFE53935.toInt(), isPreset = true, sortOrder = 3),
        CategoryEntity(id = 4, name = "健康", color = 0xFFFB8C00.toInt(), isPreset = true, sortOrder = 4),
    )
}
