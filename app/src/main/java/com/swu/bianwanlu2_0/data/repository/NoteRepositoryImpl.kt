package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(userId: Long): Flow<List<Note>> =
        noteDao.getAllByUser(userId)

    override fun countNotes(userId: Long): Flow<Int> =
        noteDao.countByUser(userId)

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getById(id)

    override suspend fun insert(note: Note): Long =
        noteDao.insert(note)

    override suspend fun update(note: Note) =
        noteDao.update(note)

    override suspend fun delete(note: Note) =
        noteDao.delete(note)
}
