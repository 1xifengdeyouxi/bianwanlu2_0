package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.TimelineEventRepository
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val categorySelectionStore: CategorySelectionStore,
    private val timelineEventRepository: TimelineEventRepository,
    private val reminderCoordinator: ReminderCoordinator,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.TODO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _currentFilter = MutableStateFlow(TodoFilter.ALL)
    val currentFilter: StateFlow<TodoFilter> = _currentFilter.asStateFlow()

    private val _refreshVersion = MutableStateFlow(0)

    private val _selectedTodoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTodoIds: StateFlow<Set<Long>> = _selectedTodoIds.asStateFlow()

    private val _selectionModeOverride = MutableStateFlow(false)

    val isSelectionMode: StateFlow<Boolean> = combine(_selectedTodoIds, _selectionModeOverride) { selectedIds, selectionOverride ->
        selectionOverride || selectedIds.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val todos: StateFlow<List<Todo>> = combine(_selectedCategory, _refreshVersion) { category, _ -> category }
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

    val selectedTodos: StateFlow<List<Todo>> = combine(todos, _selectedTodoIds) { list, selectedIds ->
        list.filter { it.id in selectedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

        viewModelScope.launch {
            todos
                .map { list -> list.map { it.id }.toSet() }
                .distinctUntilChanged()
                .collect { availableIds ->
                    val retained = _selectedTodoIds.value.filterTo(mutableSetOf()) { it in availableIds }
                    if (retained.size != _selectedTodoIds.value.size) {
                        _selectedTodoIds.value = retained
                    }
                }
        }
    }

    fun selectCategory(category: Category?) {
        clearSelection()
        _selectedCategory.value = category
        categorySelectionStore.setSelectedCategoryId(CategoryType.TODO, category?.id)
    }

    fun setFilter(filter: TodoFilter) {
        clearSelection()
        _currentFilter.value = filter
    }

    fun startSelectionMode() {
        _selectionModeOverride.value = true
    }

    fun enterSelection(todoId: Long) {
        _selectionModeOverride.value = true
        _selectedTodoIds.value = _selectedTodoIds.value + todoId
    }

    fun toggleSelection(todoId: Long) {
        _selectionModeOverride.value = true
        val current = _selectedTodoIds.value.toMutableSet()
        if (!current.add(todoId)) {
            current.remove(todoId)
        }
        _selectedTodoIds.value = current
    }

    fun clearSelection() {
        _selectionModeOverride.value = false
        _selectedTodoIds.value = emptySet()
    }

    fun selectAllFilteredTodos() {
        _selectionModeOverride.value = true
        _selectedTodoIds.value = filteredTodos.value.mapTo(mutableSetOf()) { it.id }
    }

    fun applyPriorityToSelectedTodos() {
        val selectedIds = _selectedTodoIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val selectedItems = selectedIds.mapNotNull { id ->
                todoRepository.getTodoById(id).first()
            }
            if (selectedItems.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val hasNonPriorityTodo = selectedItems.any { !it.isPriority }
            val todosToUpdate = if (hasNonPriorityTodo) {
                selectedItems.filter { !it.isPriority }
            } else {
                selectedItems.filter { it.isPriority }
            }

            todosToUpdate.forEach { todo ->
                val updatedTodo = todo.copy(
                    isPriority = hasNonPriorityTodo,
                    updatedAt = now,
                )
                todoRepository.update(updatedTodo)
                reminderCoordinator.syncTodo(updatedTodo)
                logTodoEvent(updatedTodo, TimelineActionType.UPDATE, now)
            }
            _refreshVersion.value += 1
            clearSelection()
        }
    }

    fun updateReminderForSelectedTodos(reminderTime: Long?) {
        val selectedIds = _selectedTodoIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val selectedItems = selectedIds.mapNotNull { id ->
                todoRepository.getTodoById(id).first()
            }
            if (selectedItems.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            selectedItems.forEach { todo ->
                if (todo.reminderTime != reminderTime) {
                    val updatedTodo = todo.copy(
                        reminderTime = reminderTime,
                        updatedAt = now,
                    )
                    todoRepository.update(updatedTodo)
                    reminderCoordinator.syncTodo(updatedTodo)
                    logTodoEvent(updatedTodo, TimelineActionType.REMINDER, now, reminderTime)
                }
            }
            _refreshVersion.value += 1
            clearSelection()
        }
    }

    fun deleteSelectedTodos() {
        val selectedIds = _selectedTodoIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val selectedItems = selectedIds.mapNotNull { id ->
                todoRepository.getTodoById(id).first()
            }
            selectedItems.forEach { todo ->
                logTodoEvent(todo, TimelineActionType.DELETE)
                reminderCoordinator.removeTodo(todo)
            }
            todoRepository.deleteByIds(selectedIds)
            _refreshVersion.value += 1
            clearSelection()
        }
    }

    fun reorderTodos(reorderedTodos: List<Todo>) {
        if (reorderedTodos.isEmpty()) return

        viewModelScope.launch {
            val baseOrder = System.currentTimeMillis()
            val now = System.currentTimeMillis()
            reorderedTodos.forEachIndexed { index, todo ->
                todoRepository.update(
                    todo.copy(
                        sortOrder = baseOrder - index,
                        updatedAt = now
                    )
                )
            }
            _refreshVersion.value += 1
        }
    }

    fun addTodo(
        title: String,
        reminderTime: Long? = null,
        isPriority: Boolean = false,
        cardColor: Long = Todo.DEFAULT_CARD_COLOR
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newTodo = Todo(
                title = title.trim(),
                categoryId = _selectedCategory.value?.id,
                isPriority = isPriority,
                reminderTime = reminderTime,
                cardColor = cardColor,
                createdAt = now,
                updatedAt = now,
                userId = GUEST_USER_ID,
            )
            val todoId = todoRepository.insert(newTodo)
            val savedTodo = newTodo.copy(id = todoId)
            reminderCoordinator.syncTodo(savedTodo)
            logTodoEvent(savedTodo, TimelineActionType.CREATE, now)
            if (reminderTime != null) {
                logTodoEvent(savedTodo, TimelineActionType.REMINDER, now, reminderTime)
            }
        }
    }

    fun toggleComplete(todo: Todo) {
        viewModelScope.launch {
            val newStatus = if (todo.status == TodoStatus.ACTIVE) TodoStatus.COMPLETED else TodoStatus.ACTIVE
            val now = System.currentTimeMillis()
            val updatedTodo = todo.copy(
                status = newStatus,
                completedAt = if (newStatus == TodoStatus.COMPLETED) now else null,
                updatedAt = now,
            )
            todoRepository.update(updatedTodo)
            reminderCoordinator.syncTodo(updatedTodo)
            logTodoEvent(
                updatedTodo,
                actionType = if (newStatus == TodoStatus.COMPLETED) TimelineActionType.COMPLETE else TimelineActionType.UPDATE,
                occurredAt = now,
            )
        }
    }

    suspend fun getTodoById(id: Long): Todo? {
        return todoRepository.getTodoById(id).first()
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
            val trimmedTitle = title.trim()
            val now = System.currentTimeMillis()
            val reminderChanged = todo.reminderTime != reminderTime
            val hasContentChanged = todo.title != trimmedTitle ||
                todo.isPriority != isPriority ||
                todo.cardColor != cardColor
            if (!reminderChanged && !hasContentChanged) return@launch

            val updatedTodo = todo.copy(
                title = trimmedTitle,
                reminderTime = reminderTime,
                isPriority = isPriority,
                cardColor = cardColor,
                updatedAt = now,
            )
            todoRepository.update(updatedTodo)
            reminderCoordinator.syncTodo(updatedTodo)
            if (hasContentChanged) {
                logTodoEvent(updatedTodo, TimelineActionType.UPDATE, now)
            }
            if (reminderChanged) {
                logTodoEvent(updatedTodo, TimelineActionType.REMINDER, now, reminderTime)
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            logTodoEvent(todo, TimelineActionType.DELETE)
            reminderCoordinator.removeTodo(todo)
            todoRepository.delete(todo)
        }
    }

    private suspend fun logTodoEvent(
        todo: Todo,
        actionType: TimelineActionType,
        occurredAt: Long = System.currentTimeMillis(),
        referenceTime: Long? = todo.reminderTime,
    ) {
        timelineEventRepository.insert(
            TimelineEvent(
                itemId = todo.id,
                itemType = TimelineItemType.TODO,
                actionType = actionType,
                categoryId = todo.categoryId,
                categoryName = resolveCategoryName(todo.categoryId, "待办"),
                title = todo.title.trim(),
                contentPreview = buildTodoContentPreview(todo),
                referenceTime = referenceTime,
                occurredAt = occurredAt,
                userId = todo.userId,
            )
        )
    }

    private fun resolveCategoryName(categoryId: Long?, fallback: String): String {
        return categoryId?.let { id ->
            categories.value.firstOrNull { it.id == id }?.name
        } ?: fallback
    }

    private fun buildTodoContentPreview(todo: Todo): String {
        return todo.description?.trim()?.takeIf { it.isNotBlank() }
            ?: todo.title.trim()
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
                val reminderTime = it.reminderTime
                it.status == TodoStatus.ACTIVE && (reminderTime == null || reminderTime >= now)
            }
            TodoFilter.EXPIRED -> list.filter {
                val reminderTime = it.reminderTime
                it.status == TodoStatus.ACTIVE && reminderTime != null && reminderTime < now
            }
            TodoFilter.TODAY -> list.filter {
                val reminderTime = it.reminderTime
                reminderTime != null && reminderTime in todayStart..todayEnd
            }
            TodoFilter.RECENT_7_DAYS -> list.filter {
                val reminderTime = it.reminderTime
                reminderTime != null && reminderTime in recentStart..recentEnd
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

