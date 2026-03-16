package com.swu.bianwanlu2_0.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE user_id = :userId AND type = :type ORDER BY sort_order ASC")
    fun getByUserAndType(userId: Long, type: CategoryType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY type ASC, sort_order ASC")
    fun getAllByUser(userId: Long): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM notes WHERE category_id = :categoryId")
    fun countNotesByCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE category_id = :categoryId")
    fun countTodosByCategory(categoryId: Long): Flow<Int>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM categories WHERE user_id = :userId AND type = :type")
    suspend fun getMaxSortOrder(userId: Long, type: CategoryType): Int

    @Query("SELECT COUNT(*) FROM categories WHERE user_id = :userId AND type = :type")
    suspend fun countByUserAndType(userId: Long, type: CategoryType): Int

    @Query("SELECT * FROM categories WHERE user_id = :userId AND type = :type ORDER BY sort_order ASC LIMIT 1")
    suspend fun getFirstByUserAndType(userId: Long, type: CategoryType): Category?

    @Query("DELETE FROM categories WHERE user_id = :userId")
    suspend fun deleteAllByUser(userId: Long)

    @Query("UPDATE categories SET user_id = :targetUserId WHERE user_id = :sourceUserId")
    suspend fun reassignUser(sourceUserId: Long, targetUserId: Long)
}
