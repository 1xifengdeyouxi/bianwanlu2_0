package com.swu.bianwanlu2_0.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swu.bianwanlu2_0.data.local.entity.Todo
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("SELECT * FROM todos WHERE user_id = :userId ORDER BY is_priority DESC, updated_at DESC")
    fun getAllByUser(userId: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND category_id = :categoryId ORDER BY is_priority DESC, updated_at DESC")
    fun getByUserAndCategory(userId: Long, categoryId: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND status = 0 ORDER BY is_priority DESC, updated_at DESC")
    fun getActiveByUser(userId: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND status = 0 AND reminder_time IS NOT NULL AND reminder_time < :now ORDER BY updated_at DESC")
    fun getExpiredByUser(userId: Long, now: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND reminder_time BETWEEN :dayStart AND :dayEnd ORDER BY is_priority DESC, reminder_time ASC")
    fun getTodayByUser(userId: Long, dayStart: Long, dayEnd: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND reminder_time BETWEEN :start AND :end ORDER BY is_priority DESC, reminder_time ASC")
    fun getByDateRange(userId: Long, start: Long, end: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE user_id = :userId AND is_priority = 1 ORDER BY updated_at DESC")
    fun getPriorityByUser(userId: Long): Flow<List<Todo>>

    @Query("SELECT COUNT(*) FROM todos WHERE user_id = :userId")
    fun countByUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE user_id = :userId AND category_id = :categoryId")
    fun countByUserAndCategory(userId: Long, categoryId: Long): Flow<Int>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Todo?>

    @Query("UPDATE todos SET category_id = :targetCategoryId, updated_at = :updatedAt WHERE user_id = :userId AND category_id IS NULL")
    suspend fun assignUncategorized(userId: Long, targetCategoryId: Long, updatedAt: Long)

    @Query("UPDATE todos SET category_id = :targetCategoryId, updated_at = :updatedAt WHERE category_id = :categoryId")
    suspend fun moveCategory(categoryId: Long, targetCategoryId: Long, updatedAt: Long)

    @Query("DELETE FROM todos WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)
}
