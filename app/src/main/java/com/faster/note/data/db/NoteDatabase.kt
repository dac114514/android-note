package com.faster.note.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.faster.note.data.db.dao.FolderDao
import com.faster.note.data.db.dao.NoteDao
import com.faster.note.data.db.dao.TagDao
import com.faster.note.data.db.entity.*

@Database(
    entities = [NoteEntity::class, FolderEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var INSTANCE: NoteDatabase? = null

        fun getInstance(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_app.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
