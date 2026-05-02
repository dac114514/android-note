package com.faster.note.data.db.entity

data class NoteWithTags(
    val note: NoteEntity,
    val tags: List<TagEntity> = emptyList()
)
