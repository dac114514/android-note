package com.faster.note.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.ui.day.DayViewModel
import kotlinx.coroutines.flow.*
import java.util.*

data class MonthUiState(
    val year: Int,
    val month: Int,
    val markedDates: Set<Int> = emptySet(),
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val completedCount: Int = 0
)

class MonthViewModel : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MonthUiState> = combine(
        _currentMonth.flatMapLatest { cal ->
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val monthStart = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val monthEnd = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1)
            }.timeInMillis

            val mockSchedules = DayViewModel.mockSchedules()
            val filtered = mockSchedules.filter { it.date in monthStart..monthEnd }
            val dates = filtered.map { s ->
                Calendar.getInstance().apply { timeInMillis = s.date }.get(Calendar.DAY_OF_MONTH)
            }.toSet()

            flowOf(MonthUiState(
                year = year,
                month = month,
                markedDates = dates,
                totalCount = filtered.size,
                completedCount = filtered.count { it.isCompleted }
            ))
        },
        _searchQuery
    ) { state, query -> state.copy(searchQuery = query) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState(0, 0))

    fun goToPreviousMonth() { _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, -1) } }
    fun goToNextMonth() { _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, 1) } }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
}
