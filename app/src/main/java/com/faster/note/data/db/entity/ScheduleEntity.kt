package com.faster.note.data.db.entity

data class ScheduleEntity(
    val id: Long = 0,
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
