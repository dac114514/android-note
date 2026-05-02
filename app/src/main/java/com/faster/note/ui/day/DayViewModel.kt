package com.faster.note.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
import java.util.*

data class DayUiState(
    val year: Int,
    val month: Int,
    val day: Int,
    val schedules: List<ScheduleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class DayViewModel : ViewModel() {

    private val _currentDate = MutableStateFlow(Calendar.getInstance())

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
        CategoryRepository.categories
    ) { schedules, categories ->
        DayUiState(
            year = _currentDate.value.get(Calendar.YEAR),
            month = _currentDate.value.get(Calendar.MONTH) + 1,
            day = _currentDate.value.get(Calendar.DAY_OF_MONTH),
            schedules = schedules,
            categories = categories,
            completedCount = schedules.count { it.isCompleted },
            totalCount = schedules.size
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
}
