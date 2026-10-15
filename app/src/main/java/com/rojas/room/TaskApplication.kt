package com.rojas.room

import android.app.Application
import androidx.room.Room
import com.rojas.room.data.AppDatabase
import com.rojas.room.data.TaskRepository

class TaskApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "tasks.db").build()
    }

    val repository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }
}
