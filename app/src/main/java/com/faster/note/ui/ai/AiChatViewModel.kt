package com.faster.note.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.ai.ActionParser
import com.faster.note.data.ai.DeepSeekService
import com.faster.note.data.ai.ResponseBlock
import com.faster.note.data.ai.ToolCall
import com.faster.note.data.db.entity.ScheduleEntity
import com.faster.note.data.repository.AiChatRepository
import com.faster.note.data.repository.AiConfigRepository
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ChatMessage
import com.faster.note.data.repository.MessageRole
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val apiKeyConfigured: Boolean = false,
    val messageCount: Int = 0
)

class AiChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var messageIdCounter = 0L

    init {
        viewModelScope.launch {
            val saved = AiChatRepository.loadAll()
            messageIdCounter = saved.maxOfOrNull { it.id } ?: 0L
            _uiState.value = _uiState.value.copy(
                messages = saved,
                apiKeyConfigured = AiConfigRepository.apiKey.value.isNotBlank(),
                messageCount = saved.size
            )
        }
        viewModelScope.launch {
            AiChatRepository.messages.collect { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    messageCount = messages.size
                )
            }
        }
        viewModelScope.launch {
            AiChatRepository.clearVersion.collectLatest { version ->
                if (version > 0L) {
                    messageIdCounter = 0L
                    _uiState.value = _uiState.value.copy(
                        messages = emptyList(),
                        messageCount = 0
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        val apiKey = AiConfigRepository.apiKey.value
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请先在设置中配置 API Key")
            return
        }

        val userMsg = ChatMessage(
            id = ++messageIdCounter,
            role = MessageRole.USER,
            content = text
        )

        val updatedMessages = _uiState.value.messages + userMsg
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            inputText = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val epochCtx = buildEpochContext()
                val todaySummary = buildTodaySummary()
                val systemPrompt = DeepSeekService.buildChatSystemPrompt(
                    epochCtx.dateStr, epochCtx.todayStartMillis, epochCtx.weekday
                ) + if (todaySummary.isNotBlank()) "\n\n今日已有日程：\n$todaySummary" else ""

                val messages = buildMessagesJson()
                val tools = DeepSeekService.buildToolsJson()

                // First API call with tools
                var result = DeepSeekService.chatCompletion(apiKey, messages, systemPrompt, tools)

                // Tool calling loop — max 10 iterations
                var loopCount = 0
                while (loopCount < 10 && result.toolCalls.isNotEmpty()) {
                    // Add assistant message with tool_calls to history
                    messages.put(buildAssistantToolCallMsg(result.toolCalls))

                    // Execute each tool call with verification + retry
                    for (tc in result.toolCalls) {
                        val execResult = executeToolWithRetry(tc.name, tc.arguments, maxRetries = 3)
                        messages.put(JSONObject().apply {
                            put("role", "tool")
                            put("tool_call_id", tc.id)
                            put("content", execResult)
                        })
                    }

                    result = DeepSeekService.chatCompletion(apiKey, messages, systemPrompt, tools)
                    loopCount++
                }

                val aiResponse = result.content

                // Parse final response — extract text and card blocks
                val blocks = ActionParser.parseResponseBlocks(aiResponse)
                val displayText = blocks
                    .filterIsInstance<ResponseBlock.Text>()
                    .joinToString("") { it.content }
                val cards = blocks
                    .filterIsInstance<ResponseBlock.Card>()
                    .map { it.data }

                val aiMsg = ChatMessage(
                    id = ++messageIdCounter,
                    role = MessageRole.AI,
                    content = displayText,
                    scheduleCards = cards
                )

                val finalMessages = _uiState.value.messages + aiMsg
                _uiState.value = _uiState.value.copy(
                    messages = finalMessages,
                    isLoading = false,
                    messageCount = finalMessages.size
                )

                AiChatRepository.saveMessages(finalMessages)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "请求失败"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearContext() {
        AiChatRepository.clearMessages()
    }

    // === Message building ===

    private fun buildMessagesJson(): JSONArray = JSONArray().apply {
        _uiState.value.messages.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.AI -> "assistant"
            }
            put(JSONObject().apply {
                put("role", role)
                put("content", msg.content)
            })
        }
    }

    private fun buildAssistantToolCallMsg(toolCalls: List<ToolCall>): JSONObject = JSONObject().apply {
        put("role", "assistant")
        put("content", null)
        put("tool_calls", JSONArray().apply {
            toolCalls.forEach { tc ->
                put(JSONObject().apply {
                    put("id", tc.id)
                    put("type", "function")
                    put("function", JSONObject().apply {
                        put("name", tc.name)
                        put("arguments", tc.arguments)
                    })
                })
            }
        })
    }

    // === Tool execution with verification and retry ===

    private fun executeToolWithRetry(name: String, argsJson: String, maxRetries: Int): String {
        val json = try { JSONObject(argsJson) } catch (_: Exception) { return "[ERROR] 无效的参数: $argsJson" }
        var lastResult = ""
        for (attempt in 0..maxRetries) {
            val result = try {
                when (name) {
                    "create_schedule" -> executeCreate(json)
                    "read_schedules" -> executeRead(json)
                    "update_schedule_date" -> executeUpdateDate(json)
                    "update_schedule_info" -> executeUpdateInfo(json)
                    "delete_schedule" -> executeDelete(json)
                    else -> "[ERROR] 未知工具: $name"
                }
            } catch (e: Exception) {
                "[ERROR] 执行失败: ${e.message}"
            }
            lastResult = result

            if (result.startsWith("[SUCCESS]")) {
                val verified = verifyToolResult(name, json)
                if (verified) return result
                // Verification failed, retry
                lastResult = "[RETRY] $result (验证未通过，第${attempt + 1}次重试)"
            } else if (result.startsWith("[ERROR]")) {
                // Parameter errors — don't retry, return immediately
                return result
            }
        }
        return lastResult
    }

    private fun verifyToolResult(name: String, json: JSONObject): Boolean {
        return when (name) {
            "create_schedule" -> {
                val title = json.optString("title", "")
                val date = json.optLong("date", -1L)
                if (title.isBlank() || date <= 0) return true // can't verify, assume success
                ScheduleRepository.schedules.value.any {
                    it.title == title && kotlin.math.abs(it.date - date) < 3600000
                }
            }
            "update_schedule_date" -> {
                val id = json.optLong("id", -1L)
                val newDate = json.optLong("date", -1L)
                if (id <= 0 || newDate <= 0) return true
                ScheduleRepository.schedules.value.find { it.id == id }
                    ?.let { kotlin.math.abs(it.date - newDate) < 3600000 }
                    ?: false
            }
            "update_schedule_info" -> {
                val id = json.optLong("id", -1L)
                id > 0 && ScheduleRepository.schedules.value.any { it.id == id }
            }
            "delete_schedule" -> {
                val id = json.optLong("id", -1L)
                id > 0 && ScheduleRepository.schedules.value.none { it.id == id }
            }
            else -> true
        }
    }

    // === Date helpers ===

    private data class EpochContext(
        val dateStr: String,
        val todayStartMillis: Long,
        val weekday: String
    )

    private fun buildEpochContext(): EpochContext {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINESE)
        val tz = TimeZone.getDefault()
        val gmtOffset = tz.getOffset(now) / 3600000
        val tzStr = if (gmtOffset >= 0) "GMT+$gmtOffset" else "GMT$gmtOffset"
        val dateStr = "${sdf.format(Date())} (${tzStr})"
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStartMillis = cal.timeInMillis
        val weekday = SimpleDateFormat("E", Locale.CHINESE).format(Date())
        return EpochContext(dateStr, todayStartMillis, weekday)
    }

    private fun getTodayDateRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val startOfNextDay = cal.timeInMillis
        return startOfDay to startOfNextDay
    }

    private fun buildTodaySummary(): String {
        val (todayStart, todayEnd) = getTodayDateRange()
        val todaySchedules = ScheduleRepository.schedules.value
            .filter { it.date >= todayStart && it.date < todayEnd }
            .sortedBy { it.date }
        if (todaySchedules.isEmpty()) return ""

        val cats = CategoryRepository.categories.value
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return todaySchedules.joinToString("\n") { s ->
            val cat = cats.find { it.id == s.categoryId }
            val timeInfo = if (s.isAllDay) "全天" else s.startTime?.let { tf.format(Date(it)) } ?: ""
            "[${s.id}] ${s.title} $timeInfo ${cat?.name ?: ""} ${if (s.isCompleted) "[完成]" else "[待办]"}"
        }
    }

    // === Tool implementations ===

    private fun executeCreate(json: JSONObject): String {
        val title = json.optString("title", "").ifBlank {
            return "[ERROR] 创建失败：缺少 title 参数"
        }
        val date = json.optLong("date", -1L).let {
            if (it <= 0) return "[ERROR] 创建失败：缺少或无效的 date 参数"
            it
        }
        val categoryName = json.optString("categoryName", "")
        var categoryWarn = ""
        val categoryId = if (categoryName.isNotBlank()) {
            CategoryRepository.categories.value.find {
                it.name.equals(categoryName, ignoreCase = true)
            }?.id.also {
                if (it == null) categoryWarn = " [WARN] 未找到匹配的类别: $categoryName"
            }
        } else null

        val schedule = ScheduleEntity(
            title = title,
            date = date,
            startTime = optLongSafe(json, "startTime"),
            endTime = optLongSafe(json, "endTime"),
            isAllDay = json.optBoolean("isAllDay", false),
            categoryId = categoryId,
            notes = json.optString("notes", null)
        )
        val scheduleId = ScheduleRepository.saveSchedule(schedule)
        return "[SUCCESS] 已创建日程：$title (ID: $scheduleId)$categoryWarn"
    }

    private fun executeRead(json: JSONObject): String {
        val startDate = json.optLong("startDate", -1L)
        val endDate = json.optLong("endDate", -1L)
        if (startDate <= 0 || endDate <= 0) return "[ERROR] 读取失败：缺少或无效的日期参数"

        val schedules = ScheduleRepository.schedules.value
            .filter { it.date in startDate..endDate }
            .sortedBy { it.date }

        if (schedules.isEmpty()) return "[ERROR] 该日期范围内没有日程"

        val cats = CategoryRepository.categories.value
        val dateFmt = SimpleDateFormat("yyyy-MM-dd(E)", Locale.CHINESE)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val cal = Calendar.getInstance()

        val sb = StringBuilder("[SUCCESS] 共找到 ${schedules.size} 个日程：\n")
        schedules.forEachIndexed { index, s ->
            cal.timeInMillis = s.date
            val dateStr = dateFmt.format(Date(s.date))
            val cat = cats.find { it.id == s.categoryId }
            val catName = cat?.name ?: ""
            val time = if (s.isAllDay) "全天"
            else if (s.startTime != null) {
                "${timeFmt.format(Date(s.startTime))}-${timeFmt.format(Date(s.endTime ?: s.startTime))}"
            } else ""
            val status = if (s.isCompleted) "已完成" else "待完成"
            sb.append("ID:${s.id} | $dateStr | $time | ${s.title} | $catName | $status")
            if (index < schedules.size - 1) sb.append("\n")
        }
        return sb.toString()
    }

    private fun executeUpdateDate(json: JSONObject): String {
        val id = parseId(json)
        if (id <= 0) return "[ERROR] 修改失败：缺少或无效的 id 参数"
        val newDate = json.optLong("date", -1L)
        if (newDate <= 0) return "[ERROR] 修改失败：缺少或无效的 date 参数"

        val existing = ScheduleRepository.schedules.value.find { it.id == id }
            ?: return "[ERROR] 未找到 ID 为 $id 的日程"

        ScheduleRepository.saveSchedule(existing.copy(date = newDate))
        return "[SUCCESS] 已更新日程 [${existing.title}] 的日期"
    }

    private fun executeUpdateInfo(json: JSONObject): String {
        val id = parseId(json)
        if (id <= 0) return "[ERROR] 修改失败：缺少或无效的 id 参数"

        val existing = ScheduleRepository.schedules.value.find { it.id == id }
            ?: return "[ERROR] 未找到 ID 为 $id 的日程"

        var updated = existing
        if (json.has("title")) updated = updated.copy(title = json.getString("title"))
        if (json.has("startTime")) {
            optLongSafe(json, "startTime")?.let { updated = updated.copy(startTime = it) }
        }
        if (json.has("endTime")) {
            optLongSafe(json, "endTime")?.let { updated = updated.copy(endTime = it) }
        }
        if (json.has("isAllDay")) updated = updated.copy(isAllDay = json.getBoolean("isAllDay"))
        if (json.has("isCompleted")) updated = updated.copy(isCompleted = json.getBoolean("isCompleted"))
        if (json.has("notes")) updated = updated.copy(notes = json.optString("notes"))
        if (json.has("categoryName")) {
            val cat = CategoryRepository.categories.value.find {
                it.name.equals(json.getString("categoryName"), ignoreCase = true)
            }
            if (cat != null) updated = updated.copy(categoryId = cat.id)
            else updated = updated.copy(notes = (updated.notes?.plus("; ") ?: "") + "[WARN] 未找到匹配的类别: ${json.getString("categoryName")}")
        }

        ScheduleRepository.saveSchedule(updated)
        return "[SUCCESS] 已更新日程：${updated.title}"
    }

    private fun executeDelete(json: JSONObject): String {
        val id = parseId(json)
        if (id <= 0) return "[ERROR] 删除失败：缺少或无效的 id 参数"
        val existing = ScheduleRepository.schedules.value.find { it.id == id }
            ?: return "[SUCCESS] 日程已被删除（ID: $id）"  // Already deleted — not an error on retry
        ScheduleRepository.deleteSchedule(id)
        return "[SUCCESS] 已删除日程：${existing.title} (ID: $id)"
    }

    private fun optLongSafe(json: JSONObject, key: String): Long? {
        if (!json.has(key)) return null
        val v = json.opt(key)
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
    }

    private fun parseId(json: JSONObject): Long {
        val v = json.opt("id")
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}
