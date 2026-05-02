package com.faster.note

import android.app.Application
import com.faster.note.data.analysis.LocalAnalysisService
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import java.util.*

class ScheduleApp : Application() {
    val scheduleRepository by lazy { ScheduleRepository() }
    val categoryRepository by lazy { CategoryRepository() }
    val analysisService by lazy { LocalAnalysisService(scheduleRepository, categoryRepository) }

    override fun onCreate() {
        super.onCreate()
        initMockData()
    }

    private fun initMockData() {
        val presetCategories = listOf(
            CategoryEntity(id = 1, name = "工作", color = 0xFF1565C0.toInt(), isPreset = true, sortOrder = 1),
            CategoryEntity(id = 2, name = "个人", color = 0xFF43A047.toInt(), isPreset = true, sortOrder = 2),
            CategoryEntity(id = 3, name = "学习", color = 0xFFE53935.toInt(), isPreset = true, sortOrder = 3),
            CategoryEntity(id = 4, name = "健康", color = 0xFFFB8C00.toInt(), isPreset = true, sortOrder = 4),
        )
        categoryRepository.addAll(presetCategories)

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val mockSchedules = listOf(
            ScheduleEntity(id = 1, title = "团队站会", startTime = today + 9 * 3600000L, endTime = today + 9 * 3600000L + 1800000L, categoryId = 1, date = today, isCompleted = true),
            ScheduleEntity(id = 2, title = "午休", startTime = today + 12 * 3600000L, endTime = today + 13 * 3600000L, categoryId = 2, date = today),
            ScheduleEntity(id = 3, title = "学习 Jetpack Compose", startTime = today + 20 * 3600000L, endTime = today + 21 * 3600000L + 1800000L, categoryId = 3, date = today),
            ScheduleEntity(id = 4, title = "项目评审", startTime = today + 14 * 3600000L, endTime = today + 15 * 3600000L + 1800000L, categoryId = 1, date = today + 86400000L),
            ScheduleEntity(id = 5, title = "健身", startTime = today + 18 * 3600000L, endTime = today + 19 * 3600000L, categoryId = 4, date = today + 86400000L),
            ScheduleEntity(id = 6, title = "周末出游", isAllDay = true, categoryId = 2, date = today + 2 * 86400000L),
        )
        scheduleRepository.addAll(mockSchedules)
    }
}
