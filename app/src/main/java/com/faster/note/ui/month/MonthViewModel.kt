package com.faster.note.ui.month

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.note.data.repository.ScheduleRepository
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

class MonthViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MonthUiState> = _currentMonth.flatMapLatest { cal ->
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        scheduleRepository.getSchedulesForMonth(year, month).map { schedules ->
            val dates = schedules.map { s ->
                val c = Calendar.getInstance().apply { timeInMillis = s.date }
                c.get(Calendar.DAY_OF_MONTH)
            }.toSet()
            MonthUiState(
                year = year,
                month = month,
                markedDates = dates,
                totalCount = schedules.size,
                completedCount = schedules.count { it.isCompleted }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthUiState(0, 0))

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, -1) }
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.apply { add(Calendar.MONTH, 1) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MonthViewModel(scheduleRepository) as T
        }
    }
}
