package com.faster.note.data.repository

import com.faster.note.data.local.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object AiConfigRepository {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private fun configFile(): File =
        File(DataStore.appContext.filesDir, "ai_config.json")

    suspend fun loadAll() {
        withContext(Dispatchers.IO) {
            val file = configFile()
            if (file.exists()) {
                try {
                    val text = file.readText()
                    if (text.isNotBlank()) {
                        val json = JSONObject(text)
                        _apiKey.value = json.optString("apiKey", "")
                    }
                } catch (_: Exception) { /* use default */ }
            }
        }
    }

    fun saveApiKey(key: String) {
        _apiKey.value = key
        ioScope.launch {
            configFile().writeText(JSONObject().apply {
                put("apiKey", key)
            }.toString(2))
        }
    }
}
