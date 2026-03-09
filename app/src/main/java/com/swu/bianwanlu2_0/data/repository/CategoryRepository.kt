package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(userId: Long, type: CategoryType): Flow<List<Category>>
    fun getAllCategories(userId: Long): Flow<List<Category>>
    fun countNotesByCategory(categoryId: Long): Flow<Int>
    fun countTodosByCategory(categoryId: Long): Flow<Int>
    suspend fun insert(category: Category): Long
    suspend fun update(category: Category)
    suspend fun delete(category: Category)
}
