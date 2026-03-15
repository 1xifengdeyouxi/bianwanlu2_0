package com.swu.bianwanlu2_0.data.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.swu.bianwanlu2_0.data.local.CalendarSyncStore
import com.swu.bianwanlu2_0.data.local.ReminderSettingsStore
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class ReminderCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val todoDao: TodoDao,
    private val reminderSettingsStore: ReminderSettingsStore,
    private val calendarSyncStore: CalendarSyncStore,
    private val systemCalendarSyncHelper: SystemCalendarSyncHelper,
    private val reminderNotificationHelper: ReminderNotificationHelper,
) {
    private val appContext = context.applicationContext
    private val alarmManager: AlarmManager by lazy {
        appContext.getSystemService(AlarmManager::class.java)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun resyncAllAsync() {
        scope.launch { resyncAll() }
    }

    suspend fun resyncAll() {
        val notes = noteDao.getAllByUser(GUEST_USER_ID).first()
        val todos = todoDao.getAllByUser(GUEST_USER_ID).first()

        notes.forEach { syncNote(it) }
        todos.forEach { syncTodo(it) }

        cleanupOrphanedCalendarMappings(notes, todos)
    }

    suspend fun clearAllScheduledState() {
        val notes = noteDao.getAllByUser(GUEST_USER_ID).first()
        val todos = todoDao.getAllByUser(GUEST_USER_ID).first()

        notes.forEach { cancelReminderAlarms(ReminderItemType.NOTE, it.id) }
        todos.forEach { cancelReminderAlarms(ReminderItemType.TODO, it.id) }
        clearAllCalendarEvents()
    }

    suspend fun syncNote(note: Note) {
        cancelReminderAlarms(ReminderItemType.NOTE, note.id)
        scheduleReminderIfNeeded(
            itemType = ReminderItemType.NOTE,
            itemId = note.id,
            reminderTime = note.reminderTime,
            isPriority = note.isPriority,
            isActive = note.status == NoteStatus.ACTIVE,
        )
        syncCalendarForNote(note)
    }

    suspend fun syncTodo(todo: Todo) {
        cancelReminderAlarms(ReminderItemType.TODO, todo.id)
        scheduleReminderIfNeeded(
            itemType = ReminderItemType.TODO,
            itemId = todo.id,
            reminderTime = todo.reminderTime,
            isPriority = todo.isPriority,
            isActive = todo.status == TodoStatus.ACTIVE,
        )
        syncCalendarForTodo(todo)
    }

    suspend fun removeNote(note: Note) {
        cancelReminderAlarms(ReminderItemType.NOTE, note.id)
        removeCalendarEntry(ReminderItemType.NOTE, note.id)
    }

    suspend fun removeTodo(todo: Todo) {
        cancelReminderAlarms(ReminderItemType.TODO, todo.id)
        removeCalendarEntry(ReminderItemType.TODO, todo.id)
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean): CalendarSyncToggleResult {
        if (!enabled) {
            clearAllCalendarEvents()
            reminderSettingsStore.setCalendarSyncEnabled(false)
            return CalendarSyncToggleResult.DISABLED
        }

        if (!systemCalendarSyncHelper.hasCalendarPermissions()) {
            return CalendarSyncToggleResult.PERMISSION_DENIED
        }

        if (!systemCalendarSyncHelper.hasWritableCalendar()) {
            return CalendarSyncToggleResult.NO_WRITABLE_CALENDAR
        }

        reminderSettingsStore.setCalendarSyncEnabled(true)
        resyncAll()
        return CalendarSyncToggleResult.ENABLED
    }

    suspend fun handleAlarmIntent(intent: Intent) {
        val payload = parseAlarmPayload(intent) ?: return
        when (payload.itemType) {
            ReminderItemType.NOTE -> handleNoteAlarm(payload)
            ReminderItemType.TODO -> handleTodoAlarm(payload)
        }
    }

    private suspend fun handleNoteAlarm(payload: AlarmPayload) {
        val note = noteDao.getById(payload.itemId).first() ?: return
        val reminderTime = note.reminderTime ?: return
        if (note.status != NoteStatus.ACTIVE || reminderTime != payload.scheduledReminderTime) return
        if (payload.triggerType == ReminderTriggerType.EARLY) {
            if (!note.isPriority || System.currentTimeMillis() >= reminderTime) return
        }

        reminderNotificationHelper.showReminderNotification(
            itemType = ReminderItemType.NOTE,
            itemId = note.id,
            displayTitle = buildNoteDisplayTitle(note),
            detailText = buildNoteDetailText(note),
            reminderTime = reminderTime,
            isEarlyReminder = payload.triggerType == ReminderTriggerType.EARLY,
        )
    }

    private suspend fun handleTodoAlarm(payload: AlarmPayload) {
        val todo = todoDao.getById(payload.itemId).first() ?: return
        val reminderTime = todo.reminderTime ?: return
        if (todo.status != TodoStatus.ACTIVE || reminderTime != payload.scheduledReminderTime) return
        if (payload.triggerType == ReminderTriggerType.EARLY) {
            if (!todo.isPriority || System.currentTimeMillis() >= reminderTime) return
        }

        reminderNotificationHelper.showReminderNotification(
            itemType = ReminderItemType.TODO,
            itemId = todo.id,
            displayTitle = buildTodoDisplayTitle(todo),
            detailText = buildTodoDetailText(todo),
            reminderTime = reminderTime,
            isEarlyReminder = payload.triggerType == ReminderTriggerType.EARLY,
        )
    }

    private suspend fun syncCalendarForNote(note: Note) {
        val reminderTime = note.reminderTime
        val shouldSync = reminderSettingsStore.isCalendarSyncEnabled() &&
            note.status == NoteStatus.ACTIVE &&
            reminderTime != null

        if (!shouldSync) {
            removeCalendarEntry(ReminderItemType.NOTE, note.id)
            return
        }

        val eventId = systemCalendarSyncHelper.upsertReminderEvent(
            title = buildCalendarTitle(ReminderItemType.NOTE, buildNoteDisplayTitle(note)),
            description = buildCalendarDescription(buildNoteDetailText(note), reminderTime),
            reminderTime = reminderTime,
            existingEventId = calendarSyncStore.getEventId(ReminderItemType.NOTE, note.id),
        ) ?: return

        calendarSyncStore.setEventId(ReminderItemType.NOTE, note.id, eventId)
    }

    private suspend fun syncCalendarForTodo(todo: Todo) {
        val reminderTime = todo.reminderTime
        val shouldSync = reminderSettingsStore.isCalendarSyncEnabled() &&
            todo.status == TodoStatus.ACTIVE &&
            reminderTime != null

        if (!shouldSync) {
            removeCalendarEntry(ReminderItemType.TODO, todo.id)
            return
        }

        val eventId = systemCalendarSyncHelper.upsertReminderEvent(
            title = buildCalendarTitle(ReminderItemType.TODO, buildTodoDisplayTitle(todo)),
            description = buildCalendarDescription(buildTodoDetailText(todo), reminderTime),
            reminderTime = reminderTime,
            existingEventId = calendarSyncStore.getEventId(ReminderItemType.TODO, todo.id),
        ) ?: return

        calendarSyncStore.setEventId(ReminderItemType.TODO, todo.id, eventId)
    }

    private suspend fun cleanupOrphanedCalendarMappings(
        notes: List<Note>,
        todos: List<Todo>,
    ) {
        val activeKeys = buildSet {
            notes.forEach { add(mappingKey(ReminderItemType.NOTE, it.id)) }
            todos.forEach { add(mappingKey(ReminderItemType.TODO, it.id)) }
        }
        val staleEntries = calendarSyncStore.getAllEntries().filterNot {
            mappingKey(it.itemType, it.itemId) in activeKeys
        }
        if (staleEntries.isEmpty()) return

        staleEntries.forEach { entry ->
            if (systemCalendarSyncHelper.hasCalendarPermissions()) {
                systemCalendarSyncHelper.deleteEvent(entry.eventId)
            }
            calendarSyncStore.setEventId(entry.itemType, entry.itemId, null)
        }
    }

    private suspend fun clearAllCalendarEvents() {
        val entries = calendarSyncStore.getAllEntries()
        if (entries.isEmpty()) return

        if (systemCalendarSyncHelper.hasCalendarPermissions()) {
            entries.forEach { entry ->
                systemCalendarSyncHelper.deleteEvent(entry.eventId)
            }
        }
        calendarSyncStore.clearAll()
    }

    private suspend fun removeCalendarEntry(itemType: ReminderItemType, itemId: Long) {
        val eventId = calendarSyncStore.getEventId(itemType, itemId) ?: return
        if (systemCalendarSyncHelper.hasCalendarPermissions()) {
            systemCalendarSyncHelper.deleteEvent(eventId)
        }
        calendarSyncStore.setEventId(itemType, itemId, null)
    }

    private fun scheduleReminderIfNeeded(
        itemType: ReminderItemType,
        itemId: Long,
        reminderTime: Long?,
        isPriority: Boolean,
        isActive: Boolean,
    ) {
        if (!isActive || reminderTime == null) return
        if (reminderTime <= System.currentTimeMillis()) return

        scheduleAlarm(
            itemType = itemType,
            itemId = itemId,
            triggerAtMillis = reminderTime,
            scheduledReminderTime = reminderTime,
            triggerType = ReminderTriggerType.DUE,
        )

        if (isPriority) {
            val earlyTriggerTime = reminderTime - EARLY_REMINDER_OFFSET_MS
            if (earlyTriggerTime > System.currentTimeMillis()) {
                scheduleAlarm(
                    itemType = itemType,
                    itemId = itemId,
                    triggerAtMillis = earlyTriggerTime,
                    scheduledReminderTime = reminderTime,
                    triggerType = ReminderTriggerType.EARLY,
                )
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(
        itemType: ReminderItemType,
        itemId: Long,
        triggerAtMillis: Long,
        scheduledReminderTime: Long,
        triggerType: ReminderTriggerType,
    ) {
        val pendingIntent = createAlarmPendingIntent(
            itemType = itemType,
            itemId = itemId,
            scheduledReminderTime = scheduledReminderTime,
            triggerType = triggerType,
        )

        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }.onFailure {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun cancelReminderAlarms(itemType: ReminderItemType, itemId: Long) {
        ReminderTriggerType.entries.forEach { triggerType ->
            val pendingIntent = createAlarmPendingIntent(
                itemType = itemType,
                itemId = itemId,
                scheduledReminderTime = 0L,
                triggerType = triggerType,
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun createAlarmPendingIntent(
        itemType: ReminderItemType,
        itemId: Long,
        scheduledReminderTime: Long,
        triggerType: ReminderTriggerType,
    ): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_SCHEDULED_REMINDER_TIME, scheduledReminderTime)
            putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            buildRequestCode(itemType, itemId, triggerType),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun parseAlarmPayload(intent: Intent): AlarmPayload? {
        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)?.let {
            runCatching { ReminderItemType.valueOf(it) }.getOrNull()
        } ?: return null
        val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE)?.let {
            runCatching { ReminderTriggerType.valueOf(it) }.getOrNull()
        } ?: return null
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it >= 0L } ?: return null
        val scheduledReminderTime = intent.getLongExtra(EXTRA_SCHEDULED_REMINDER_TIME, -1L)
            .takeIf { it >= 0L } ?: return null
        return AlarmPayload(
            itemType = itemType,
            itemId = itemId,
            scheduledReminderTime = scheduledReminderTime,
            triggerType = triggerType,
        )
    }

    private fun buildRequestCode(
        itemType: ReminderItemType,
        itemId: Long,
        triggerType: ReminderTriggerType,
    ): Int {
        val raw = "alarm_${itemType.name}_${itemId}_${triggerType.name}".hashCode()
        return if (raw == Int.MIN_VALUE) 0 else kotlin.math.abs(raw)
    }

    private fun buildNoteDisplayTitle(note: Note): String {
        return note.title.trim().ifBlank {
            note.content.trim().takeIf { it.isNotBlank() }
                ?: if (note.imageUris.isNotBlank()) "图片笔记" else "笔记"
        }
    }

    private fun buildNoteDetailText(note: Note): String {
        return note.content.trim().takeIf { it.isNotBlank() }
            ?: if (note.imageUris.isNotBlank()) "包含图片内容" else "点击打开应用查看详情"
    }

    private fun buildTodoDisplayTitle(todo: Todo): String {
        return todo.title.trim().ifBlank {
            todo.description?.trim()?.takeIf { it.isNotBlank() } ?: "待办"
        }
    }

    private fun buildTodoDetailText(todo: Todo): String {
        return todo.description?.trim()?.takeIf { it.isNotBlank() }
            ?: "点击打开应用查看详情"
    }

    private fun buildCalendarTitle(itemType: ReminderItemType, title: String): String {
        val prefix = if (itemType == ReminderItemType.NOTE) {
            "[便玩录][笔记]"
        } else {
            "[便玩录][待办]"
        }
        return "$prefix $title"
    }

    private fun buildCalendarDescription(detailText: String, reminderTime: Long): String {
        return buildString {
            append(detailText)
            append("\n提醒时间：")
            append(formatReminderTime(reminderTime))
        }
    }

    private fun formatReminderTime(reminderTime: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }
        return "%04d-%02d-%02d %02d:%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    private fun mappingKey(itemType: ReminderItemType, itemId: Long): String = "${itemType.name}_$itemId"

    private data class AlarmPayload(
        val itemType: ReminderItemType,
        val itemId: Long,
        val scheduledReminderTime: Long,
        val triggerType: ReminderTriggerType,
    )

    companion object {
        private const val ACTION_REMINDER_ALARM = "com.swu.bianwanlu2_0.action.REMINDER_ALARM"
        private const val EXTRA_ITEM_TYPE = "extra_item_type"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_SCHEDULED_REMINDER_TIME = "extra_scheduled_reminder_time"
        private const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        private const val EARLY_REMINDER_OFFSET_MS = 15 * 60 * 1000L
    }
}
