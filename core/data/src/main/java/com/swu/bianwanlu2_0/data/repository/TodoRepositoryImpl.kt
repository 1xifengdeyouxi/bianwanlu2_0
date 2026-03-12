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

    override fun getActiveTodos(userId: Long): Flow<List<Todo>> =
        todoDao.getActiveByUser(userId)

    override fun getExpiredTodos(userId: Long, now: Long): Flow<List<Todo>> =
        todoDao.getExpiredByUser(userId, now)

    override fun getTodayTodos(userId: Long, dayStart: Long, dayEnd: Long): Flow<List<Todo>> =
        todoDao.getTodayByUser(userId, dayStart, dayEnd)

    override fun getTodosByDateRange(userId: Long, start: Long, end: Long): Flow<List<Todo>> =
        todoDao.getByDateRange(userId, start, end)

    override fun getPriorityTodos(userId: Long): Flow<List<Todo>> =
        todoDao.getPriorityByUser(userId)

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

    override suspend fun updatePriorityByIds(ids: List<Long>, isPriority: Boolean, updatedAt: Long) =
        todoDao.updatePriorityByIds(ids, isPriority, updatedAt)

    override suspend fun updateReminderByIds(ids: List<Long>, reminderTime: Long?, updatedAt: Long) =
        todoDao.updateReminderByIds(ids, reminderTime, updatedAt)

    override suspend fun deleteByIds(ids: List<Long>) =
        todoDao.deleteByIds(ids)
}
