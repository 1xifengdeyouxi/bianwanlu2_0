package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.NoteRepository
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
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    /** 笔记分类列表 */
    val categories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.NOTE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前选中的分类，null 表示"全部" */
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    /** 根据选中分类自动切换数据源 */
    val notes: StateFlow<List<Note>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                noteRepository.getAllNotes(GUEST_USER_ID)
            } else {
                noteRepository.getNotesByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCount: StateFlow<Int> = _selectedCategory
        .flatMapLatest { category ->
            if (category == null) {
                noteRepository.countNotes(GUEST_USER_ID)
            } else {
                noteRepository.countNotesByCategory(GUEST_USER_ID, category.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 选中的分类名称，用于 TopBar 显示 */
    val selectedCategoryName: StateFlow<String> = MutableStateFlow("笔记").also { flow ->
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                (flow as MutableStateFlow).value = category?.name ?: "笔记"
            }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            noteRepository.insert(
                Note(
                    title = title.trim(),
                    content = content.trim(),
                    categoryId = _selectedCategory.value?.id,
                    userId = GUEST_USER_ID
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }
}
