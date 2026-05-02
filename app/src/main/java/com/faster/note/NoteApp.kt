package com.faster.note

import android.app.Application
import com.faster.note.data.db.NoteDatabase
import com.faster.note.data.repository.FolderRepository
import com.faster.note.data.repository.NoteRepository
import com.faster.note.data.repository.TagRepository

class NoteApp : Application() {
    val database by lazy { NoteDatabase(this) }
    val noteRepository by lazy { NoteRepository(database.noteDao) }
    val folderRepository by lazy { FolderRepository(database.folderDao) }
    val tagRepository by lazy { TagRepository(database.tagDao) }
}
