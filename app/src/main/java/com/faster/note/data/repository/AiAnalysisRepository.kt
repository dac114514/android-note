package com.faster.note.data.repository

import com.faster.note.data.local.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AiAnalysisRepository {

    private fun analysisFile(year: Int, month: Int): File =
        File(DataStore.appContext.filesDir, "ai_analysis_${year}_${month}.json")

    suspend fun loadAnalysis(year: Int, month: Int): String = withContext(Dispatchers.IO) {
        val file = analysisFile(year, month)
        if (file.exists()) file.readText().trim() else ""
    }

    fun saveAnalysis(year: Int, month: Int, text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            analysisFile(year, month).writeText(text)
        }
    }
}
