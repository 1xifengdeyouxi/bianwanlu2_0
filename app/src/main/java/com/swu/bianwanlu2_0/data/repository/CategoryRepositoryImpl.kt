package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategories(userId: Long, type: CategoryType): Flow<List<Category>> =
        categoryDao.getByUserAndType(userId, type)

    override fun getAllCategories(userId: Long): Flow<List<Category>> =
        categoryDao.getAllByUser(userId)

    override fun countNotesByCategory(categoryId: Long): Flow<Int> =
        categoryDao.countNotesByCategory(categoryId)

    override fun countTodosByCategory(categoryId: Long): Flow<Int> =
        categoryDao.countTodosByCategory(categoryId)

    override suspend fun insert(category: Category): Long =
        categoryDao.insert(category)

    override suspend fun update(category: Category) =
        categoryDao.update(category)

    override suspend fun delete(category: Category) =
        categoryDao.delete(category)
}
