package com.swu.bianwanlu2_0.data.local.converter

import androidx.room.TypeConverter
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
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

    @TypeConverter
    fun categoryTypeToInt(type: CategoryType): Int = type.ordinal

    @TypeConverter
    fun intToCategoryType(value: Int): CategoryType = CategoryType.entries[value]

    @TypeConverter
    fun timelineItemTypeToInt(type: TimelineItemType): Int = type.ordinal

    @TypeConverter
    fun intToTimelineItemType(value: Int): TimelineItemType = TimelineItemType.entries[value]

    @TypeConverter
    fun timelineActionTypeToInt(type: TimelineActionType): Int = type.ordinal

    @TypeConverter
    fun intToTimelineActionType(value: Int): TimelineActionType = TimelineActionType.entries[value]
}
