package com.faster.note.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity
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
    private val _schedules = MutableStateFlow(mockSchedules())
    private val _categories = MutableStateFlow(mockCategories())

    val uiState: StateFlow<DayUiState> = combine(
        _currentDate.flatMapLatest { cal ->
            val dateStart = Calendar.getInstance().apply {
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dateEnd = dateStart + 86400000L - 1
            _schedules.map { list ->
                list.filter { it.date in dateStart..dateEnd }
                    .sortedWith(compareBy<ScheduleEntity> { !it.isAllDay }.thenBy { it.startTime ?: Long.MAX_VALUE })
            }
        },
        _categories
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
        _schedules.value = _schedules.value.map {
            if (it.id == schedule.id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun saveSchedule(schedule: ScheduleEntity) {
        _schedules.value = if (schedule.id == 0L) {
            val newId = (_schedules.value.maxOfOrNull { it.id } ?: 0) + 1
            _schedules.value + schedule.copy(id = newId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        } else {
            _schedules.value.map { if (it.id == schedule.id) schedule else it }
        }
    }

    fun deleteSchedule(id: Long) {
        _schedules.value = _schedules.value.filter { it.id != id }
    }

    companion object {
        fun mockCategories(): List<CategoryEntity> = listOf(
            CategoryEntity(id = 1, name = "工作", color = 0xFF1565C0.toInt(), isPreset = true, sortOrder = 1),
            CategoryEntity(id = 2, name = "个人", color = 0xFF43A047.toInt(), isPreset = true, sortOrder = 2),
            CategoryEntity(id = 3, name = "学习", color = 0xFFE53935.toInt(), isPreset = true, sortOrder = 3),
            CategoryEntity(id = 4, name = "健康", color = 0xFFFB8C00.toInt(), isPreset = true, sortOrder = 4),
        )

        fun mockSchedules(): List<ScheduleEntity> {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            return listOf(
                ScheduleEntity(id = 1, title = "团队站会", startTime = today + 9 * 3600000L, endTime = today + 9 * 3600000L + 1800000L, categoryId = 1, date = today, isCompleted = true),
                ScheduleEntity(id = 2, title = "午休", startTime = today + 12 * 3600000L, endTime = today + 13 * 3600000L, categoryId = 2, date = today),
                ScheduleEntity(id = 3, title = "学习 Jetpack Compose", startTime = today + 20 * 3600000L, endTime = today + 21 * 3600000L + 1800000L, categoryId = 3, date = today),
                ScheduleEntity(id = 4, title = "项目评审", startTime = today + 14 * 3600000L, endTime = today + 15 * 3600000L + 1800000L, categoryId = 1, date = today + 86400000L),
                ScheduleEntity(id = 5, title = "健身", startTime = today + 18 * 3600000L, endTime = today + 19 * 3600000L, categoryId = 4, date = today + 86400000L),
                ScheduleEntity(id = 6, title = "周末出游", isAllDay = true, categoryId = 2, date = today + 2 * 86400000L),
            )
        }
    }
}
