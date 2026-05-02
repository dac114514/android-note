package com.faster.note.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object DeepSeekService {

    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MODEL = "deepseek-v4-flash"

    suspend fun requestAnalysis(apiKey: String, userPrompt: String): String =
        withContext(Dispatchers.IO) {
            val systemPrompt = "你是一个日程分析助手。用户会提供一个月内的日程列表，请从以下角度分析：" +
                    "1. 时间分配概况（各类日程占比）" +
                    "2. 完成情况分析（已完成/未完成）" +
                    "3. 发现的问题或建议（如时间安排不合理等）" +
                    "4. 下个月改进建议。" +
                    "请用中文回复，保持简洁有条理，200字左右。"

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 1024)
            }

            val conn = URL(BASE_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 30000
                conn.readTimeout = 30000

                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(
                        InputStreamReader(
                            if (responseCode in 200..299) conn.inputStream else conn.errorStream
                        )
                    )
                    val response = reader.readText()
                    val json = JSONObject(response)
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                    val errorBody = errorReader.readText()
                    val errorMsg = try {
                        JSONObject(errorBody).optString("error", "未知错误")
                    } catch (_: Exception) {
                        "HTTP $responseCode"
                    }
                    throw RuntimeException("API 请求失败: $errorMsg")
                }
            } finally {
                conn.disconnect()
            }
        }

    fun buildMonthPrompt(
        year: Int,
        month: Int,
        schedules: List<ScheduleInfo>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("${year}年${month}月日程列表：")
        sb.appendLine()

        val grouped = schedules.groupBy { it.day }
        for (day in grouped.keys.sorted()) {
            sb.appendLine("--- ${month}月${day}日 ---")
            grouped[day]?.forEach { s ->
                val completed = if (s.isCompleted) "[已完成]" else "[待完成]"
                val time = if (s.isAllDay) "全天" else s.timeRange
                val cat = if (s.category.isNotEmpty()) "(${s.category})" else ""
                sb.appendLine("  $completed $time ${s.title} $cat")
            }
            sb.appendLine()
        }

        sb.appendLine("总计：${schedules.size} 个日程")
        sb.appendLine("已完成：${schedules.count { it.isCompleted }}")
        sb.appendLine("未完成：${schedules.count { !it.isCompleted }}")
        return sb.toString()
    }

    data class ScheduleInfo(
        val day: Int,
        val title: String,
        val timeRange: String,
        val isAllDay: Boolean,
        val isCompleted: Boolean,
        val category: String
    )
}
