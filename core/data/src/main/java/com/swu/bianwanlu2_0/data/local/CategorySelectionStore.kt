package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorySelectionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("category_selection", Context.MODE_PRIVATE)

    fun getSelectedCategoryId(userId: Long, type: CategoryType): Long? {
        val key = keyFor(userId, type)
        return if (preferences.contains(key)) preferences.getLong(key, -1L).takeIf { it >= 0L } else null
    }

    fun setSelectedCategoryId(userId: Long, type: CategoryType, categoryId: Long?) {
        val key = keyFor(userId, type)
        if (categoryId == null) {
            preferences.edit().remove(key).apply()
        } else {
            preferences.edit().putLong(key, categoryId).apply()
        }
    }

    fun copySelections(sourceUserId: Long, targetUserId: Long) {
        CategoryType.entries.forEach { type ->
            setSelectedCategoryId(targetUserId, type, getSelectedCategoryId(sourceUserId, type))
        }
    }

    fun clearAll(userId: Long) {
        preferences.edit()
            .remove(keyFor(userId, CategoryType.NOTE))
            .remove(keyFor(userId, CategoryType.TODO))
            .apply()
    }

    private fun keyFor(userId: Long, type: CategoryType): String {
        val suffix = when (type) {
            CategoryType.NOTE -> "note"
            CategoryType.TODO -> "todo"
        }
        return "selected_${suffix}_category_id_$userId"
    }
}
