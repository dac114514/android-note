package com.faster.note.data

import com.faster.note.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AppUpdaterService {

    private const val API_URL = "https://api.github.com/repos/dac114514/android-note/releases/latest"

    suspend fun checkForUpdate(): UpdateResult? = withContext(Dispatchers.IO) {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode != 200) return@withContext null

            val json = JSONObject(BufferedReader(InputStreamReader(conn.inputStream)).readText())
            val tagName = json.getString("tag_name")

            if (isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
                UpdateResult(
                    latestVersion = tagName,
                    downloadUrl = json.getString("html_url"),
                    releaseNotes = json.optString("body", "")
                )
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun isNewerVersion(tag: String, current: String): Boolean {
        val parts = tag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val curParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts.size, curParts.size)) {
            val a = parts.getOrElse(i) { 0 }
            val b = curParts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    data class UpdateResult(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )
}
