package com.faster.note.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("categoryId"), Index("date")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isAllDay: Boolean = false,
    val isCompleted: Boolean = false,
    val categoryId: Long? = null,
    val location: String? = null,
    val notes: String? = null,
    val reminderMin: Int? = null,
    val repeatRule: String? = null,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
