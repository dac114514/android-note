package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT t.* FROM tags t INNER JOIN note_tag_cross_ref c ON t.id = c.tagId WHERE c.noteId = :noteId")
    fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>
}
