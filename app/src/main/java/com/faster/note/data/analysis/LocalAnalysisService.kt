package com.faster.note.data.analysis

import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.first

class LocalAnalysisService(
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: CategoryRepository
) : AnalysisService {

    override suspend fun analyzeDaily(year: Int, month: Int, day: Int): AnalysisReport {
        val schedules = scheduleRepository.getSchedulesForDate(year, month, day).first()
        val total = schedules.size
        val completed = schedules.count { it.isCompleted }
        val rate = if (total > 0) completed.toFloat() / total else 0f
        return AnalysisReport(
            period = "daily",
            totalEvents = total,
            completedEvents = completed,
            completionRate = rate,
            categoryDistribution = emptyList(),
            summary = "今日共 $total 项日程，已完成 $completed 项（${(rate * 100).toInt()}%）"
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
