package com.faster.note.ui.month

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

data class MonthUiState(
    val year: Int,
    val month: Int,
    val markedDateCounts: Map<Int, Int> = emptyMap(),
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val selectedDay: Int? = null,
    val selectedDaySchedules: List<ScheduleEntity> = emptyList(),
    val selectedDayScheduleCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val aiApiKeyConfigured: Boolean = false,
    val aiAnalysisText: String = "",
    val aiLoading: Boolean = false,
    val aiError: String? = null
)

class MonthViewModel : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDay = MutableStateFlow<Int?>(null)
    private val _aiAnalysisText = MutableStateFlow("")
    private val _aiLoading = MutableStateFlow(false)
    private val _aiError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MonthUiState> = combine(
        _currentMonth,
        ScheduleRepository.schedules,
        _searchQuery,
        _selectedDay,
        CategoryRepository.categories,
        AiConfigRepository.apiKey,
        _aiAnalysisText,
        _aiLoading,
        _aiError
    ) { cal, allSchedules, query, selectedDay, categories, apiKey, aiText, aiLoading, aiError ->
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val monthStart = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        val filtered = allSchedules.filter { it.date in monthStart..monthEnd }
        val dateCounts = filtered.groupBy { s ->
            Calendar.getInstance().apply { timeInMillis = s.date }.get(Calendar.DAY_OF_MONTH)
        }.mapValues { it.value.size }

        val schedulesForSelectedDay = if (selectedDay != null) {
            val dayStart = Calendar.getInstance().apply {
                set(year, month - 1, selectedDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86400000L - 1
            filtered.filter { it.date in dayStart..dayEnd }
                .sortedWith(compareBy<ScheduleEntity> { !it.isAllDay }.thenBy { it.startTime ?: Long.MAX_VALUE })
        } else emptyList()

        MonthUiState(
            year = year,
            month = month,
            markedDateCounts = dateCounts,
            searchQuery = query,
            totalCount = filtered.size,
            completedCount = filtered.count { it.isCompleted },
            selectedDay = selectedDay,
            selectedDaySchedules = schedulesForSelectedDay,
            selectedDayScheduleCount = schedulesForSelectedDay.size,
            categories = categories,
            aiApiKeyConfigured = apiKey.isNotBlank(),
            aiAnalysisText = aiText,
            aiLoading = aiLoading,
            aiError = aiError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState(0, 0))

    fun selectDay(day: Int) { _selectedDay.value = day }
    fun clearSelection() { _selectedDay.value = null }

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, -1) }
        clearSelection()
    }
    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, 1) }
        clearSelection()
    }
    fun goToToday() {
        _currentMonth.value = Calendar.getInstance()
        clearSelection()
    }
    fun goToYearMonth(year: Int, month: Int) {
        _currentMonth.value = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
        clearSelection()
    }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun requestAiAnalysis() {
        val apiKey = AiConfigRepository.apiKey.value
        if (apiKey.isBlank()) return

        val year = _currentMonth.value.get(Calendar.YEAR)
        val month = _currentMonth.value.get(Calendar.MONTH) + 1
        val allSchedules = ScheduleRepository.schedules.value

        val monthStart = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
        }.timeInMillis

        val monthSchedules = allSchedules.filter { it.date in monthStart..monthEnd }

        val categories = CategoryRepository.categories.value
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        val scheduleInfos = monthSchedules.map { s ->
            val cat = categories.find { it.id == s.categoryId }
            val timeRange = if (s.isAllDay) "全天"
            else if (s.startTime != null && s.endTime != null) "${sdf.format(Date(s.startTime))}-${sdf.format(Date(s.endTime))}"
            else if (s.startTime != null) sdf.format(Date(s.startTime))
            else ""
            DeepSeekService.ScheduleInfo(
                day = Calendar.getInstance().apply { timeInMillis = s.date }.get(Calendar.DAY_OF_MONTH),
                title = s.title,
                timeRange = timeRange,
                isAllDay = s.isAllDay,
                isCompleted = s.isCompleted,
                category = cat?.name ?: ""
            )
        }

        val prompt = DeepSeekService.buildMonthPrompt(year, month, scheduleInfos)

        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                val result = DeepSeekService.requestAnalysis(apiKey, prompt)
                _aiAnalysisText.value = result
                _aiError.value = null
            } catch (e: Exception) {
                _aiError.value = e.message ?: "分析失败"
            } finally {
                _aiLoading.value = false
            }
        }
    }
}
