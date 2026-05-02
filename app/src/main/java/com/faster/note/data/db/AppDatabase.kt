package com.faster.note.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.faster.note.data.db.dao.CategoryDao
import com.faster.note.data.db.dao.ScheduleDao
import com.faster.note.data.db.entity.CategoryEntity
import com.faster.note.data.db.entity.ScheduleEntity

@Database(
    entities = [ScheduleEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DB_NAME = "schedule_app.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
