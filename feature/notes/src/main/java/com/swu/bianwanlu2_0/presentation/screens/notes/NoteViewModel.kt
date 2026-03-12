package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.NoteRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class NoteFilter(val label: String) {
    ALL("全部"),
    IN_PROGRESS("进行中"),
    EXPIRED("已过期"),
    TODAY("今天"),
    RECENT_7_DAYS("最近"),
    HIGH_PRIORITY("高级优先")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val categorySelectionStore: CategorySelectionStore
) : ViewModel() {

    /** 笔记分类列表 */
    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.NOTE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前选中的分类 */
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter.asStateFlow()

    private val notesByCategory: Flow<List<Note>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(emptyList())
            } else {
                noteRepository.getNotesByCategory(GUEST_USER_ID, category.id)
            }
        }

    val notes: StateFlow<List<Note>> = notesByCategory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredNotes: StateFlow<List<Note>> = combine(notesByCategory, _currentFilter) { list, filter ->
        applyFilter(list, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeNotes: StateFlow<List<Note>> = filteredNotes
        .map { list -> list.filter { it.status == NoteStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedNotes: StateFlow<List<Note>> = filteredNotes
        .map { list -> list.filter { it.status == NoteStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(0)
            } else {
                noteRepository.countNotesByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 选中的分类名称，用于 TopBar 显示 */
    val selectedCategoryName: StateFlow<String> = MutableStateFlow("笔记").also { flow ->
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                flow.value = category?.name ?: "笔记"
            }
        }
    }

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategory(GUEST_USER_ID, CategoryType.NOTE)
        }

        viewModelScope.launch {
            categories.collect { categoryList ->
                if (categoryList.isEmpty()) return@collect

                val savedCategoryId = categorySelectionStore.getSelectedCategoryId(CategoryType.NOTE)
                val currentCategory = _selectedCategory.value
                val matchedCategory = when {
                    currentCategory != null -> categoryList.firstOrNull { it.id == currentCategory.id }
                    savedCategoryId != null -> categoryList.find { it.id == savedCategoryId }
                    else -> categoryList.firstOrNull()
                }

                if (_selectedCategory.value?.id != matchedCategory?.id) {
                    _selectedCategory.value = matchedCategory
                }

                categorySelectionStore.setSelectedCategoryId(CategoryType.NOTE, matchedCategory?.id)
            }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
        categorySelectionStore.setSelectedCategoryId(CategoryType.NOTE, category?.id)
    }

    fun setFilter(filter: NoteFilter) {
        _currentFilter.value = filter
    }

    fun addNote(
        title: String,
        content: String,
        reminderTime: Long? = null,
        isPriority: Boolean = false,
        cardColor: Long = Note.DEFAULT_CARD_COLOR,
        textColor: Long = Note.DEFAULT_TEXT_COLOR,
        imageUris: String = ""
    ) {
        if (title.isBlank() && content.isBlank() && imageUris.isBlank()) return
        viewModelScope.launch {
            noteRepository.insert(
                Note(
                    title = title.trim(),
                    content = content.trim(),
                    categoryId = _selectedCategory.value?.id,
                    reminderTime = reminderTime,
                    isPriority = isPriority,
                    cardColor = cardColor,
                    textColor = textColor,
                    imageUris = imageUris,
                    userId = GUEST_USER_ID
                )
            )
        }
    }

    fun updateNote(
        existing: Note,
        title: String,
        content: String,
        reminderTime: Long?,
        isPriority: Boolean,
        cardColor: Long,
        textColor: Long,
        imageUris: String
    ) {
        if (title.isBlank() && content.isBlank() && imageUris.isBlank()) return
        viewModelScope.launch {
            noteRepository.update(
                existing.copy(
                    title = title.trim(),
                    content = content.trim(),
                    reminderTime = reminderTime,
                    isPriority = isPriority,
                    cardColor = cardColor,
                    textColor = textColor,
                    imageUris = imageUris,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleComplete(note: Note) {
        viewModelScope.launch {
            val newStatus = if (note.status == NoteStatus.ACTIVE) NoteStatus.COMPLETED else NoteStatus.ACTIVE
            noteRepository.update(
                note.copy(
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    private fun applyFilter(list: List<Note>, filter: NoteFilter): List<Note> {
        val now = System.currentTimeMillis()
        val todayStart = getTodayStart()
        val todayEnd = getTodayEnd()
        val recentStart = getTodayStart()
        val recentEnd = getRecentEnd()

        return when (filter) {
            NoteFilter.ALL -> list
            NoteFilter.IN_PROGRESS -> list.filter {
                val reminderTime = it.reminderTime
                it.status == NoteStatus.ACTIVE &&
                        (reminderTime == null || reminderTime >= now)
            }
            NoteFilter.EXPIRED -> list.filter {
                val reminderTime = it.reminderTime
                it.status == NoteStatus.ACTIVE &&
                        reminderTime != null &&
                        reminderTime < now
            }
            NoteFilter.TODAY -> list.filter {
                val reminderTime = it.reminderTime
                reminderTime != null && reminderTime in todayStart..todayEnd
            }
            NoteFilter.RECENT_7_DAYS -> list.filter {
                val reminderTime = it.reminderTime
                reminderTime != null && reminderTime in recentStart..recentEnd
            }
            NoteFilter.HIGH_PRIORITY -> list.filter { it.isPriority }
        }
    }

    private fun getTodayStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getTodayEnd(): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = getTodayStart()
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - 1
    }

    private fun getRecentEnd(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }
        return cal.timeInMillis
    }
}
