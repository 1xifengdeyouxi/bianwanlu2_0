package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    val todos: StateFlow<List<Todo>> = repository
        .getAllTodos(GUEST_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todoCount: StateFlow<Int> = repository
        .countTodos(GUEST_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Todo(title = title.trim(), userId = GUEST_USER_ID)
            )
        }
    }

    fun toggleComplete(todo: Todo) {
        viewModelScope.launch {
            val newStatus = if (todo.status == TodoStatus.ACTIVE) TodoStatus.COMPLETED else TodoStatus.ACTIVE
            repository.update(
                todo.copy(
                    status = newStatus,
                    completedAt = if (newStatus == TodoStatus.COMPLETED) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            repository.delete(todo)
        }
    }
}
