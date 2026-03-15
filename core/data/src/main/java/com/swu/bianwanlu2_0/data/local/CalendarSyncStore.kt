package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.data.reminder.CalendarSyncEntry
import com.swu.bianwanlu2_0.data.reminder.ReminderItemType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEventId(itemType: ReminderItemType, itemId: Long): Long? {
        val key = keyFor(itemType, itemId)
        return if (preferences.contains(key)) {
            preferences.getLong(key, -1L).takeIf { it >= 0L }
        } else {
            null
        }
    }

    fun setEventId(itemType: ReminderItemType, itemId: Long, eventId: Long?) {
        val key = keyFor(itemType, itemId)
        if (eventId == null) {
            preferences.edit().remove(key).apply()
        } else {
            preferences.edit().putLong(key, eventId).apply()
        }
    }

    fun getAllEntries(): List<CalendarSyncEntry> {
        return preferences.all.mapNotNull { (key, value) -> parseEntry(key, value) }
    }

    fun clearAll() {
        val editor = preferences.edit()
        preferences.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .forEach(editor::remove)
        editor.apply()
    }

    private fun parseEntry(key: String, value: Any?): CalendarSyncEntry? {
        if (!key.startsWith(KEY_PREFIX)) return null
        val raw = key.removePrefix(KEY_PREFIX)
        val parts = raw.split('_', limit = 2)
        if (parts.size != 2) return null
        val itemType = runCatching {
            ReminderItemType.valueOf(parts[0].uppercase())
        }.getOrNull() ?: return null
        val itemId = parts[1].toLongOrNull() ?: return null
        val eventId = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        } ?: return null
        return CalendarSyncEntry(
            itemType = itemType,
            itemId = itemId,
            eventId = eventId,
        )
    }

    private fun keyFor(itemType: ReminderItemType, itemId: Long): String {
        return "$KEY_PREFIX${itemType.name.lowercase()}_$itemId"
    }

    private companion object {
        const val PREFS_NAME = "calendar_sync_store"
        const val KEY_PREFIX = "calendar_event_"
    }
}
