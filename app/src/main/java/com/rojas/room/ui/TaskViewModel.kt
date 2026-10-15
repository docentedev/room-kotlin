package com.rojas.room.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rojas.room.data.TaskEntity
import com.rojas.room.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repo: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> =
        repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<TaskEntity?>(null)
    val selected: StateFlow<TaskEntity?> = _selected.asStateFlow()

    fun loadById(id: Long) {
        viewModelScope.launch { _selected.value = repo.getById(id) }
    }

    fun clearSelected() {
        _selected.value = null
    }

    fun save(title: String, onDone: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.save(title.trim())
            onDone()
        }
    }

    fun updateById(id: Long, title: String, done: Boolean, onDone: () -> Unit = {}) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.updateById(id, title.trim(), done)
            _selected.value = repo.getById(id)
            onDone()
        }
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch { repo.updateById(task.id, task.title, !task.done) }
    }

    fun deleteById(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteById(id)
            onDone()
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val repo: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo) as T
        }
    }
}
