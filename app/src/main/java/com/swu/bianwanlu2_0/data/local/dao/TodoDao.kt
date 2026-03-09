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

    @Query("SELECT * FROM todos WHERE user_id = :userId ORDER BY status ASC, updated_at DESC")
    fun getAllByUser(userId: Long): Flow<List<Todo>>

    @Query("SELECT COUNT(*) FROM todos WHERE user_id = :userId")
    fun countByUser(userId: Long): Flow<Int>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Todo?>
}
