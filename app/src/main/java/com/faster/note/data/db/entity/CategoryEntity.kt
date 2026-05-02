package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0xFF1565C0.toInt(),
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
)
