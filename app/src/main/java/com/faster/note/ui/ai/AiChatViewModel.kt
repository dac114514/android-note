package com.faster.note.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.ai.ActionParser
import com.faster.note.data.ai.DeepSeekService
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
                val currentDate = buildCurrentDateString()
                val todaySummary = buildTodaySummary()
                val systemPrompt = DeepSeekService.buildChatSystemPrompt(currentDate) +
                    if (todaySummary.isNotBlank()) "\n\n今日已有日程：\n$todaySummary" else ""

                // First API call
                var aiResponse = DeepSeekService.sendChatMessage(
                    apiKey,
                    buildMessagePairs(_uiState.value.messages),
                    systemPrompt
                )

                // Silent ReAct loop — no UI updates, preserves original responses for context
                var loopCount = 0
                while (loopCount < 5) {
                    val actions = ActionParser.parseActions(aiResponse)
                    if (actions.isEmpty()) break

                    val results = executeActions(actions)
                    val resultText = results.joinToString("\n") { it }

                    // Preserve original aiResponse (with tags) as assistant context for follow-up
                    val apiMessages = buildMessagePairs(_uiState.value.messages) +
                        listOf("assistant" to aiResponse) +
                        listOf("user" to "操作执行结果：\n$resultText\n请根据结果给用户回复。")

                    aiResponse = DeepSeekService.sendChatMessage(apiKey, apiMessages, systemPrompt)
                    loopCount++
                }

                // Single final response — parse blocks inline
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

    private fun buildMessagePairs(messages: List<ChatMessage>): List<Pair<String, String>> {
        return messages.map { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.AI -> "assistant"
            }
            role to msg.content
        }
    }

    // === Date helpers ===

    private fun buildCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)
        return sdf.format(Date())
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

    // === Action execution ===

    private fun executeActions(actions: List<ActionBlock>): List<String> {
        return actions.map { action ->
            try {
                when (action.type) {
                    ActionType.CREATE -> executeCreate(action.payload)
                    ActionType.READ -> executeRead(action.payload)
                    ActionType.UPDATE -> executeUpdate(action.payload)
                    ActionType.DELETE -> executeDelete(action.payload)
                }
            } catch (e: Exception) {
                "操作失败: ${e.message}"
            }
        }
    }

    private fun executeCreate(json: JSONObject): String {
        val title = json.optString("title", "").ifBlank {
            return "创建失败：缺少 title 参数"
        }
        val date = json.optLong("date", -1L).let {
            if (it <= 0) return "创建失败：缺少或无效的 date 参数"
            it
        }
        val categoryName = json.optString("categoryName", "")
        val categoryId = if (categoryName.isNotBlank()) {
            CategoryRepository.categories.value.find {
                it.name.equals(categoryName, ignoreCase = true)
            }?.id
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
        return "已创建日程：$title (ID: $scheduleId)"
    }

    private fun executeRead(json: JSONObject): String {
        val startDate = json.getLong("startDate")
        val endDate = json.getLong("endDate")
        val schedules = ScheduleRepository.schedules.value
            .filter { it.date in startDate..endDate }
            .sortedBy { it.date }

        if (schedules.isEmpty()) return "该日期范围内没有日程"

        val cats = CategoryRepository.categories.value
        val sdf = SimpleDateFormat("M月d日", Locale.CHINESE)
        val cal = Calendar.getInstance()

        val sb = StringBuilder()
        sb.appendLine("共找到 ${schedules.size} 个日程：")
        schedules.forEach { s ->
            cal.timeInMillis = s.date
            val dateStr = sdf.format(cal.time)
            val cat = cats.find { it.id == s.categoryId }
            val catName = if (cat != null) "(${cat.name})" else ""
            val time = if (s.isAllDay) "全天"
            else if (s.startTime != null) {
                val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "${tf.format(Date(s.startTime))}-${tf.format(Date(s.endTime ?: s.startTime))}"
            } else ""
            sb.appendLine("- [${s.id}] ${dateStr} $time ${s.title} $catName ${if (s.isCompleted) "[已完成]" else "[待完成]"}")
        }
        return sb.toString()
    }

    private fun executeUpdate(json: JSONObject): String {
        val id = json.getLong("id")
        val existing = ScheduleRepository.schedules.value.find { it.id == id }
            ?: return "未找到 ID 为 $id 的日程"

        var updated = existing
        if (json.has("title")) updated = updated.copy(title = json.getString("title"))
        if (json.has("date")) {
            val newDate = json.optLong("date", -1L)
            if (newDate > 0) updated = updated.copy(date = newDate)
        }
        if (json.has("startTime")) {
            optLongSafe(json, "startTime")?.let { updated = updated.copy(startTime = it) }
        }
        if (json.has("endTime")) {
            optLongSafe(json, "endTime")?.let { updated = updated.copy(endTime = it) }
        }
        if (json.has("isAllDay")) updated = updated.copy(isAllDay = json.getBoolean("isAllDay"))
        if (json.has("notes")) updated = updated.copy(notes = json.optString("notes"))
        if (json.has("isCompleted")) updated = updated.copy(isCompleted = json.getBoolean("isCompleted"))
        if (json.has("categoryName")) {
            val cat = CategoryRepository.categories.value.find {
                it.name.equals(json.getString("categoryName"), ignoreCase = true)
            }
            if (cat != null) updated = updated.copy(categoryId = cat.id)
        }

        ScheduleRepository.saveSchedule(updated)
        return "已更新日程：${updated.title}"
    }

    private fun executeDelete(json: JSONObject): String {
        val id = json.getLong("id")
        val existing = ScheduleRepository.schedules.value.find { it.id == id }
        if (existing == null) return "未找到 ID 为 $id 的日程"
        ScheduleRepository.deleteSchedule(id)
        return "已删除日程：${existing.title}"
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
}
