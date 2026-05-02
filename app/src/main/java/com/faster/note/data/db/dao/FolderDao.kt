package com.faster.note.data.db.dao

import com.faster.note.data.db.DatabaseHelper
import com.faster.note.data.db.entity.FolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class FolderDao(private val db: DatabaseHelper) {

    private val notifier = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)

    fun getAllFolders(): Flow<List<FolderEntity>> = notifier
        .onStart { emit(Unit) }
        .map { db.getAllFolders() }
        .flowOn(Dispatchers.IO)

    fun getNoteCountForFolder(folderId: Long): Flow<Int> = notifier
        .onStart { emit(Unit) }
        .map { db.getNoteCountForFolder(folderId) }
        .flowOn(Dispatchers.IO)

    suspend fun getFolderById(id: Long): FolderEntity? = withContext(Dispatchers.IO) {
        db.getFolderById(id)
    }

    suspend fun insertFolder(folder: FolderEntity): Long = withContext(Dispatchers.IO) {
        db.insertFolder(folder).also { notifier.tryEmit(Unit) }
    }

    suspend fun updateFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        db.updateFolder(folder).also { notifier.tryEmit(Unit) }
    }

    suspend fun deleteFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        db.deleteFolder(folder).also { notifier.tryEmit(Unit) }
    }
}
