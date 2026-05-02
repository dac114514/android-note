package com.faster.note.data.db.dao

import com.faster.note.data.db.DatabaseHelper
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class TagDao(private val db: DatabaseHelper) {

    private val notifier = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)

    fun getAllTags(): Flow<List<TagEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getAllTags() }
        .flowOn(Dispatchers.IO)

    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getTagsForNote(noteId) }
        .flowOn(Dispatchers.IO)

    suspend fun getTagById(id: Long): TagEntity? = withContext(Dispatchers.IO) {
        db.getTagById(id)
    }

    suspend fun insertTag(tag: TagEntity): Long = withContext(Dispatchers.IO) {
        db.insertTag(tag).also { notifier.tryEmit(Unit) }
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        db.deleteTag(tag).also { notifier.tryEmit(Unit) }
    }
}
