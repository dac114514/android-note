package com.faster.note

import android.app.Application
import com.faster.note.data.db.AppDatabase

class ScheduleApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
}
