package com.swu.bianwanlu2_0.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID

enum class TimelineItemType {
    NOTE,
    TODO,
}

enum class TimelineActionType(val label: String) {
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    COMPLETE("完成"),
    REMINDER("提醒"),
}

@Entity(
    tableName = "timeline_events",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["occurred_at"]),
        Index(value = ["category_id"]),
    ],
)
data class TimelineEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "item_type")
    val itemType: TimelineItemType,
    @ColumnInfo(name = "action_type")
    val actionType: TimelineActionType,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "category_name")
    val categoryName: String = "",
    val title: String = "",
    @ColumnInfo(name = "content_preview")
    val contentPreview: String = "",
    @ColumnInfo(name = "reference_time")
    val referenceTime: Long? = null,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: Long = GUEST_USER_ID,
)
