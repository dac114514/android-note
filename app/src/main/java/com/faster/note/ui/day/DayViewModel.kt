package com.faster.note.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

class DayViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(Calendar.getInstance())
    val uiState: StateFlow<DayUiState> = combine(
        _currentDate.flatMapLatest { cal ->
            scheduleRepository.getSchedulesForDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        },
        categoryRepository.getAllCategories()
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

    fun goToPreviousDay() {
        _currentDate.value = _currentDate.value.apply { add(Calendar.DAY_OF_MONTH, -1) }
    }

    fun goToNextDay() {
        _currentDate.value = _currentDate.value.apply { add(Calendar.DAY_OF_MONTH, 1) }
    }

    fun goToToday() {
        _currentDate.value = Calendar.getInstance()
    }

    fun goToDate(year: Int, month: Int, day: Int) {
        _currentDate.value = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
    }

    fun toggleCompleted(schedule: ScheduleEntity) {
        viewModelScope.launch {
            scheduleRepository.update(schedule.copy(isCompleted = !schedule.isCompleted))
        }
    }

    fun saveSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            if (schedule.id == 0L) scheduleRepository.insert(schedule)
            else scheduleRepository.update(schedule)
        }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch {
            scheduleRepository.delete(id)
        }
    }

    class Factory(
        private val scheduleRepository: ScheduleRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DayViewModel(scheduleRepository, categoryRepository) as T
        }
    }
}
