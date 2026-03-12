package com.swu.bianwanlu2_0.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID

enum class TodoStatus { ACTIVE, COMPLETED }

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val description: String? = null,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    val status: TodoStatus = TodoStatus.ACTIVE,

    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,

    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,

    @ColumnInfo(name = "card_color")
    val cardColor: Long = DEFAULT_CARD_COLOR,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "user_id")
    val userId: Long = GUEST_USER_ID
) {
    companion object {
        const val DEFAULT_CARD_COLOR = 0xFFFFF8E1L // 默认淡黄色
    }
}
