package com.faster.note.data.repository

import com.faster.note.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.*
import java.util.*

class ScheduleRepository {
    private val _schedules = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    private var nextId = 1L

    fun getSchedulesForDate(year: Int, month: Int, day: Int): Flow<List<ScheduleEntity>> {
        val (dateStart, dateEnd) = getDayRange(year, month, day)
        return _schedules.map { list ->
            list.filter { it.date in dateStart..dateEnd }
                .sortedWith(compareBy<ScheduleEntity> { !it.isAllDay }.thenBy { it.startTime ?: Long.MAX_VALUE })
        }
    }

    fun getSchedulesForMonth(year: Int, month: Int): Flow<List<ScheduleEntity>> {
        val (start, end) = getMonthRange(year, month)
        return _schedules.map { list ->
            list.filter { it.date in start..end }.sortedBy { it.date }
        }
    }

    fun getScheduleById(id: Long): Flow<ScheduleEntity?> =
        _schedules.map { list -> list.find { it.id == id } }

    fun search(query: String): Flow<List<ScheduleEntity>> =
        _schedules.map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.notes?.contains(query, ignoreCase = true) == true)
            }.sortedByDescending { it.date }
        }

    suspend fun insert(schedule: ScheduleEntity): Long {
        val id = nextId++
        val newSchedule = schedule.copy(id = id)
        _schedules.value = _schedules.value + newSchedule
        return id
    }

    suspend fun update(schedule: ScheduleEntity) {
        _schedules.value = _schedules.value.map { if (it.id == schedule.id) schedule else it }
    }

    suspend fun delete(id: Long) {
        _schedules.value = _schedules.value.filter { it.id != id }
    }

    suspend fun getTotalCount(year: Int, month: Int): Int {
        val (start, end) = getMonthRange(year, month)
        return _schedules.value.count { it.date in start..end }
    }

    suspend fun getCompletedCount(year: Int, month: Int): Int {
        val (start, end) = getMonthRange(year, month)
        return _schedules.value.count { it.date in start..end && it.isCompleted }
    }

    suspend fun getDatesWithSchedules(year: Int, month: Int): List<Long> {
        val (start, end) = getMonthRange(year, month)
        return _schedules.value.filter { it.date in start..end }
            .map { it.date }
            .distinct()
    }

    fun addAll(schedules: List<ScheduleEntity>) {
        _schedules.value = schedules
        nextId = (schedules.maxOfOrNull { it.id } ?: 0) + 1
    }

    private fun getDayRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to (cal.timeInMillis - 1)
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return start to cal.timeInMillis
    }
}
