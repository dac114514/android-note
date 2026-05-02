package com.faster.note.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
import java.util.*

data class MonthUiState(
    val year: Int,
    val month: Int,
    val markedDateCounts: Map<Int, Int> = emptyMap(),
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val completedCount: Int = 0
)

class MonthViewModel : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MonthUiState> = combine(
        _currentMonth,
        ScheduleRepository.schedules,
        _searchQuery
    ) { cal, allSchedules, query ->
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

        MonthUiState(
            year = year,
            month = month,
            markedDateCounts = dateCounts,
            searchQuery = query,
            totalCount = filtered.size,
            completedCount = filtered.count { it.isCompleted }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState(0, 0))

    fun goToPreviousMonth() { _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, -1) } }
    fun goToNextMonth() { _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, 1) } }
    fun goToToday() { _currentMonth.value = Calendar.getInstance() }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
}
