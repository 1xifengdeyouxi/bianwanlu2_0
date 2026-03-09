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

    val status: NoteStatus = NoteStatus.ACTIVE,

    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** 预留用户 ID，登录功能完成后传入实际用户 ID */
    @ColumnInfo(name = "user_id")
    val userId: Long = GUEST_USER_ID
)
