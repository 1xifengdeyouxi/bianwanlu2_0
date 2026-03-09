package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Todo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao
) : TodoRepository {

    override fun getAllTodos(userId: Long): Flow<List<Todo>> =
        todoDao.getAllByUser(userId)

    override fun getTodosByCategory(userId: Long, categoryId: Long): Flow<List<Todo>> =
        todoDao.getByUserAndCategory(userId, categoryId)

    override fun countTodos(userId: Long): Flow<Int> =
        todoDao.countByUser(userId)

    override fun countTodosByCategory(userId: Long, categoryId: Long): Flow<Int> =
        todoDao.countByUserAndCategory(userId, categoryId)

    override fun getTodoById(id: Long): Flow<Todo?> =
        todoDao.getById(id)

    override suspend fun insert(todo: Todo): Long =
        todoDao.insert(todo)

    override suspend fun update(todo: Todo) =
        todoDao.update(todo)

    override suspend fun delete(todo: Todo) =
        todoDao.delete(todo)
}
