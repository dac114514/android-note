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
        """```?\s*\[SCHEDULE_CARD:\s*(\{.*?\})\s*]\s*```?""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )
    private val FALLBACK_CARD_REGEX = Regex(
        """\{[^}]*?"id"\s*:\s*\d+[^}]*?"title"\s*:\s*"[^"]*"[^}]*?\}""",
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
        val primary = CARD_REGEX.findAll(text).mapNotNull { match ->
            try {
                parseCardJson(JSONObject(match.groupValues[1]))
            } catch (_: Exception) { null }
        }.toList()
        if (primary.isNotEmpty()) return primary

        return FALLBACK_CARD_REGEX.findAll(text).mapNotNull { match ->
            try {
                parseCardJson(JSONObject(match.value))
            } catch (_: Exception) { null }
        }.toList()
    }

    private fun parseCardJson(json: JSONObject): CardData? {
        val id = json.optLong("id", -1L).takeIf { it > 0 }
            ?: json.optLong("scheduleId", -1L).takeIf { it > 0 }
            ?: return null
        val title = json.optString("title", "").ifBlank { return null }
        val date = json.optLong("date", 0L)
        return CardData(
            scheduleId = id,
            title = title,
            date = date,
            startTime = optLongSafe(json, "startTime"),
            endTime = optLongSafe(json, "endTime"),
            isAllDay = json.optBoolean("isAllDay", false),
            categoryName = json.optString("categoryName", ""),
            categoryColor = json.optInt("categoryColor", 0xFF1565C0.toInt())
        )
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

    fun stripTags(text: String): String {
        var result = text.replace(ACTION_REGEX, "").trim()
        result = CARD_REGEX.replace(result, "").trim()
        result = FALLBACK_CARD_REGEX.replace(result, "").trim()
        return result
    }
}
