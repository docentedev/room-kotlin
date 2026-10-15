package com.rojas.room.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    fun getAll(): Flow<List<TaskEntity>> = dao.getAll()

    suspend fun getById(id: Long): TaskEntity? = dao.getById(id)

    suspend fun save(title: String): Long =
        dao.save(TaskEntity(title = title))

    suspend fun updateById(id: Long, title: String, done: Boolean) {
        dao.getById(id)?.let { dao.update(it.copy(title = title, done = done)) }
    }

    suspend fun deleteById(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
    }
}
