package com.faster.note.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.note.data.ai.ActionBlock
import com.faster.note.data.ai.ActionParser
import com.faster.note.data.ai.ActionType
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
import kotlinx.coroutines.launch
import org.json.JSONObject
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
                val currentMessages = _uiState.value.messages
                var aiResponse = callDeepSeek(apiKey, currentMessages)

                var maxLoops = 5
                while (maxLoops-- > 0) {
                    val actions = ActionParser.parseActions(aiResponse)
                    if (actions.isEmpty()) break

                    val results = executeActions(actions)
                    val resultText = results.joinToString("\n") { it }

                    val cleanPartial = ActionParser.stripTags(aiResponse)
                    val intermediateAiMsg = ChatMessage(
                        id = ++messageIdCounter,
                        role = MessageRole.AI,
                        content = cleanPartial.ifBlank { "(正在处理您的请求...)" }
                    )

                    val withIntermediate = _uiState.value.messages + intermediateAiMsg
                    _uiState.value = _uiState.value.copy(messages = withIntermediate)

                    aiResponse = DeepSeekService.sendChatMessage(
                        apiKey,
                        buildMessagePairs(_uiState.value.messages) + listOf("user" to "操作执行结果：\n$resultText\n请根据结果给用户回复。"),
                        DeepSeekService.CHAT_SYSTEM_PROMPT
                    )
                }

                val cards = ActionParser.parseScheduleCards(aiResponse)
                val cleanContent = ActionParser.stripTags(aiResponse)

                val aiMsg = ChatMessage(
                    id = ++messageIdCounter,
                    role = MessageRole.AI,
                    content = cleanContent,
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
        viewModelScope.launch {
            AiChatRepository.clearMessages()
            messageIdCounter = 0L
            _uiState.value = _uiState.value.copy(
                messages = emptyList(),
                messageCount = 0
            )
        }
    }

    private suspend fun callDeepSeek(apiKey: String, messages: List<ChatMessage>): String {
        return DeepSeekService.sendChatMessage(
            apiKey,
            buildMessagePairs(messages),
            DeepSeekService.CHAT_SYSTEM_PROMPT
        )
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
        val title = json.getString("title")
        val date = json.getLong("date")
        val categoryName = json.optString("categoryName", "")
        val categoryId = if (categoryName.isNotBlank()) {
            CategoryRepository.categories.value.find {
                it.name.equals(categoryName, ignoreCase = true)
            }?.id
        } else null

        val schedule = ScheduleEntity(
            title = title,
            date = date,
            startTime = if (json.has("startTime")) json.getLong("startTime") else null,
            endTime = if (json.has("endTime")) json.getLong("endTime") else null,
            isAllDay = json.optBoolean("isAllDay", false),
            categoryId = categoryId,
            notes = json.optString("notes", null)
        )
        ScheduleRepository.saveSchedule(schedule)
        return "已创建日程：$title (ID: ${schedule.id})"
    }

    private fun executeRead(json: JSONObject): String {
        val startDate = json.getLong("startDate")
        val endDate = json.getLong("endDate")
        val schedules = ScheduleRepository.schedules.value
            .filter { it.date in startDate..endDate }
            .sortedBy { it.date }

        if (schedules.isEmpty()) return "该日期范围内没有日程"

        val cats = CategoryRepository.categories.value
        val sdf = java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINESE)
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
                val tf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
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
        if (json.has("date")) updated = updated.copy(date = json.getLong("date"))
        if (json.has("startTime")) updated = updated.copy(startTime = json.optLong("startTime"))
        if (json.has("endTime")) updated = updated.copy(endTime = json.optLong("endTime"))
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
}
