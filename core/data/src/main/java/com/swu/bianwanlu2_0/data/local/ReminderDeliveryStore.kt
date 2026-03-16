package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.data.reminder.ReminderItemType
import com.swu.bianwanlu2_0.data.reminder.ReminderTriggerType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderDeliveryStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasDelivered(
        itemType: ReminderItemType,
        itemId: Long,
        triggerType: ReminderTriggerType,
        scheduledReminderTime: Long,
    ): Boolean {
        return preferences.getLong(keyFor(itemType, itemId, triggerType), Long.MIN_VALUE) == scheduledReminderTime
    }

    fun markDelivered(
        itemType: ReminderItemType,
        itemId: Long,
        triggerType: ReminderTriggerType,
        scheduledReminderTime: Long,
    ) {
        preferences.edit()
            .putLong(keyFor(itemType, itemId, triggerType), scheduledReminderTime)
            .apply()
    }

    fun clearForItem(itemType: ReminderItemType, itemId: Long) {
        val editor = preferences.edit()
        ReminderTriggerType.entries.forEach { triggerType ->
            editor.remove(keyFor(itemType, itemId, triggerType))
        }
        editor.apply()
    }

    private fun keyFor(
        itemType: ReminderItemType,
        itemId: Long,
        triggerType: ReminderTriggerType,
    ): String = "${itemType.name}_${itemId}_${triggerType.name}"

    private companion object {
        const val PREFS_NAME = "reminder_delivery_store"
    }
}
