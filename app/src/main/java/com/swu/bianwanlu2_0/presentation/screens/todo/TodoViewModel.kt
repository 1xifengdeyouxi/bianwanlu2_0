package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TodoFilter(val label: String) {
    ALL("全部"),
    IN_PROGRESS("进行中"),
    EXPIRED("已过期"),
    TODAY("今天"),
    RECENT_7_DAYS("最近"),
    HIGH_PRIORITY("高级优先")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val categorySelectionStore: CategorySelectionStore
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.TODO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _currentFilter = MutableStateFlow(TodoFilter.ALL)
    val currentFilter: StateFlow<TodoFilter> = _currentFilter.asStateFlow()

    val todos: StateFlow<List<Todo>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(emptyList())
            } else {
                todoRepository.getTodosByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredTodos: StateFlow<List<Todo>> = combine(todos, _currentFilter) { list, filter ->
        applyFilter(list, filter)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTodos: StateFlow<List<Todo>> = filteredTodos
        .map { list -> list.filter { it.status == TodoStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTodos: StateFlow<List<Todo>> = filteredTodos
        .map { list -> list.filter { it.status == TodoStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todoCount: StateFlow<Int> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(0)
            } else {
                todoRepository.countTodosByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val selectedCategoryName: StateFlow<String> = MutableStateFlow("待办").also { flow ->
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                flow.value = category?.name ?: "待办"
            }
        }
    }

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategory(GUEST_USER_ID, CategoryType.TODO)
        }

        viewModelScope.launch {
            categories.collect { categoryList ->
                if (categoryList.isEmpty()) return@collect

                val savedCategoryId = categorySelectionStore.getSelectedCategoryId(CategoryType.TODO)
                val currentCategory = _selectedCategory.value
                val matchedCategory = when {
                    currentCategory != null -> categoryList.firstOrNull { it.id == currentCategory.id }
                    savedCategoryId != null -> categoryList.find { it.id == savedCategoryId }
                    else -> categoryList.firstOrNull()
                }

                if (_selectedCategory.value?.id != matchedCategory?.id) {
                    _selectedCategory.value = matchedCategory
                }

                categorySelectionStore.setSelectedCategoryId(CategoryType.TODO, matchedCategory?.id)
            }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
        categorySelectionStore.setSelectedCategoryId(CategoryType.TODO, category?.id)
    }

    fun setFilter(filter: TodoFilter) {
        _currentFilter.value = filter
    }

    fun addTodo(
        title: String,
        reminderTime: Long? = null,
        isPriority: Boolean = false,
        cardColor: Long = Todo.DEFAULT_CARD_COLOR
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todoRepository.insert(
                Todo(
                    title = title.trim(),
                    categoryId = _selectedCategory.value?.id,
                    isPriority = isPriority,
                    reminderTime = reminderTime,
                    cardColor = cardColor,
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

    fun updateTodo(
        todo: Todo,
        title: String,
        reminderTime: Long?,
        isPriority: Boolean,
        cardColor: Long
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            todoRepository.update(
                todo.copy(
                    title = title.trim(),
                    reminderTime = reminderTime,
                    isPriority = isPriority,
                    cardColor = cardColor,
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

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }

    private fun applyFilter(list: List<Todo>, filter: TodoFilter): List<Todo> {
        val now = System.currentTimeMillis()
        val (todayStart, todayEnd) = getTodayRange()
        val (recentStart, recentEnd) = getNext7DaysRange()

        return when (filter) {
            TodoFilter.ALL -> list
            TodoFilter.IN_PROGRESS -> list.filter {
                it.status == TodoStatus.ACTIVE && (it.reminderTime == null || it.reminderTime >= now)
            }
            TodoFilter.EXPIRED -> list.filter {
                it.status == TodoStatus.ACTIVE && it.reminderTime != null && it.reminderTime < now
            }
            TodoFilter.TODAY -> list.filter {
                it.reminderTime != null && it.reminderTime in todayStart..todayEnd
            }
            TodoFilter.RECENT_7_DAYS -> list.filter {
                it.reminderTime != null && it.reminderTime in recentStart..recentEnd
            }
            TodoFilter.HIGH_PRIORITY -> list.filter { it.isPriority }
        }
    }

    private fun getNext7DaysRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val end = cal.timeInMillis - 1
        return start to end
    }
}
