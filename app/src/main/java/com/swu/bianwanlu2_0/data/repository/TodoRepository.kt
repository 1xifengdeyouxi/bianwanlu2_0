package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.entity.Todo
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getAllTodos(userId: Long): Flow<List<Todo>>
    fun countTodos(userId: Long): Flow<Int>
    fun getTodoById(id: Long): Flow<Todo?>
    suspend fun insert(todo: Todo): Long
    suspend fun update(todo: Todo)
    suspend fun delete(todo: Todo)
}
