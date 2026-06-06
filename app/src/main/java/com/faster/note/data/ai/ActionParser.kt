package com.faster.note.data.ai

import org.json.JSONObject

data class ActionBlock(val type: ActionType, val payload: JSONObject)

enum class ActionType { CREATE, READ, UPDATE, DELETE }

data class CardData(
    val scheduleId: Long,
    val title: String,
    val date: Long,
    val startTime: Long?,
    val endTime: Long?,
    val isAllDay: Boolean,
    val categoryName: String,
    val categoryColor: Int
)

object ActionParser {

    private val ACTION_REGEX = Regex(
        """\[ACTION:(CREATE|READ|UPDATE|DELETE)]\s*(\{.*?\})\s*\[/ACTION]""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val CARD_REGEX = Regex(
        """\[SCHEDULE_CARD:\s*(\{.*?\})\s*]""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    fun parseActions(text: String): List<ActionBlock> {
        return ACTION_REGEX.findAll(text).mapNotNull { match ->
            try {
                val type = ActionType.valueOf(match.groupValues[1])
                val json = JSONObject(match.groupValues[2])
                ActionBlock(type, json)
            } catch (_: Exception) { null }
        }.toList()
    }

    fun parseScheduleCards(text: String): List<CardData> {
        return CARD_REGEX.findAll(text).mapNotNull { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                CardData(
                    scheduleId = json.getLong("id"),
                    title = json.getString("title"),
                    date = json.getLong("date"),
                    startTime = if (json.has("startTime")) json.getLong("startTime") else null,
                    endTime = if (json.has("endTime")) json.getLong("endTime") else null,
                    isAllDay = json.optBoolean("isAllDay", false),
                    categoryName = json.optString("categoryName", ""),
                    categoryColor = json.optInt("categoryColor", 0xFF1565C0.toInt())
                )
            } catch (_: Exception) { null }
        }.toList()
    }

    fun stripTags(text: String): String {
        var result = text.replace(ACTION_REGEX, "").trim()
        result = CARD_REGEX.replace(result, "").trim()
        return result
    }
}
