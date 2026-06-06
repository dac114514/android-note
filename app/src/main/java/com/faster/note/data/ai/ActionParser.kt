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

sealed class ResponseBlock {
    data class Text(val content: String) : ResponseBlock()
    data class Card(val data: CardData) : ResponseBlock()
}

object ActionParser {

    private val ACTION_REGEX = Regex(
        """\[ACTION:(CREATE|READ|UPDATE|DELETE)]\s*(\{.*?\})\s*\[/ACTION]""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // Match [SCHEDULE_CARD:{json}] optionally wrapped in ``` or ```json
    private val FULL_CARD_REGEX = Regex(
        """(?:`{3}(?:json)?\s*)?\[SCHEDULE_CARD:\s*(\{.*?\})\s*\](?:\s*`{3})?""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    // Fallback: match bare JSON containing "id" (numeric) and "title" (string), field-order independent,
    // optionally wrapped in ``` or ```json
    private val BARE_CARD_REGEX = Regex(
        """(?:`{3}(?:json)?\s*)?(\{(?=[^}]*?"id\s*:\s*\d+)(?=[^}]*?"title\s*:\s*"[^"]*")[^}]*?\})(?:\s*`{3})?""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    // Clean orphaned brackets / code fences that remain after partial tag removal
    private val REMNANT_CLEANUP = Regex(
        """\[/?SCHEDULE_CARD:?\]|```(?:json)?\s*""",
        setOf(RegexOption.IGNORE_CASE)
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

    /**
     * Parse AI response text into a list of ResponseBlocks (Text or Card).
     * Preserves the original order of text and card tags in the response.
     * Two-stage parsing: primary FULL_CARD_REGEX, fallback BARE_CARD_REGEX.
     */
    fun parseResponseBlocks(text: String): List<ResponseBlock> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        val fullMatches = FULL_CARD_REGEX.findAll(trimmed).toList()
        val matches = if (fullMatches.isNotEmpty()) fullMatches
        else BARE_CARD_REGEX.findAll(trimmed).toList()

        if (matches.isEmpty()) {
            val cleaned = trimmed.replace(REMNANT_CLEANUP, "").trim()
            return if (cleaned.isNotBlank()) listOf(ResponseBlock.Text(cleaned)) else emptyList()
        }

        val blocks = mutableListOf<ResponseBlock>()
        var lastEnd = 0

        for (match in matches) {
            // Text segment before this match
            if (match.range.first > lastEnd) {
                val beforeText = trimmed.substring(lastEnd, match.range.first)
                val cleaned = beforeText.replace(REMNANT_CLEANUP, "").trim()
                if (cleaned.isNotBlank()) {
                    blocks.add(ResponseBlock.Text(cleaned))
                }
            }
            // Card segment from this match
            val cardJson = match.groupValues[1]
            try {
                val card = parseCardJson(JSONObject(cardJson))
                if (card != null) {
                    blocks.add(ResponseBlock.Card(card))
                }
            } catch (_: Exception) { }
            lastEnd = match.range.last + 1
        }

        // Text segment after the last match
        if (lastEnd < trimmed.length) {
            val afterText = trimmed.substring(lastEnd)
            val cleaned = afterText.replace(REMNANT_CLEANUP, "").trim()
            if (cleaned.isNotBlank()) {
                blocks.add(ResponseBlock.Text(cleaned))
            }
        }

        return blocks
    }

    fun parseScheduleCards(text: String): List<CardData> {
        return parseResponseBlocks(text)
            .filterIsInstance<ResponseBlock.Card>()
            .map { it.data }
    }

    fun stripTags(text: String): String {
        val blocks = parseResponseBlocks(text)
        val textContent = blocks
            .filterIsInstance<ResponseBlock.Text>()
            .joinToString("") { it.content }
        return textContent.replace(REMNANT_CLEANUP, "").trim()
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
}
