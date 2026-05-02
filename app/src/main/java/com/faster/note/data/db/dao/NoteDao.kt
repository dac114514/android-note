package com.faster.note.data.db.dao

import com.faster.note.data.db.DatabaseHelper
import com.faster.note.data.db.entity.NoteEntity
import com.faster.note.data.db.entity.NoteTagCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class NoteDao(private val db: DatabaseHelper) {

    private val notifier = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)

    fun getAllNotes(): Flow<List<NoteEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getAllNotes() }
        .flowOn(Dispatchers.IO)

    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getNotesByFolder(folderId) }
        .flowOn(Dispatchers.IO)

    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getNotesByTag(tagId) }
        .flowOn(Dispatchers.IO)

    fun getFavoriteNotes(): Flow<List<NoteEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getFavoriteNotes() }
        .flowOn(Dispatchers.IO)

    fun getTagIdsForNote(noteId: Long): Flow<List<Long>> = notifier
        .onStart { emit(Unit) }
        .map { db.getTagIdsForNote(noteId) }
        .flowOn(Dispatchers.IO)

    suspend fun getNoteById(id: Long): NoteEntity? = withContext(Dispatchers.IO) {
        db.getNoteById(id)
    }

    suspend fun insertNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        db.insertNote(note).also { notifier.tryEmit(Unit) }
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        db.updateNote(note).also { notifier.tryEmit(Unit) }
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        db.deleteNoteById(id).also { notifier.tryEmit(Unit) }
    }

    suspend fun addTagToNote(crossRef: NoteTagCrossRef) = withContext(Dispatchers.IO) {
        db.addTagToNote(crossRef).also { notifier.tryEmit(Unit) }
    }

    suspend fun removeAllTagsFromNote(noteId: Long) = withContext(Dispatchers.IO) {
        db.removeAllTagsFromNote(noteId).also { notifier.tryEmit(Unit) }
    }
}
