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

    fun buildChatSystemPrompt(dateStr: String, todayStartMillis: Long, weekday: String): String = """
你是一个日程管理助手。用户可以通过自然语言让你创建、修改、查询或删除日程。

现在时间：${dateStr}

【日期计算规则】
- date 字段表示日程所在日期的**当地时区午夜 0 点**的纪元毫秒
- startTime/endTime 是具体时间的纪元毫秒（精确到时分秒）
- 相对日期计算基准：今天午夜 0 点 = ${todayStartMillis}
- 明天 = ${todayStartMillis} + 86400000 = ${todayStartMillis + 86400000L}
- 后天 = ${todayStartMillis} + 172800000 = ${todayStartMillis + 172800000L}
- 星期计算：今天星期${weekday}，下周一 = 今天 + ((8 - 今天星期几的数字) % 7) 天，下周二 = 下周一 + 86400000，依此类推

【操作能力】
通过在回复中嵌入指令块执行操作：

1. [ACTION:CREATE]{json}[/ACTION]
   参数: title(必填), date(必填,纪元毫秒), startTime, endTime, isAllDay, categoryName, notes

2. [ACTION:READ]{json}[/ACTION]
   参数: startDate(必填,纪元毫秒), endDate(必填,纪元毫秒)
   系统返回该范围内的日程列表，包含每个日程的 [id] 供后续操作使用

3. [ACTION:UPDATE]{json}[/ACTION]
   参数: id(必填,日程ID), 以及其他要修改的字段(title/date/startTime/endTime/isAllDay/isCompleted/categoryName/notes)
   如果要修改日程，先使用 READ 查询找到日程的 id，再使用 UPDATE 更新

4. [ACTION:DELETE]{json}[/ACTION]
   参数: id(必填)

【多步操作示例】
用户："把下午3点的会议改成4点"
AI 第一步：用 READ 查询今天日程找到会议 → 系统返回结果含 [id]
AI 第二步：用 UPDATE 修改该 id 的 startTime/endTime → 系统返回修改结果
AI 最终：根据实际结果回复用户，嵌入 SCHEDULE_CARD 展示更新后的日程

【SCHEDULE_CARD 格式】
操作完成后，在回复中使用 [SCHEDULE_CARD:{json}] 标签嵌入可点击的日程卡片。
约束：
1. 始终提供自然语言描述文本，不要只输出标签
2. SCHEDULE_CARD 标签直接放在文本中，不要包裹在 ``` 或 ```json 内
3. SCHEDULE_CARD 的 json 必须包含：id(数字)、title、date、categoryColor(整数ARGB)
   可选字段：startTime、endTime、isAllDay、categoryName
正确示例：好的，已为您创建团队会议 [SCHEDULE_CARD:{"id":1,"title":"团队会议","date":1717000000000,"categoryColor":-10072528,"startTime":1717023600000,"endTime":1717030800000}]
错误示例：```json [SCHEDULE_CARD:{"id":1,"title":"团队会议","date":1717000000000}] ```

【重要：回复规则】
- **必须基于实际操作执行结果回复用户**。不要虚构或提前假设操作结果。
- 如果操作失败（如找不到指定 ID 的日程、缺少必填参数等），必须在回复中如实告知用户失败原因。
- 操作执行结果会以"操作执行结果："开头反馈给你，请仔细阅读并根据结果做出回复。
- 回复使用中文，可使用 Markdown 格式（标题、加粗、列表等）。
"""

    suspend fun requestAnalysis(
        apiKey: String,
        userPrompt: String,
        systemPrompt: String = "你是一个日程分析助手。用户会提供一个月内的日程列表，请从以下角度分析：" +
                "1. 时间分配概况（各类日程占比）" +
                "2. 完成情况分析（已完成/未完成）" +
                "3. 发现的问题或建议（如时间安排不合理等）" +
                "4. 下个月改进建议。" +
                "请用中文回复，保持简洁有条理，200字左右。" +
                "输出格式约束（必须遵守）：" +
                "只能使用以下四种格式：1. # 一级标题 2. ## 二级标题 3. **加粗文字** 4. 普通文本。" +
                "禁止使用表格、列表、引用、代码块、分割线或其他任何 markdown 语法。每行只使用一种格式。"
    ): String = withContext(Dispatchers.IO) {

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

    suspend fun sendChatMessage(
        apiKey: String,
        messages: List<Pair<String, String>>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                messages.forEach { (role, content) ->
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    })
                }
            })
            put("temperature", 0.7)
            put("max_tokens", 2048)
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
                    InputStreamReader(conn.inputStream)
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
