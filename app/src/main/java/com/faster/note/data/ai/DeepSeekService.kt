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

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

data class ChatResult(
    val content: String,
    val toolCalls: List<ToolCall>
)

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

【可用工具】
你有以下工具可用：read_schedules, create_schedule, update_schedule_date, update_schedule_info, delete_schedule。
当用户询问日程安排、分析日程或任何需要查询数据时，**必须**调用 read_schedules，不能仅凭上下文回答。

【多步操作示例】
用户："把下午3点的会议改成4点"
调用 read_schedules(startDate=今天, endDate=今天) 查询找到会议 → 获得会议ID
调用 update_schedule_info(id=会议ID, startTime=新的时间, endTime=新的结束时间) 修改时间
AI 最终：根据工具返回的实际结果回复用户

【SCHEDULE_CARD 格式】
操作完成后，在回复中使用 [SCHEDULE_CARD:{json}] 标签嵌入可点击的日程卡片。
约束：
1. 始终提供自然语言描述文本，不要只输出标签
2. SCHEDULE_CARD 标签直接放在文本中，不要包裹在 ``` 或 ```json 内
3. SCHEDULE_CARD 的 json 必须包含：id(数字)、title、date、categoryColor(整数ARGB)
   可选字段：startTime、endTime、isAllDay、categoryName
正确示例：好的，已为您创建团队会议 [SCHEDULE_CARD:{"id":1,"title":"团队会议","date":1717000000000,"categoryColor":-10072528,"startTime":1717023600000,"endTime":1717030800000}]
错误示例：```json [SCHEDULE_CARD:{"id":1,"title":"团队会议","date":1717000000000}] ```

【日程展示规则】
根据日程数量选择展示方式：
- 如果展示 1-3 个日程：使用 [SCHEDULE_CARD:{json}] 标签为每个日程生成卡片，内嵌在文本中
- 如果展示 4 个及以上日程：使用 markdown 表格展示，格式如下：
  | ID | 日期 | 时间 | 标题 | 分组 | 状态 |
  |----|------|------|------|------|------|
  | 1 | 6月6日 | 15:00 | 团队会议 | 工作 | 待完成 |
  | 2 | 6月7日 | 09:00 | 站会 | 工作 | 已完成 |
  表格必须包含表头行、分隔行、数据行。每行以 | 开始和结束。

【重要：回复规则】
- **必须基于工具返回的实际操作结果回复用户**。不要虚构或提前假设操作结果。
- 如果操作失败（如找不到指定 ID 的日程、缺少必填参数等），必须在回复中如实告知用户失败原因。
- 工具执行结果会以 [SUCCESS]、[ERROR] 或 [RETRY] 开头反馈给你，请仔细阅读并根据结果做出回复。
- 回复使用中文，可使用 Markdown 格式（标题、加粗、列表等）。
"""

    fun buildToolsJson(): JSONArray = JSONArray().apply {
        put(toolObject("read_schedules", "读取指定日期范围内的所有日程。当用户询问日程安排、分析日程或任何需要查询日程数据时，必须调用此工具，不能仅凭上下文回答。", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("startDate", JSONObject().apply {
                    put("type", "number")
                    put("description", "开始日期当地午夜的纪元毫秒")
                })
                put("endDate", JSONObject().apply {
                    put("type", "number")
                    put("description", "结束日期当地午夜的纪元毫秒（包含该日期）")
                })
            })
            put("required", JSONArray().apply { put("startDate"); put("endDate") })
        }))
        put(toolObject("create_schedule", "创建新日程", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("title", JSONObject().apply {
                    put("type", "string")
                    put("description", "日程标题")
                })
                put("date", JSONObject().apply {
                    put("type", "number")
                    put("description", "日期当地午夜的纪元毫秒")
                })
                put("startTime", JSONObject().apply {
                    put("type", "number")
                    put("description", "开始时间的纪元毫秒（可选）")
                })
                put("endTime", JSONObject().apply {
                    put("type", "number")
                    put("description", "结束时间的纪元毫秒（可选）")
                })
                put("isAllDay", JSONObject().apply {
                    put("type", "boolean")
                    put("description", "是否全天日程")
                })
                put("categoryName", JSONObject().apply {
                    put("type", "string")
                    put("description", "分组名称（可选，可选值：工作/个人/学习/健康）")
                })
                put("notes", JSONObject().apply {
                    put("type", "string")
                    put("description", "备注（可选）")
                })
            })
            put("required", JSONArray().apply { put("title"); put("date") })
        }))
        put(toolObject("update_schedule_date", "修改已有日程的日期", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("id", JSONObject().apply {
                    put("type", "number")
                    put("description", "日程ID")
                })
                put("date", JSONObject().apply {
                    put("type", "number")
                    put("description", "新的日期当地午夜的纪元毫秒")
                })
            })
            put("required", JSONArray().apply { put("id"); put("date") })
        }))
        put(toolObject("update_schedule_info", "修改日程的详细信息（时间、标题、分组、完成状态、备注等）。只包含需要修改的字段。", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("id", JSONObject().apply {
                    put("type", "number")
                    put("description", "日程ID")
                })
                put("title", JSONObject().apply {
                    put("type", "string")
                    put("description", "新标题")
                })
                put("startTime", JSONObject().apply {
                    put("type", "number")
                    put("description", "新开始时间的纪元毫秒")
                })
                put("endTime", JSONObject().apply {
                    put("type", "number")
                    put("description", "新结束时间的纪元毫秒")
                })
                put("isAllDay", JSONObject().apply {
                    put("type", "boolean")
                    put("description", "是否全天日程")
                })
                put("categoryName", JSONObject().apply {
                    put("type", "string")
                    put("description", "分组名称（可选值：工作/个人/学习/健康）")
                })
                put("isCompleted", JSONObject().apply {
                    put("type", "boolean")
                    put("description", "是否已完成")
                })
                put("notes", JSONObject().apply {
                    put("type", "string")
                    put("description", "备注")
                })
            })
            put("required", JSONArray().apply { put("id") })
        }))
        put(toolObject("delete_schedule", "删除指定ID的日程", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("id", JSONObject().apply {
                    put("type", "number")
                    put("description", "日程ID")
                })
            })
            put("required", JSONArray().apply { put("id") })
        }))
    }

    private fun toolObject(name: String, desc: String, params: JSONObject) = JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", name)
            put("description", desc)
            put("parameters", params)
        })
    }

    suspend fun chatCompletion(
        apiKey: String,
        messages: JSONArray,
        systemPrompt: String,
        tools: JSONArray? = null
    ): ChatResult = withContext(Dispatchers.IO) {

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (i in 0 until messages.length()) {
                    put(messages.getJSONObject(i))
                }
            })
            if (tools != null) put("tools", tools)
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
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)
                val message = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                val content = message.optString("content", "")
                val toolCalls = if (message.has("tool_calls")) {
                    val arr = message.getJSONArray("tool_calls")
                    (0 until arr.length()).map { i ->
                        val tc = arr.getJSONObject(i)
                        val func = tc.getJSONObject("function")
                        ToolCall(
                            id = tc.getString("id"),
                            name = func.getString("name"),
                            arguments = func.getString("arguments")
                        )
                    }
                } else emptyList()
                ChatResult(content, toolCalls)
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
