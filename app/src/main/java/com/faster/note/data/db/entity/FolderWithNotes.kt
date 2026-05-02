package com.faster.note.data.db.entity

data class FolderWithNotes(
    val folder: FolderEntity,
    val notes: List<NoteEntity> = emptyList()
)
