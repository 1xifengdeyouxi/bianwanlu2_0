package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.NoteRepository
import com.swu.bianwanlu2_0.data.repository.TimelineEventRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val categorySelectionStore: CategorySelectionStore,
    private val timelineEventRepository: TimelineEventRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.NOTE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _currentFilter = MutableStateFlow(NoteFilter.ALL)
    val currentFilter: StateFlow<NoteFilter> = _currentFilter.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedNoteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    val selectedNotes: StateFlow<List<Note>> = combine(notes, _selectedNoteIds) { list, selectedIds ->
        list.filter { it.id in selectedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                flowOf(0)
            } else {
                noteRepository.countNotesByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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

        viewModelScope.launch {
            notes
                .map { list -> list.map { it.id }.toSet() }
                .distinctUntilChanged()
                .collect { availableIds ->
                    val retained = _selectedNoteIds.value.filterTo(mutableSetOf()) { it in availableIds }
                    if (retained.size != _selectedNoteIds.value.size) {
                        _selectedNoteIds.value = retained
                    }
                }
        }
    }

    fun selectCategory(category: Category?) {
        clearSelection()
        _selectedCategory.value = category
        categorySelectionStore.setSelectedCategoryId(CategoryType.NOTE, category?.id)
    }

    fun setFilter(filter: NoteFilter) {
        clearSelection()
        _currentFilter.value = filter
    }

    fun enterSelection(noteId: Long) {
        _selectedNoteIds.value = _selectedNoteIds.value + noteId
    }

    fun toggleSelection(noteId: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (!current.add(noteId)) {
            current.remove(noteId)
        }
        _selectedNoteIds.value = current
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun selectAllFilteredNotes() {
        _selectedNoteIds.value = filteredNotes.value.mapTo(mutableSetOf()) { it.id }
    }

    fun applyPriorityToSelectedNotes() {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val selectedItems = selectedIds.mapNotNull { id ->
                noteRepository.getNoteById(id).first()
            }
            if (selectedItems.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val hasNonPriorityNote = selectedItems.any { !it.isPriority }
            val notesToUpdate = if (hasNonPriorityNote) {
                selectedItems.filter { !it.isPriority }
            } else {
                selectedItems.filter { it.isPriority }
            }

            notesToUpdate.forEach { note ->
                val updatedNote = note.copy(
                    isPriority = hasNonPriorityNote,
                    updatedAt = now,
                )
                noteRepository.update(updatedNote)
                logNoteEvent(updatedNote, TimelineActionType.UPDATE, now)
            }
            clearSelection()
        }
    }

    fun updateReminderForSelectedNotes(reminderTime: Long?) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val selectedItems = selectedIds.mapNotNull { id ->
                noteRepository.getNoteById(id).first()
            }
            if (selectedItems.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            selectedItems.forEach { note ->
                if (note.reminderTime != reminderTime) {
                    val updatedNote = note.copy(
                        reminderTime = reminderTime,
                        updatedAt = now,
                    )
                    noteRepository.update(updatedNote)
                    logNoteEvent(updatedNote, TimelineActionType.REMINDER, now, reminderTime)
                }
            }
            clearSelection()
        }
    }

    fun deleteSelectedNotes() {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            selectedIds.forEach { id ->
                noteRepository.getNoteById(id).first()?.let { note ->
                    logNoteEvent(note, TimelineActionType.DELETE)
                    noteRepository.delete(note)
                }
            }
            clearSelection()
        }
    }

    fun reorderNotes(reorderedNotes: List<Note>) {
        if (reorderedNotes.isEmpty()) return

        viewModelScope.launch {
            val baseOrder = System.currentTimeMillis()
            val now = System.currentTimeMillis()
            reorderedNotes.forEachIndexed { index, note ->
                noteRepository.update(
                    note.copy(
                        sortOrder = baseOrder - index,
                        updatedAt = now
                    )
                )
            }
        }
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
            val now = System.currentTimeMillis()
            val newNote = Note(
                title = title.trim(),
                content = content.trim(),
                categoryId = _selectedCategory.value?.id,
                reminderTime = reminderTime,
                isPriority = isPriority,
                cardColor = cardColor,
                textColor = textColor,
                imageUris = imageUris,
                createdAt = now,
                updatedAt = now,
                userId = GUEST_USER_ID,
            )
            val noteId = noteRepository.insert(newNote)
            val savedNote = newNote.copy(id = noteId)
            logNoteEvent(savedNote, TimelineActionType.CREATE, now)
            if (reminderTime != null) {
                logNoteEvent(savedNote, TimelineActionType.REMINDER, now, reminderTime)
            }
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
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()
            val now = System.currentTimeMillis()
            val reminderChanged = existing.reminderTime != reminderTime
            val hasContentChanged = existing.title != trimmedTitle ||
                existing.content != trimmedContent ||
                existing.isPriority != isPriority ||
                existing.cardColor != cardColor ||
                existing.textColor != textColor ||
                existing.imageUris != imageUris
            if (!reminderChanged && !hasContentChanged) return@launch

            val updatedNote = existing.copy(
                title = trimmedTitle,
                content = trimmedContent,
                reminderTime = reminderTime,
                isPriority = isPriority,
                cardColor = cardColor,
                textColor = textColor,
                imageUris = imageUris,
                updatedAt = now,
            )
            noteRepository.update(updatedNote)
            if (hasContentChanged) {
                logNoteEvent(updatedNote, TimelineActionType.UPDATE, now)
            }
            if (reminderChanged) {
                logNoteEvent(updatedNote, TimelineActionType.REMINDER, now, reminderTime)
            }
        }
    }

    fun toggleComplete(note: Note) {
        viewModelScope.launch {
            val newStatus = if (note.status == NoteStatus.ACTIVE) NoteStatus.COMPLETED else NoteStatus.ACTIVE
            val now = System.currentTimeMillis()
            val updatedNote = note.copy(
                status = newStatus,
                updatedAt = now,
            )
            noteRepository.update(updatedNote)
            logNoteEvent(
                updatedNote,
                actionType = if (newStatus == NoteStatus.COMPLETED) TimelineActionType.COMPLETE else TimelineActionType.UPDATE,
                occurredAt = now,
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            logNoteEvent(note, TimelineActionType.DELETE)
            noteRepository.delete(note)
        }
    }

    private suspend fun logNoteEvent(
        note: Note,
        actionType: TimelineActionType,
        occurredAt: Long = System.currentTimeMillis(),
        referenceTime: Long? = note.reminderTime,
    ) {
        timelineEventRepository.insert(
            TimelineEvent(
                itemId = note.id,
                itemType = TimelineItemType.NOTE,
                actionType = actionType,
                categoryId = note.categoryId,
                categoryName = resolveCategoryName(note.categoryId, "笔记"),
                title = note.title.trim(),
                contentPreview = buildNoteContentPreview(note),
                referenceTime = referenceTime,
                occurredAt = occurredAt,
                userId = note.userId,
            )
        )
    }

    private fun resolveCategoryName(categoryId: Long?, fallback: String): String {
        return categoryId?.let { id ->
            categories.value.firstOrNull { it.id == id }?.name
        } ?: fallback
    }

    private fun buildNoteContentPreview(note: Note): String {
        val trimmedContent = note.content.trim()
        return when {
            trimmedContent.isNotBlank() -> trimmedContent
            note.imageUris.isNotBlank() -> "图片笔记"
            note.title.isNotBlank() -> note.title.trim()
            else -> ""
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
