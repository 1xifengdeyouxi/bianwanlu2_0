package com.swu.bianwanlu2_0.data.local.converter

import androidx.room.TypeConverter
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus

class Converters {

    @TypeConverter
    fun noteStatusToInt(status: NoteStatus): Int = status.ordinal

    @TypeConverter
    fun intToNoteStatus(value: Int): NoteStatus = NoteStatus.entries[value]

    @TypeConverter
    fun todoStatusToInt(status: TodoStatus): Int = status.ordinal

    @TypeConverter
    fun intToTodoStatus(value: Int): TodoStatus = TodoStatus.entries[value]
}
