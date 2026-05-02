package com.faster.note.data.db.entity

data class CategoryEntity(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF1565C0.toInt(),
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
)
