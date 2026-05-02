package com.faster.note.data.repository

import com.faster.note.data.db.dao.NoteDao
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByFolder(folderId: Long) = noteDao.getNotesByFolder(folderId)

    fun getNotesByTag(tagId: Long) = noteDao.getNotesByTag(tagId)

    fun getFavoriteNotes() = noteDao.getFavoriteNotes()

    suspend fun getNoteById(id: Long) = noteDao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        noteDao.removeAllTagsFromNote(noteId)
        tagIds.forEach { tagId ->
            noteDao.addTagToNote(NoteTagCrossRef(noteId, tagId))
        }
    }

    fun getTagIdsForNote(noteId: Long) = noteDao.getTagIdsForNote(noteId)
}
