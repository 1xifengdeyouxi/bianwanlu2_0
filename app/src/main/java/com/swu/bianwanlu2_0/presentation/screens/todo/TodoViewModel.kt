package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.TODO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val todos: StateFlow<List<Todo>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                todoRepository.getAllTodos(GUEST_USER_ID)
            } else {
                todoRepository.getTodosByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todoCount: StateFlow<Int> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                todoRepository.countTodos(GUEST_USER_ID)
            } else {
                todoRepository.countTodosByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val selectedCategoryName: StateFlow<String> = MutableStateFlow("待办").also { flow ->
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                (flow as MutableStateFlow).value = category?.name ?: "待办"
            }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todoRepository.insert(
                Todo(
                    title = title.trim(),
                    categoryId = _selectedCategory.value?.id,
                    userId = GUEST_USER_ID
                )
            )
        }
    }

    fun toggleComplete(todo: Todo) {
        viewModelScope.launch {
            val newStatus = if (todo.status == TodoStatus.ACTIVE) TodoStatus.COMPLETED else TodoStatus.ACTIVE
            todoRepository.update(
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
            todoRepository.delete(todo)
        }
    }
}
