package com.faster.note.data.analysis

data class AnalysisReport(
    val period: String,
    val totalEvents: Int,
    val completedEvents: Int,
    val completionRate: Float,
    val categoryDistribution: List<CategoryStat>,
    val summary: String
)

data class CategoryStat(
    val categoryName: String,
    val categoryColor: Int,
    val count: Int,
    val percentage: Float
)

interface AnalysisService {
    suspend fun analyzeDaily(year: Int, month: Int, day: Int): AnalysisReport
    suspend fun analyzeMonthly(year: Int, month: Int): AnalysisReport
    suspend fun getCompletionRate(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ): Float
}
