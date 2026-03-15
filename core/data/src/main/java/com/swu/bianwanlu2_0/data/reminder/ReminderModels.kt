package com.swu.bianwanlu2_0.data.reminder

enum class ReminderItemType {
    NOTE,
    TODO,
}

enum class ReminderTriggerType {
    DUE,
    EARLY,
}

enum class CalendarSyncToggleResult {
    ENABLED,
    DISABLED,
    PERMISSION_DENIED,
    NO_WRITABLE_CALENDAR,
}

data class CalendarSyncEntry(
    val itemType: ReminderItemType,
    val itemId: Long,
    val eventId: Long,
)
