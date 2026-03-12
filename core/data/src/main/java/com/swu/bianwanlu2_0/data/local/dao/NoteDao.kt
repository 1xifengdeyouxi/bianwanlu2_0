package com.swu.bianwanlu2_0.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swu.bianwanlu2_0.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getAllByUser(userId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE user_id = :userId AND category_id = :categoryId ORDER BY updated_at DESC")
    fun getByUserAndCategory(userId: Long, categoryId: Long): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM notes WHERE user_id = :userId")
    fun countByUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE user_id = :userId AND category_id = :categoryId")
    fun countByUserAndCategory(userId: Long, categoryId: Long): Flow<Int>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Note?>

    @Query("UPDATE notes SET category_id = :targetCategoryId, updated_at = :updatedAt WHERE user_id = :userId AND category_id IS NULL")
    suspend fun assignUncategorized(userId: Long, targetCategoryId: Long, updatedAt: Long)

    @Query("UPDATE notes SET category_id = :targetCategoryId, updated_at = :updatedAt WHERE category_id = :categoryId")
    suspend fun moveCategory(categoryId: Long, targetCategoryId: Long, updatedAt: Long)

    @Query("DELETE FROM notes WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)
}
