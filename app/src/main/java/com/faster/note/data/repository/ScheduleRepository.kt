package com.faster.note.data.repository

import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.local.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

object ScheduleRepository {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _schedules = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    val schedules: StateFlow<List<ScheduleEntity>> = _schedules.asStateFlow()

    private fun schedulesDir(): File =
        File(DataStore.appContext.filesDir, "schedules").also { it.mkdirs() }

    suspend fun loadAll() {
        val result = withContext(Dispatchers.IO) {
            val dir = schedulesDir()
            if (dir.listFiles().isNullOrEmpty()) {
                seedMockData(dir)
            } else {
                dir.listFiles()!!.flatMap { file ->
                    try {
                        val text = file.readText()
                        if (text.isBlank()) emptyList()
                        else JSONArray(text).let { arr ->
                            (0 until arr.length()).map { scheduleFromJson(arr.getJSONObject(it)) }
                        }
                    } catch (_: Exception) { emptyList() }
                }
            }
        }
        _schedules.value = result
    }

    private suspend fun seedMockData(dir: File): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        val mock = mockSchedules()
        val byDate = mock.groupBy { it.date }
        byDate.forEach { (dateMillis, items) ->
            val file = File(dir, dateToFileName(dateMillis))
            file.writeText(JSONArray().apply { items.forEach { put(scheduleToJson(it)) } }.toString(2))
        }
        mock
    }

    fun saveSchedule(schedule: ScheduleEntity) {
        val now = System.currentTimeMillis()
        _schedules.value = if (schedule.id == 0L) {
            val newId = (_schedules.value.maxOfOrNull { it.id } ?: 0) + 1
            _schedules.value + schedule.copy(id = newId, createdAt = now, updatedAt = now)
        } else {
            _schedules.value.map { if (it.id == schedule.id) schedule.copy(updatedAt = now) else it }
        }
        persistDate(schedule.date)
    }

    fun deleteSchedule(id: Long) {
        val target = _schedules.value.find { it.id == id } ?: return
        _schedules.value = _schedules.value.filter { it.id != id }
        persistDate(target.date)
    }

    fun toggleCompleted(id: Long) {
        _schedules.value = _schedules.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted, updatedAt = System.currentTimeMillis()) else it
        }
        _schedules.value.find { it.id == id }?.let { persistDate(it.date) }
    }

    private fun persistDate(dateMillis: Long) {
        ioScope.launch {
            val file = File(schedulesDir(), dateToFileName(dateMillis))
            val dayItems = _schedules.value.filter { isSameDay(it.date, dateMillis) }
            if (dayItems.isEmpty()) {
                file.delete()
            } else {
                file.writeText(JSONArray().apply { dayItems.forEach { put(scheduleToJson(it)) } }.toString(2))
            }
        }
    }

    // === JSON ===

    private fun scheduleToJson(s: ScheduleEntity): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("title", s.title)
        put("date", s.date)
        s.startTime?.let { put("startTime", it) }
        s.endTime?.let { put("endTime", it) }
        put("isAllDay", s.isAllDay)
        put("isCompleted", s.isCompleted)
        s.categoryId?.let { put("categoryId", it) }
        s.location?.let { put("location", it) }
        s.notes?.let { put("notes", it) }
        s.reminderMin?.let { put("reminderMin", it) }
        s.repeatRule?.let { put("repeatRule", it) }
        put("createdAt", s.createdAt)
        put("updatedAt", s.updatedAt)
    }

    private fun scheduleFromJson(obj: JSONObject): ScheduleEntity = ScheduleEntity(
        id = obj.getLong("id"),
        title = obj.getString("title"),
        date = obj.getLong("date"),
        startTime = if (obj.has("startTime")) obj.getLong("startTime") else null,
        endTime = if (obj.has("endTime")) obj.getLong("endTime") else null,
        isAllDay = obj.optBoolean("isAllDay", false),
        isCompleted = obj.optBoolean("isCompleted", false),
        categoryId = if (obj.has("categoryId")) obj.getLong("categoryId") else null,
        location = obj.optString("location", null),
        notes = obj.optString("notes", null),
        reminderMin = if (obj.has("reminderMin")) obj.getInt("reminderMin") else null,
        repeatRule = obj.optString("repeatRule", null),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt")
    )

    // === Utilities ===

    private fun dateToFileName(dateMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return "%04d-%02d-%02d.json".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
               calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    // === Mock Data ===

    private fun mockSchedules(): List<ScheduleEntity> {
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
