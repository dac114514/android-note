package com.faster.note.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class FolderWithNotes(
    @Embedded val folder: FolderEntity,
    @Relation(parentColumn = "id", entityColumn = "folderId")
    val notes: List<NoteEntity> = emptyList()
)
