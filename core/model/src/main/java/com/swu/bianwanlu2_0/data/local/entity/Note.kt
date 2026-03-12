package com.swu.bianwanlu2_0.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID

enum class NoteStatus { ACTIVE, COMPLETED }

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val content: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    val status: NoteStatus = NoteStatus.ACTIVE,

    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,

    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,

    @ColumnInfo(name = "card_color")
    val cardColor: Long = DEFAULT_CARD_COLOR,

    @ColumnInfo(name = "text_color")
    val textColor: Long = DEFAULT_TEXT_COLOR,

    @ColumnInfo(name = "image_uris")
    val imageUris: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: Long = GUEST_USER_ID
) {
    companion object {
        const val DEFAULT_TEXT_COLOR: Long = 0xFF212121
        const val DEFAULT_CARD_COLOR: Long = 0xFFFFF8E1
    }
}
