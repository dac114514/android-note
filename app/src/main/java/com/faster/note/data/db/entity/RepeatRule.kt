package com.faster.note.data.db.entity

import org.json.JSONArray
import org.json.JSONObject

data class RepeatRule(
    val type: String = "none",       // "none" | "daily" | "weekly" | "monthly" | "custom"
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(),  // 1=Mon ... 7=Sun
    val endDate: Long? = null,
    val excludeDates: List<Long> = emptyList()
) {
    fun toJson(): String = JSONObject().apply {
        put("type", type)
        put("interval", interval)
        put("daysOfWeek", JSONArray(daysOfWeek))
        if (endDate != null) put("endDate", endDate)
        put("excludeDates", JSONArray(excludeDates))
    }.toString()

    companion object {
        fun fromJson(json: String?): RepeatRule {
            if (json.isNullOrBlank()) return RepeatRule()
            return try {
                val obj = JSONObject(json)
                RepeatRule(
                    type = obj.optString("type", "none"),
                    interval = obj.optInt("interval", 1),
                    daysOfWeek = obj.optJSONArray("daysOfWeek")?.let {
                        (0 until it.length()).map { i -> it.getInt(i) }
                    } ?: emptyList(),
                    endDate = if (obj.has("endDate")) obj.getLong("endDate") else null,
                    excludeDates = obj.optJSONArray("excludeDates")?.let {
                        (0 until it.length()).map { i -> it.getLong(i) }
                    } ?: emptyList()
                )
            } catch (_: Exception) { RepeatRule() }
        }
    }
}
