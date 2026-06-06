package com.faster.note.data.repository

import com.faster.note.data.ai.CardData
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

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val scheduleCards: List<CardData> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole { USER, AI }

object AiChatRepository {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _clearVersion = MutableStateFlow(0L)
    val clearVersion: StateFlow<Long> = _clearVersion.asStateFlow()

    private fun chatFile(): File =
        File(DataStore.appContext.filesDir, "ai_chat_history.json")

    suspend fun loadAll(): List<ChatMessage> = withContext(Dispatchers.IO) {
        val file = chatFile()
        if (!file.exists()) return@withContext emptyList()
        try {
            val text = file.readText()
            if (text.isBlank()) emptyList().also { _messages.value = it }
            else JSONArray(text).let { arr ->
                (0 until arr.length()).map { chatMessageFromJson(arr.getJSONObject(it)) }
            }.also { _messages.value = it }
        } catch (_: Exception) { emptyList().also { _messages.value = it } }
    }

    fun saveMessage(msg: ChatMessage) {
        _messages.value = _messages.value + msg
        persistAllAsync()
    }

    fun saveMessages(newMessages: List<ChatMessage>) {
        _messages.value = newMessages
        persistAllAsync()
    }

    fun clearMessages() {
        _messages.value = emptyList()
        _clearVersion.value += 1
        chatFile().delete()
    }

    fun getMessageCount(): Int = _messages.value.size

    private fun persistAllAsync() {
        val data = _messages.value
        ioScope.launch {
            val file = chatFile()
            file.writeText(JSONArray().apply {
                data.forEach { put(chatMessageToJson(it)) }
            }.toString(2))
        }
    }

    private fun chatMessageToJson(msg: ChatMessage): JSONObject = JSONObject().apply {
        put("id", msg.id)
        put("role", msg.role.name)
        put("content", msg.content)
        put("timestamp", msg.timestamp)
        if (msg.scheduleCards.isNotEmpty()) {
            put("scheduleCards", JSONArray().apply {
                msg.scheduleCards.forEach { card ->
                    put(JSONObject().apply {
                        put("scheduleId", card.scheduleId)
                        put("title", card.title)
                        put("date", card.date)
                        card.startTime?.let { put("startTime", it) }
                        card.endTime?.let { put("endTime", it) }
                        put("isAllDay", card.isAllDay)
                        put("categoryName", card.categoryName)
                        put("categoryColor", card.categoryColor)
                    })
                }
            })
        }
    }

    private fun chatMessageFromJson(obj: JSONObject): ChatMessage = ChatMessage(
        id = obj.getLong("id"),
        role = MessageRole.valueOf(obj.getString("role")),
        content = obj.getString("content"),
        scheduleCards = if (obj.has("scheduleCards")) {
            val arr = obj.getJSONArray("scheduleCards")
            (0 until arr.length()).map { i ->
                val c = arr.getJSONObject(i)
                CardData(
                    scheduleId = c.getLong("scheduleId"),
                    title = c.getString("title"),
                    date = c.getLong("date"),
                    startTime = if (c.has("startTime")) c.getLong("startTime") else null,
                    endTime = if (c.has("endTime")) c.getLong("endTime") else null,
                    isAllDay = c.optBoolean("isAllDay", false),
                    categoryName = c.optString("categoryName", ""),
                    categoryColor = c.optInt("categoryColor", 0xFF1565C0.toInt())
                )
            }
        } else emptyList(),
        timestamp = obj.getLong("timestamp")
    )
}
