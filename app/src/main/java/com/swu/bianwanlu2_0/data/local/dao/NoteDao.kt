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

    @Query("SELECT COUNT(*) FROM notes WHERE user_id = :userId")
    fun countByUser(userId: Long): Flow<Int>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Note?>
}
