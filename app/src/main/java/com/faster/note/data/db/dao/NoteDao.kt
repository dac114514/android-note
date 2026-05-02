package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_tag_cross_ref c ON n.id = c.noteId
        WHERE c.tagId = :tagId
        ORDER BY n.updatedAt DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    // Tag associations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToNote(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun removeAllTagsFromNote(noteId: Long)

    @Query("SELECT tagId FROM note_tag_cross_ref WHERE noteId = :noteId")
    fun getTagIdsForNote(noteId: Long): Flow<List<Long>>
}
