package com.faster.note.data.db

import android.content.Context
import com.faster.note.data.db.dao.FolderDao
import com.faster.note.data.db.dao.NoteDao
import com.faster.note.data.db.dao.TagDao

class NoteDatabase(context: Context) {
    val noteDao: NoteDao
    val folderDao: FolderDao
    val tagDao: TagDao

    init {
        val helper = DatabaseHelper(context)
        noteDao = NoteDao(helper)
        folderDao = FolderDao(helper)
        tagDao = TagDao(helper)
    }
}
