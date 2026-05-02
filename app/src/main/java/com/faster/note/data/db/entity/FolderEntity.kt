package com.faster.note.data.db.entity

data class FolderEntity(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6C63FF.toInt()
)
