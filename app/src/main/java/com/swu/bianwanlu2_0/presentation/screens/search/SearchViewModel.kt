package com.swu.bianwanlu2_0.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.SearchHistoryStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.NoteRepository
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SEARCH_GUEST_USER_ID = 1L
private const val SEARCH_HISTORY_SAVE_DELAY = 600L

enum class SearchItemType(val label: String) {
    NOTE("笔记"),
    TODO("待办"),
}

data class SearchCategoryOption(
    val id: Long?,
    val label: String,
)

data class SearchResultItem(
    val uniqueKey: String,
    val type: SearchItemType,
    val categoryId: Long?,
    val categoryLabel: String,
    val title: String,
    val content: String,
    val reminderTime: Long?,
    val createdAt: Long,
    val isCompleted: Boolean,
    val note: Note? = null,
    val todo: Todo? = null,
    val matchScore: Int,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    noteRepository: NoteRepository,
    todoRepository: TodoRepository,
    categoryRepository: CategoryRepository,
    private val searchHistoryStore: SearchHistoryStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchHistoryStore.history

    private var historySaveJob: Job? = null

    private val allCategories = categoryRepository
        .getAllCategories(SEARCH_GUEST_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryOptions: StateFlow<List<SearchCategoryOption>> = allCategories
        .combine(_selectedCategoryId) { categories, _ ->
            buildList {
                add(SearchCategoryOption(id = null, label = "全部分类"))
                categories
                    .sortedWith(compareBy<Category>({ it.type.ordinal }, { it.sortOrder }, { it.id }))
                    .forEach { category ->
                        add(
                            SearchCategoryOption(
                                id = category.id,
                                label = "${category.type.displayLabel()} · ${category.name}",
                            ),
                        )
                    }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf(SearchCategoryOption(null, "全部分类")),
        )

    val results: StateFlow<List<SearchResultItem>> = combine(
        noteRepository.getAllNotes(SEARCH_GUEST_USER_ID),
        todoRepository.getAllTodos(SEARCH_GUEST_USER_ID),
        allCategories,
        _selectedCategoryId,
        _submittedQuery,
    ) { notes, todos, categories, selectedCategoryId, submittedQuery ->
        buildSearchResults(
            notes = notes,
            todos = todos,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            query = submittedQuery.trim(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        _query.value = value
        val normalizedQuery = value.trim()
        _submittedQuery.value = normalizedQuery

        if (normalizedQuery.isBlank()) {
            historySaveJob?.cancel()
        } else {
            scheduleHistorySave(normalizedQuery)
        }
    }

    fun submitSearch() {
        val normalizedQuery = _query.value.trim()
        _submittedQuery.value = normalizedQuery
        historySaveJob?.cancel()
        searchHistoryStore.addQuery(normalizedQuery)
    }

    fun clearQuery() {
        historySaveJob?.cancel()
        _query.value = ""
        _submittedQuery.value = ""
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun selectHistory(query: String) {
        val normalizedQuery = query.trim()
        historySaveJob?.cancel()
        _query.value = normalizedQuery
        _submittedQuery.value = normalizedQuery
        searchHistoryStore.addQuery(normalizedQuery)
    }

    fun clearHistory() {
        searchHistoryStore.clearHistory()
    }

    private fun scheduleHistorySave(query: String) {
        historySaveJob?.cancel()
        historySaveJob = viewModelScope.launch {
            delay(SEARCH_HISTORY_SAVE_DELAY)
            searchHistoryStore.addQuery(query)
        }
    }

    private fun buildSearchResults(
        notes: List<Note>,
        todos: List<Todo>,
        categories: List<Category>,
        selectedCategoryId: Long?,
        query: String,
    ): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.lowercase()
        val categoriesById = categories.associateBy { it.id }

        val noteResults = notes
            .asSequence()
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .mapNotNull { note ->
                val categoryName = categoriesById[note.categoryId]?.name.orEmpty()
                val score = calculateMatchScore(
                    query = normalizedQuery,
                    title = note.title,
                    content = note.content,
                    categoryName = categoryName,
                )
                if (score == null) {
                    null
                } else {
                    SearchResultItem(
                        uniqueKey = "note_${note.id}",
                        type = SearchItemType.NOTE,
                        categoryId = note.categoryId,
                        categoryLabel = buildCategoryLabel(SearchItemType.NOTE, categoryName),
                        title = note.title,
                        content = note.content,
                        reminderTime = note.reminderTime,
                        createdAt = note.createdAt,
                        isCompleted = false,
                        note = note,
                        matchScore = score,
                    )
                }
            }

        val todoResults = todos
            .asSequence()
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .mapNotNull { todo ->
                val categoryName = categoriesById[todo.categoryId]?.name.orEmpty()
                val score = calculateMatchScore(
                    query = normalizedQuery,
                    title = todo.title,
                    content = todo.description.orEmpty(),
                    categoryName = categoryName,
                )
                if (score == null) {
                    null
                } else {
                    SearchResultItem(
                        uniqueKey = "todo_${todo.id}",
                        type = SearchItemType.TODO,
                        categoryId = todo.categoryId,
                        categoryLabel = buildCategoryLabel(SearchItemType.TODO, categoryName),
                        title = todo.title,
                        content = todo.description.orEmpty(),
                        reminderTime = todo.reminderTime,
                        createdAt = todo.createdAt,
                        isCompleted = todo.completedAt != null,
                        todo = todo,
                        matchScore = score,
                    )
                }
            }

        return (noteResults + todoResults)
            .sortedWith(
                compareBy<SearchResultItem> { it.matchScore }
                    .thenByDescending { it.reminderTime != null }
                    .thenByDescending { it.createdAt },
            )
            .toList()
    }

    private fun calculateMatchScore(
        query: String,
        title: String,
        content: String,
        categoryName: String,
    ): Int? {
        val normalizedTitle = title.lowercase()
        val normalizedContent = content.lowercase()
        val normalizedCategory = categoryName.lowercase()
        return when {
            normalizedTitle == query -> 0
            normalizedTitle.contains(query) -> 1
            normalizedContent.contains(query) -> 2
            normalizedCategory.contains(query) -> 3
            else -> null
        }
    }

    private fun buildCategoryLabel(type: SearchItemType, categoryName: String): String {
        if (categoryName.isBlank()) return type.label
        return "${type.label} · $categoryName"
    }

    private fun com.swu.bianwanlu2_0.data.local.entity.CategoryType.displayLabel(): String {
        return when (this) {
            com.swu.bianwanlu2_0.data.local.entity.CategoryType.NOTE -> "笔记"
            com.swu.bianwanlu2_0.data.local.entity.CategoryType.TODO -> "待办"
        }
    }
}
