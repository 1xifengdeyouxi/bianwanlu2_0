package com.swu.bianwanlu2_0.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID

enum class CategoryType { NOTE, TODO }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val type: CategoryType,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: Long = GUEST_USER_ID
)
