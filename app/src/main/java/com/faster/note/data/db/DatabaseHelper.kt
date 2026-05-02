package com.faster.note.data.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.faster.note.data.db.entity.FolderEntity
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import com.faster.note.data.db.entity.TagEntity

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DB_NAME, null, DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_FOLDERS)
        db.execSQL(CREATE_TABLE_TAGS)
        db.execSQL(CREATE_TABLE_NOTES)
        db.execSQL(CREATE_TABLE_NOTE_TAG_CROSS_REF)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS note_tag_cross_ref")
        db.execSQL("DROP TABLE IF EXISTS notes")
        db.execSQL("DROP TABLE IF EXISTS tags")
        db.execSQL("DROP TABLE IF EXISTS folders")
        onCreate(db)
    }

    // ---- Notes ----

    fun getAllNotes(): List<NoteEntity> {
        val list = mutableListOf<NoteEntity>()
        getReadableDatabase().rawQuery("SELECT * FROM notes ORDER BY updatedAt DESC", null).use { c ->
            while (c.moveToNext()) list.add(readNote(c))
        }
        return list
    }

    fun getNotesByFolder(folderId: Long): List<NoteEntity> {
        val list = mutableListOf<NoteEntity>()
        getReadableDatabase().rawQuery(
            "SELECT * FROM notes WHERE folderId = ? ORDER BY updatedAt DESC",
            arrayOf(folderId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(readNote(c))
        }
        return list
    }

    fun getNotesByTag(tagId: Long): List<NoteEntity> {
        val list = mutableListOf<NoteEntity>()
        getReadableDatabase().rawQuery(
            """SELECT n.* FROM notes n
               INNER JOIN note_tag_cross_ref c ON n.id = c.noteId
               WHERE c.tagId = ?
               ORDER BY n.updatedAt DESC""",
            arrayOf(tagId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(readNote(c))
        }
        return list
    }

    fun getFavoriteNotes(): List<NoteEntity> {
        val list = mutableListOf<NoteEntity>()
        getReadableDatabase().rawQuery(
            "SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC", null
        ).use { c ->
            while (c.moveToNext()) list.add(readNote(c))
        }
        return list
    }

    fun getNoteById(id: Long): NoteEntity? {
        getReadableDatabase().rawQuery(
            "SELECT * FROM notes WHERE id = ?", arrayOf(id.toString())
        ).use { c ->
            return if (c.moveToFirst()) readNote(c) else null
        }
    }

    fun insertNote(note: NoteEntity): Long {
        return getWritableDatabase().insert("notes", null, noteToValues(note))
    }

    fun updateNote(note: NoteEntity) {
        getWritableDatabase().update("notes", noteToValues(note), "id = ?", arrayOf(note.id.toString()))
    }

    fun deleteNoteById(id: Long) {
        getWritableDatabase().delete("notes", "id = ?", arrayOf(id.toString()))
    }

    // ---- Folders ----

    fun getAllFolders(): List<FolderEntity> {
        val list = mutableListOf<FolderEntity>()
        getReadableDatabase().rawQuery("SELECT * FROM folders ORDER BY name ASC", null).use { c ->
            while (c.moveToNext()) list.add(readFolder(c))
        }
        return list
    }

    fun getFolderById(id: Long): FolderEntity? {
        getReadableDatabase().rawQuery(
            "SELECT * FROM folders WHERE id = ?", arrayOf(id.toString())
        ).use { c ->
            return if (c.moveToFirst()) readFolder(c) else null
        }
    }

    fun insertFolder(folder: FolderEntity): Long {
        return getWritableDatabase().insert("folders", null, folderToValues(folder))
    }

    fun updateFolder(folder: FolderEntity) {
        getWritableDatabase().update("folders", folderToValues(folder), "id = ?", arrayOf(folder.id.toString()))
    }

    fun deleteFolder(folder: FolderEntity) {
        getWritableDatabase().delete("folders", "id = ?", arrayOf(folder.id.toString()))
    }

    // ---- Tags ----

    fun getAllTags(): List<TagEntity> {
        val list = mutableListOf<TagEntity>()
        getReadableDatabase().rawQuery("SELECT * FROM tags ORDER BY name ASC", null).use { c ->
            while (c.moveToNext()) list.add(readTag(c))
        }
        return list
    }

    fun getTagById(id: Long): TagEntity? {
        getReadableDatabase().rawQuery(
            "SELECT * FROM tags WHERE id = ?", arrayOf(id.toString())
        ).use { c ->
            return if (c.moveToFirst()) readTag(c) else null
        }
    }

    fun insertTag(tag: TagEntity): Long {
        return getWritableDatabase().insert("tags", null, tagToValues(tag))
    }

    fun updateTag(tag: TagEntity) {
        getWritableDatabase().update("tags", tagToValues(tag), "id = ?", arrayOf(tag.id.toString()))
    }

    fun deleteTag(tag: TagEntity) {
        getWritableDatabase().delete("tags", "id = ?", arrayOf(tag.id.toString()))
    }

    fun getTagsForNote(noteId: Long): List<TagEntity> {
        val list = mutableListOf<TagEntity>()
        getReadableDatabase().rawQuery(
            """SELECT t.* FROM tags t
               INNER JOIN note_tag_cross_ref c ON t.id = c.tagId
               WHERE c.noteId = ?
               ORDER BY t.name ASC""",
            arrayOf(noteId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(readTag(c))
        }
        return list
    }

    // ---- Note-Tag cross ref ----

    fun addTagToNote(crossRef: NoteTagCrossRef) {
        val cv = android.content.ContentValues().apply {
            put("noteId", crossRef.noteId)
            put("tagId", crossRef.tagId)
        }
        getWritableDatabase().insertWithOnConflict("note_tag_cross_ref", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeAllTagsFromNote(noteId: Long) {
        getWritableDatabase().delete("note_tag_cross_ref", "noteId = ?", arrayOf(noteId.toString()))
    }

    fun getTagIdsForNote(noteId: Long): List<Long> {
        val list = mutableListOf<Long>()
        getReadableDatabase().rawQuery(
            "SELECT tagId FROM note_tag_cross_ref WHERE noteId = ?", arrayOf(noteId.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(c.getLong(0))
        }
        return list
    }

    // ---- Readers ----

    private fun readNote(c: Cursor): NoteEntity = NoteEntity(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        title = c.getString(c.getColumnIndexOrThrow("title")) ?: "",
        content = c.getString(c.getColumnIndexOrThrow("content")) ?: "",
        folderId = if (c.isNull(c.getColumnIndexOrThrow("folderId"))) null else c.getLong(c.getColumnIndexOrThrow("folderId")),
        createdAt = c.getLong(c.getColumnIndexOrThrow("createdAt")),
        updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt")),
        isFavorite = c.getInt(c.getColumnIndexOrThrow("isFavorite")) != 0,
        color = if (c.isNull(c.getColumnIndexOrThrow("color"))) null else c.getInt(c.getColumnIndexOrThrow("color"))
    )

    private fun readFolder(c: Cursor): FolderEntity = FolderEntity(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")) ?: "",
        color = c.getInt(c.getColumnIndexOrThrow("color"))
    )

    private fun readTag(c: Cursor): TagEntity = TagEntity(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")) ?: ""
    )

    // ---- Converters ----

    private fun noteToValues(note: NoteEntity) = android.content.ContentValues().apply {
        if (note.id != 0L) put("id", note.id)
        put("title", note.title)
        put("content", note.content)
        if (note.folderId != null) put("folderId", note.folderId) else putNull("folderId")
        put("createdAt", note.createdAt)
        put("updatedAt", note.updatedAt)
        put("isFavorite", if (note.isFavorite) 1 else 0)
        if (note.color != null) put("color", note.color) else putNull("color")
    }

    private fun folderToValues(folder: FolderEntity) = android.content.ContentValues().apply {
        if (folder.id != 0L) put("id", folder.id)
        put("name", folder.name)
        put("color", folder.color)
    }

    private fun tagToValues(tag: TagEntity) = android.content.ContentValues().apply {
        if (tag.id != 0L) put("id", tag.id)
        put("name", tag.name)
    }

    companion object {
        private const val DB_NAME = "note_app.db"
        private const val DB_VERSION = 1

        private const val CREATE_TABLE_FOLDERS = """
            CREATE TABLE IF NOT EXISTS folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                color INTEGER NOT NULL
            )
        """

        private const val CREATE_TABLE_TAGS = """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
        """

        private const val CREATE_TABLE_NOTES = """
            CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL DEFAULT '',
                content TEXT NOT NULL DEFAULT '',
                folderId INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                color INTEGER,
                FOREIGN KEY (folderId) REFERENCES folders(id) ON DELETE SET NULL
            )
        """

        private const val CREATE_TABLE_NOTE_TAG_CROSS_REF = """
            CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                noteId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY (noteId, tagId),
                FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE,
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
        """
    }
}
