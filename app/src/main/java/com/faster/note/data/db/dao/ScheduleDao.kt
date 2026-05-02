package com.faster.note.data.db.dao

import androidx.room.*
import com.faster.note.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE date = :date ORDER BY startTime ASC, isAllDay DESC")
    fun getSchedulesByDate(date: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, startTime ASC")
    fun getSchedulesByDateRange(startDate: Long, endDate: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE date >= :startMonth AND date <= :endMonth ORDER BY date ASC")
    fun getSchedulesByMonth(startMonth: Long, endMonth: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getScheduleById(id: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchSchedules(query: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE categoryId = :categoryId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSchedulesByCategory(categoryId: Long, startDate: Long, endDate: Long): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    // Stats queries
    @Query("SELECT COUNT(*) FROM schedules WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalCount(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE date >= :startDate AND date <= :endDate AND isCompleted = 1")
    suspend fun getCompletedCount(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE date >= :startDate AND date <= :endDate AND categoryId = :categoryId")
    suspend fun getCategoryCount(categoryId: Long, startDate: Long, endDate: Long): Int

    @Query("SELECT DISTINCT date FROM schedules WHERE date >= :startDate AND date <= :endDate")
    suspend fun getDatesWithSchedules(startDate: Long, endDate: Long): List<Long>
}
