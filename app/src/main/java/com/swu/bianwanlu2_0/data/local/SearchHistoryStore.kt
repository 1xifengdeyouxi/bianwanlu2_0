package com.swu.bianwanlu2_0.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
    private val currentUserStore: CurrentUserStore,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _history = MutableStateFlow(readHistory(currentUserStore.peekCurrentUserId()))

    val history: StateFlow<List<String>> = _history.asStateFlow()

    init {
        scope.launch {
            currentUserStore.currentUserId.collect { userId ->
                _history.value = readHistory(userId)
            }
        }
    }

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

        persist(currentUserStore.peekCurrentUserId(), updatedHistory)
    }

    fun clearHistory() {
        clearHistory(currentUserStore.peekCurrentUserId())
    }

    fun clearHistory(userId: Long) {
        persist(userId, emptyList())
    }

    private fun persist(userId: Long, history: List<String>) {
        preferences.edit()
            .putString(keyFor(userId), JSONArray(history).toString())
            .apply()
        if (userId == currentUserStore.peekCurrentUserId()) {
            _history.value = history
        }
    }

    private fun readHistory(userId: Long): List<String> {
        val storedValue = preferences.getString(keyFor(userId), null).orEmpty()
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

    private fun keyFor(userId: Long): String = "${KEY_HISTORY_PREFIX}_$userId"

    private companion object {
        const val PREFS_NAME = "search_history"
        const val KEY_HISTORY_PREFIX = "history"
        const val MAX_HISTORY_COUNT = 12
    }
}
