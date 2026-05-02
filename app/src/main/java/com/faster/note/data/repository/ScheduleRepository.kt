package com.faster.note.data.repository

import com.faster.note.data.db.dao.ScheduleDao
import com.faster.note.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

class ScheduleRepository(private val dao: ScheduleDao) {

    private fun normalizeDate(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getMonthStart(year: Int, month: Int): Long = normalizeDate(year, month, 1)

    private fun getMonthEnd(year: Int, month: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return cal.timeInMillis
    }

    fun getSchedulesForDate(year: Int, month: Int, day: Int): Flow<List<ScheduleEntity>> {
        val date = normalizeDate(year, month, day)
        return dao.getSchedulesByDate(date)
    }

    fun getSchedulesForMonth(year: Int, month: Int): Flow<List<ScheduleEntity>> {
        return dao.getSchedulesByMonth(getMonthStart(year, month), getMonthEnd(year, month))
    }

    fun getScheduleById(id: Long): Flow<ScheduleEntity?> = dao.getScheduleById(id)

    fun search(query: String): Flow<List<ScheduleEntity>> = dao.searchSchedules(query)

    suspend fun insert(schedule: ScheduleEntity): Long = dao.insertSchedule(schedule)

    suspend fun update(schedule: ScheduleEntity) = dao.updateSchedule(schedule)

    suspend fun delete(id: Long) = dao.deleteScheduleById(id)

    suspend fun getTotalCount(year: Int, month: Int): Int =
        dao.getTotalCount(getMonthStart(year, month), getMonthEnd(year, month))

    suspend fun getCompletedCount(year: Int, month: Int): Int =
        dao.getCompletedCount(getMonthStart(year, month), getMonthEnd(year, month))

    suspend fun getDatesWithSchedules(year: Int, month: Int): List<Long> =
        dao.getDatesWithSchedules(getMonthStart(year, month), getMonthEnd(year, month))
}
