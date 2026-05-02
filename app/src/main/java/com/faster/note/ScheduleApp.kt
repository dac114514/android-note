package com.faster.note

import android.app.Application
import com.faster.note.data.db.AppDatabase
import com.faster.note.data.analysis.LocalAnalysisService
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository

class ScheduleApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val analysisService by lazy { LocalAnalysisService(scheduleRepository, categoryRepository) }
}
