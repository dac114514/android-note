package com.faster.note.data.repository

import com.faster.note.data.db.dao.TagDao
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getTagsForNote(noteId: Long) = tagDao.getTagsForNote(noteId)

    suspend fun saveTag(tag: TagEntity): Long = tagDao.insertTag(tag)

    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)
}
