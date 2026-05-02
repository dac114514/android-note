package com.faster.note.data.repository

import com.faster.note.data.db.dao.FolderDao
import com.faster.note.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderRepository(private val folderDao: FolderDao) {

    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long) = folderDao.getFolderById(id)

    suspend fun saveFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)
}
