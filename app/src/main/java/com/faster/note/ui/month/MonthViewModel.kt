package com.faster.note.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
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
    val categories: List<CategoryEntity> = emptyList()
)

class MonthViewModel : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDay = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<MonthUiState> = combine(
        _currentMonth,
        ScheduleRepository.schedules,
        _searchQuery,
        _selectedDay,
        CategoryRepository.categories
    ) { cal, allSchedules, query, selectedDay, categories ->
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
            categories = categories
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
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
}
