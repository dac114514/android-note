package com.faster.note.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.ai.DeepSeekService
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.AiConfigRepository
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DayUiState(
    val year: Int,
    val month: Int,
    val day: Int,
    val schedules: List<ScheduleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val aiLoading: Boolean = false,
    val aiAnalysisText: String = "",
    val aiError: String? = null,
    val aiApiKeyConfigured: Boolean = false
)

class DayViewModel : ViewModel() {

    private val _currentDate = MutableStateFlow(Calendar.getInstance())
    private val _aiLoading = MutableStateFlow(false)
    private val _aiAnalysisText = MutableStateFlow("")
    private val _aiError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DayUiState> = combine(
        _currentDate.flatMapLatest { cal ->
            val dateStart = Calendar.getInstance().apply {
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dateEnd = dateStart + 86400000L - 1
            ScheduleRepository.schedules.map { list ->
                list.filter { it.date in dateStart..dateEnd }
                    .sortedWith(compareBy<ScheduleEntity> { !it.isAllDay }.thenBy { it.startTime ?: Long.MAX_VALUE })
            }
        },
        CategoryRepository.categories,
        _aiLoading,
        _aiAnalysisText,
        _aiError,
        AiConfigRepository.apiKey
    ) { schedules, categories, aiLoading, aiText, aiError, apiKey ->
        DayUiState(
            year = _currentDate.value.get(Calendar.YEAR),
            month = _currentDate.value.get(Calendar.MONTH) + 1,
            day = _currentDate.value.get(Calendar.DAY_OF_MONTH),
            schedules = schedules,
            categories = categories,
            completedCount = schedules.count { it.isCompleted },
            totalCount = schedules.size,
            aiLoading = aiLoading,
            aiAnalysisText = aiText,
            aiError = aiError,
            aiApiKeyConfigured = apiKey.isNotBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUiState(0, 0, 0))

    fun goToPreviousDay() { _currentDate.value = _currentDate.value.apply { add(Calendar.DAY_OF_MONTH, -1) } }
    fun goToNextDay() { _currentDate.value = _currentDate.value.apply { add(Calendar.DAY_OF_MONTH, 1) } }
    fun goToToday() { _currentDate.value = Calendar.getInstance() }
    fun goToDate(year: Int, month: Int, day: Int) {
        _currentDate.value = Calendar.getInstance().apply { set(year, month - 1, day) }
    }

    fun toggleCompleted(schedule: ScheduleEntity) {
        ScheduleRepository.toggleCompleted(schedule.id)
    }

    fun saveSchedule(schedule: ScheduleEntity) {
        ScheduleRepository.saveSchedule(schedule)
    }

    fun deleteSchedule(id: Long) {
        ScheduleRepository.deleteSchedule(id)
    }

    fun requestDayAiAnalysis() {
        val apiKey = AiConfigRepository.apiKey.value
        if (apiKey.isBlank()) return

        val cal = _currentDate.value
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dateStart = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dateEnd = dateStart + 86400000L - 1

        val todaySchedules = ScheduleRepository.schedules.value
            .filter { it.date in dateStart..dateEnd }
            .sortedWith(compareBy<ScheduleEntity> { !it.isAllDay }.thenBy { it.startTime ?: Long.MAX_VALUE })

        val categories = CategoryRepository.categories.value
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.appendLine("请简要分析今天的日程安排，保持简洁。")
        sb.appendLine()
        sb.appendLine("${year}年${month}月${day}日日程：")
        sb.appendLine()
        todaySchedules.forEach { s ->
            val cat = categories.find { it.id == s.categoryId }
            val completed = if (s.isCompleted) "[已完成]" else "[待完成]"
            val time = if (s.isAllDay) "全天"
            else if (s.startTime != null && s.endTime != null)
                "${sdf.format(Date(s.startTime))}-${sdf.format(Date(s.endTime))}"
            else if (s.startTime != null) sdf.format(Date(s.startTime))
            else ""
            val catName = if (cat != null) "(${cat.name})" else ""
            sb.appendLine("$completed $time ${s.title} $catName")
        }
        sb.appendLine()
        sb.appendLine("总计：${todaySchedules.size} 个日程")
        sb.appendLine("已完成：${todaySchedules.count { it.isCompleted }}")
        sb.appendLine("未完成：${todaySchedules.count { !it.isCompleted }}")

        val prompt = sb.toString()

        _aiLoading.value = true
        _aiAnalysisText.value = ""
        _aiError.value = null

        viewModelScope.launch {
            try {
                val result = DeepSeekService.requestAnalysis(apiKey, prompt)
                _aiAnalysisText.value = result
            } catch (e: Exception) {
                _aiError.value = e.message ?: "分析失败"
            } finally {
                _aiLoading.value = false
            }
        }
    }
}
