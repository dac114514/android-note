package com.faster.note.data.local

import android.content.Context
import com.faster.note.data.repository.AiConfigRepository
import com.faster.note.data.repository.CategoryRepository
import com.faster.note.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object DataStore {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            ScheduleRepository.loadAll()
            CategoryRepository.loadAll()
            AiConfigRepository.loadAll()
        }
    }
}
