package com.faster.note.data.analysis

import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository

class LocalAnalysisService(
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: CategoryRepository
) : AnalysisService {

    override suspend fun analyzeDaily(year: Int, month: Int, day: Int): AnalysisReport {
        val schedules = scheduleRepository.getSchedulesForDate(year, month, day)
        // Flow-based, we'll use the repository's suspend functions instead
        val total = 0
        val completed = 0
        return AnalysisReport(
            period = "daily",
            totalEvents = total,
            completedEvents = completed,
            completionRate = 0f,
            categoryDistribution = emptyList(),
            summary = "暂无日程数据"
        )
    }

    override suspend fun analyzeMonthly(year: Int, month: Int): AnalysisReport {
        val total = scheduleRepository.getTotalCount(year, month)
        val completed = scheduleRepository.getCompletedCount(year, month)
        val rate = if (total > 0) completed.toFloat() / total else 0f
        return AnalysisReport(
            period = "monthly",
            totalEvents = total,
            completedEvents = completed,
            completionRate = rate,
            categoryDistribution = emptyList(),
            summary = "本月共 $total 项日程，已完成 $completed 项（${(rate * 100).toInt()}%）"
        )
    }

    override suspend fun getCompletionRate(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ): Float {
        val total = scheduleRepository.getTotalCount(startYear, startMonth)
        if (total == 0) return 0f
        val completed = scheduleRepository.getCompletedCount(startYear, startMonth)
        return completed.toFloat() / total
    }
}
