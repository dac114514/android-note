package com.faster.note.data.repository

import com.faster.note.data.local.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

data class TokenUsageRecord(
    val timestamp: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class DailyTokenSummary(
    val date: String,
    val totalTokens: Int
)

object TokenUsageRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val records = mutableListOf<TokenUsageRecord>()

    private fun file(): File =
        File(DataStore.appContext.filesDir, "ai_token_usage.json")

    fun loadAll() {
        val f = file()
        if (!f.exists()) return
        try {
            val text = f.readText()
            if (text.isBlank()) return
            val arr = JSONArray(text)
            records.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                records.add(TokenUsageRecord(
                    timestamp = obj.getLong("timestamp"),
                    promptTokens = obj.getInt("promptTokens"),
                    completionTokens = obj.getInt("completionTokens"),
                    totalTokens = obj.getInt("totalTokens")
                ))
            }
        } catch (_: Exception) {
            records.clear()
        }
    }

    fun addRecord(record: TokenUsageRecord) {
        records.add(record)
        persistAsync()
    }

    fun getAllRecords(): List<TokenUsageRecord> = records.toList()

    fun getTodayRecords(): List<TokenUsageRecord> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val todayEnd = cal.timeInMillis
        return records.filter { it.timestamp in todayStart until todayEnd }
    }

    fun getWeeklySummary(): List<DailyTokenSummary> {
        val cal = Calendar.getInstance()
        val summaries = mutableMapOf<String, Int>()
        // Past 7 days (including today)
        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_MONTH, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = cal.timeInMillis
            val total = records
                .filter { it.timestamp in dayStart until dayEnd }
                .sumOf { it.totalTokens }
            val label = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
            summaries[label] = total
        }
        return summaries.entries.map { DailyTokenSummary(it.key, it.value) }
    }

    fun clearAll() {
        records.clear()
        persistAsync()
    }

    private fun persistAsync() {
        val data = records.toList()
        ioScope.launch {
            file().writeText(JSONArray().apply {
                data.forEach { r ->
                    put(JSONObject().apply {
                        put("timestamp", r.timestamp)
                        put("promptTokens", r.promptTokens)
                        put("completionTokens", r.completionTokens)
                        put("totalTokens", r.totalTokens)
                    })
                }
            }.toString(2))
        }
    }
}
