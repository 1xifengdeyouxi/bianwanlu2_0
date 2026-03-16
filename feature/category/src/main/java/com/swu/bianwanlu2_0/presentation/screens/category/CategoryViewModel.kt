package com.swu.bianwanlu2_0.presentation.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.CurrentUserStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val categorySelectionStore: CategorySelectionStore,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    init {
        viewModelScope.launch {
            currentUserStore.currentUserId
                .collect { userId ->
                    categoryRepository.ensureDefaultCategory(userId, CategoryType.NOTE)
                    categoryRepository.ensureDefaultCategory(userId, CategoryType.TODO)
                }
        }
    }

    val noteCategories: StateFlow<List<Category>> = currentUserStore.currentUserId
        .flatMapLatest { userId ->
            categoryRepository.getCategories(userId, CategoryType.NOTE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todoCategories: StateFlow<List<Category>> = currentUserStore.currentUserId
        .flatMapLatest { userId ->
            categoryRepository.getCategories(userId, CategoryType.TODO)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val userId = currentUserStore.peekCurrentUserId()
            val sortOrder = categoryRepository.getNextSortOrder(userId, type)
            categoryRepository.insert(
                Category(
                    name = name.trim(),
                    type = type,
                    sortOrder = sortOrder,
                    userId = userId,
                ),
            )
        }
    }

    fun updateCategory(category: Category, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            categoryRepository.update(category.copy(name = newName.trim()))
        }
    }

    fun reorderCategories(type: CategoryType, categories: List<Category>) {
        viewModelScope.launch {
            val sameTypeCategories = categories.filter { it.type == type }
            categoryRepository.updateOrder(sameTypeCategories)
        }
    }

    fun clearCategoryItems(category: Category) {
        viewModelScope.launch {
            categoryRepository.clearItemsInCategory(category)
        }
    }

    fun deleteCategory(category: Category, fallbackCategory: Category?) {
        viewModelScope.launch {
            val selectedCategoryId = categorySelectionStore.getSelectedCategoryId(category.userId, category.type)
            if (selectedCategoryId == category.id) {
                categorySelectionStore.setSelectedCategoryId(category.userId, category.type, fallbackCategory?.id)
            }
            categoryRepository.delete(category, fallbackCategory)
        }
    }

    fun countItemsInCategory(category: Category): Flow<Int> {
        return when (category.type) {
            CategoryType.NOTE -> categoryRepository.countNotesByCategory(category.id)
            CategoryType.TODO -> categoryRepository.countTodosByCategory(category.id)
        }
    }
}
