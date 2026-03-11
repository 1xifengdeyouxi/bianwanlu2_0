package com.swu.bianwanlu2_0.presentation.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val categorySelectionStore: CategorySelectionStore
) : ViewModel() {

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategory(GUEST_USER_ID, CategoryType.NOTE)
            categoryRepository.ensureDefaultCategory(GUEST_USER_ID, CategoryType.TODO)
        }
    }

    val noteCategories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.NOTE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todoCategories: StateFlow<List<Category>> = categoryRepository
        .getCategories(GUEST_USER_ID, CategoryType.TODO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val sortOrder = categoryRepository.getNextSortOrder(GUEST_USER_ID, type)
            categoryRepository.insert(
                Category(
                    name = name.trim(),
                    type = type,
                    sortOrder = sortOrder,
                    userId = GUEST_USER_ID
                )
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
            val selectedCategoryId = categorySelectionStore.getSelectedCategoryId(category.type)
            if (selectedCategoryId == category.id) {
                categorySelectionStore.setSelectedCategoryId(category.type, fallbackCategory?.id)
            }
            categoryRepository.delete(category, fallbackCategory)
        }
    }

    fun countItemsInCategory(category: Category): StateFlow<Int> {
        val flow = when (category.type) {
            CategoryType.NOTE -> categoryRepository.countNotesByCategory(category.id)
            CategoryType.TODO -> categoryRepository.countTodosByCategory(category.id)
        }
        return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
}
