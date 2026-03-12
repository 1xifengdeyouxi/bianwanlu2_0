package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val noteDao: NoteDao,
    private val todoDao: TodoDao
) : CategoryRepository {

    private val ensureCategoryMutex = Mutex()

    override fun getCategories(userId: Long, type: CategoryType): Flow<List<Category>> =
        categoryDao.getByUserAndType(userId, type)

    override fun getAllCategories(userId: Long): Flow<List<Category>> =
        categoryDao.getAllByUser(userId)

    override fun countNotesByCategory(categoryId: Long): Flow<Int> =
        categoryDao.countNotesByCategory(categoryId)

    override fun countTodosByCategory(categoryId: Long): Flow<Int> =
        categoryDao.countTodosByCategory(categoryId)

    override suspend fun getCategoryCount(userId: Long, type: CategoryType): Int =
        categoryDao.countByUserAndType(userId, type)

    override suspend fun getFirstCategory(userId: Long, type: CategoryType): Category? =
        categoryDao.getFirstByUserAndType(userId, type)

    override suspend fun ensureDefaultCategory(userId: Long, type: CategoryType): Category {
        val category = ensureCategoryMutex.withLock {
            categoryDao.getFirstByUserAndType(userId, type)
                ?: Category(
                    name = defaultNameFor(type),
                    type = type,
                    sortOrder = categoryDao.getMaxSortOrder(userId, type) + 1,
                    userId = userId
                ).let { defaultCategory ->
                    val categoryId = categoryDao.insert(defaultCategory)
                    defaultCategory.copy(id = categoryId)
                }
        }

        val updatedAt = System.currentTimeMillis()
        when (type) {
            CategoryType.NOTE -> noteDao.assignUncategorized(userId, category.id, updatedAt)
            CategoryType.TODO -> todoDao.assignUncategorized(userId, category.id, updatedAt)
        }
        return category
    }

    override suspend fun getNextSortOrder(userId: Long, type: CategoryType): Int =
        categoryDao.getMaxSortOrder(userId, type) + 1

    override suspend fun insert(category: Category): Long =
        categoryDao.insert(category)

    override suspend fun update(category: Category) =
        categoryDao.update(category)

    override suspend fun updateOrder(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            categoryDao.update(category.copy(sortOrder = index))
        }
    }

    override suspend fun clearItemsInCategory(category: Category) {
        when (category.type) {
            CategoryType.NOTE -> noteDao.deleteByCategory(category.id)
            CategoryType.TODO -> todoDao.deleteByCategory(category.id)
        }
    }

    override suspend fun delete(category: Category, fallbackCategory: Category?) {
        val updatedAt = System.currentTimeMillis()
        val validFallback = fallbackCategory?.takeIf { it.type == category.type && it.id != category.id }

        when (category.type) {
            CategoryType.NOTE -> {
                if (validFallback != null) {
                    noteDao.moveCategory(category.id, validFallback.id, updatedAt)
                }
            }

            CategoryType.TODO -> {
                if (validFallback != null) {
                    todoDao.moveCategory(category.id, validFallback.id, updatedAt)
                }
            }
        }

        categoryDao.delete(category)
    }

    private fun defaultNameFor(type: CategoryType): String {
        return when (type) {
            CategoryType.NOTE -> "笔记"
            CategoryType.TODO -> "待办"
        }
    }
}
