package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorySelectionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("category_selection", Context.MODE_PRIVATE)

    fun getSelectedCategoryId(type: CategoryType): Long? {
        val key = keyFor(type)
        return if (preferences.contains(key)) preferences.getLong(key, -1L).takeIf { it >= 0 } else null
    }

    fun setSelectedCategoryId(type: CategoryType, categoryId: Long?) {
        val key = keyFor(type)
        if (categoryId == null) {
            preferences.edit().remove(key).apply()
        } else {
            preferences.edit().putLong(key, categoryId).apply()
        }
    }

    private fun keyFor(type: CategoryType): String {
        return when (type) {
            CategoryType.NOTE -> "selected_note_category_id"
            CategoryType.TODO -> "selected_todo_category_id"
        }
    }
}
