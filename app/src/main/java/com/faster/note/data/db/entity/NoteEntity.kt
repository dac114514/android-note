package com.faster.note.data.db.entity

data class NoteEntity(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val color: Int? = null
)
