package com.swu.bianwanlu2_0.data.reminder

import android.content.Intent

data class ReminderDeepLink(
    val itemType: ReminderItemType,
    val itemId: Long,
)

object ReminderDeepLinkContract {
    private const val EXTRA_OPEN_FROM_REMINDER = "extra_open_from_reminder"
    private const val EXTRA_ITEM_TYPE = "extra_reminder_item_type"
    private const val EXTRA_ITEM_ID = "extra_reminder_item_id"

    fun attach(intent: Intent, itemType: ReminderItemType, itemId: Long): Intent {
        return intent.apply {
            putExtra(EXTRA_OPEN_FROM_REMINDER, true)
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
    }

    fun parse(intent: Intent?): ReminderDeepLink? {
        intent ?: return null
        if (!intent.getBooleanExtra(EXTRA_OPEN_FROM_REMINDER, false)) return null
        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)
            ?.let { raw -> runCatching { ReminderItemType.valueOf(raw) }.getOrNull() }
            ?: return null
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it >= 0L } ?: return null
        return ReminderDeepLink(
            itemType = itemType,
            itemId = itemId,
        )
    }

    fun clear(intent: Intent?) {
        intent ?: return
        intent.removeExtra(EXTRA_OPEN_FROM_REMINDER)
        intent.removeExtra(EXTRA_ITEM_TYPE)
        intent.removeExtra(EXTRA_ITEM_ID)
    }
}
