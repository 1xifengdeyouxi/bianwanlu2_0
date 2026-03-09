package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(userId: Long): Flow<List<Note>>
    fun countNotes(userId: Long): Flow<Int>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun insert(note: Note): Long
    suspend fun update(note: Note)
    suspend fun delete(note: Note)
}
