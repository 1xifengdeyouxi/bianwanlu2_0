package com.swu.bianwanlu2_0.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _history = MutableStateFlow(readHistory())

    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun addQuery(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        val updatedHistory = buildList {
            add(normalized)
            addAll(
                _history.value.filterNot {
                    it.equals(normalized, ignoreCase = true)
                },
            )
        }.take(MAX_HISTORY_COUNT)

        persist(updatedHistory)
    }

    fun clearHistory() {
        persist(emptyList())
    }

    private fun persist(history: List<String>) {
        preferences.edit()
            .putString(KEY_HISTORY, JSONArray(history).toString())
            .apply()
        _history.value = history
    }

    private fun readHistory(): List<String> {
        val storedValue = preferences.getString(KEY_HISTORY, null).orEmpty()
        if (storedValue.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(storedValue)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optString(index).trim()
                    if (item.isNotBlank()) {
                        add(item)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "search_history"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY_COUNT = 12
    }
}
