package com.swu.bianwanlu2_0.data.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.swu.bianwanlu2_0.data.local.CalendarSyncStore
import com.swu.bianwanlu2_0.data.local.CurrentUserStore
import com.swu.bianwanlu2_0.data.local.ReminderSettingsStore
import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.repository.TimelineEventRepository
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
    private val categoryDao: CategoryDao,
    private val timelineEventRepository: TimelineEventRepository,
    private val currentUserStore: CurrentUserStore,
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
        resyncUser(currentUserStore.peekCurrentUserId())
    }

    suspend fun clearAllScheduledState() {
        clearScheduledStateForUser(currentUserStore.peekCurrentUserId())
    }

    suspend fun handleCurrentUserChanged(previousUserId: Long, currentUserId: Long) {
        if (previousUserId != currentUserId) {
            clearScheduledStateForUser(previousUserId)
        }
        resyncUser(currentUserId)
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

    suspend fun handleReceiverIntent(intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER_ALARM -> {
                val payload = parseAlarmPayload(intent) ?: return
                when (payload.itemType) {
                    ReminderItemType.NOTE -> handleNoteAlarm(payload)
                    ReminderItemType.TODO -> handleTodoAlarm(payload)
                }
            }

            ACTION_REMINDER_COMPLETE -> {
                val payload = parseActionPayload(intent) ?: return
                when (payload.itemType) {
                    ReminderItemType.NOTE -> completeNoteFromReminder(payload.itemId)
                    ReminderItemType.TODO -> completeTodoFromReminder(payload.itemId)
                }
            }

            ACTION_REMINDER_SNOOZE -> {
                val payload = parseActionPayload(intent) ?: return
                when (payload.itemType) {
                    ReminderItemType.NOTE -> snoozeNoteReminder(payload.itemId)
                    ReminderItemType.TODO -> snoozeTodoReminder(payload.itemId)
                }
            }
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

    private suspend fun completeNoteFromReminder(itemId: Long) {
        val note = noteDao.getById(itemId).first() ?: run {
            reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.NOTE, itemId)
            return
        }
        reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.NOTE, itemId)
        if (note.status == NoteStatus.COMPLETED) return

        val now = System.currentTimeMillis()
        val updatedNote = note.copy(
            status = NoteStatus.COMPLETED,
            updatedAt = now,
        )
        noteDao.update(updatedNote)
        syncNote(updatedNote)
        logNoteEvent(updatedNote, TimelineActionType.COMPLETE, now)
    }

    private suspend fun completeTodoFromReminder(itemId: Long) {
        val todo = todoDao.getById(itemId).first() ?: run {
            reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.TODO, itemId)
            return
        }
        reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.TODO, itemId)
        if (todo.status == TodoStatus.COMPLETED) return

        val now = System.currentTimeMillis()
        val updatedTodo = todo.copy(
            status = TodoStatus.COMPLETED,
            completedAt = now,
            updatedAt = now,
        )
        todoDao.update(updatedTodo)
        syncTodo(updatedTodo)
        logTodoEvent(updatedTodo, TimelineActionType.COMPLETE, now)
    }

    private suspend fun snoozeNoteReminder(itemId: Long) {
        val note = noteDao.getById(itemId).first() ?: run {
            reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.NOTE, itemId)
            return
        }
        reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.NOTE, itemId)
        if (note.status != NoteStatus.ACTIVE) return

        val now = System.currentTimeMillis()
        val snoozedTime = now + SNOOZE_REMINDER_OFFSET_MS
        val updatedNote = note.copy(
            reminderTime = snoozedTime,
            updatedAt = now,
        )
        noteDao.update(updatedNote)
        syncNote(updatedNote)
        logNoteEvent(updatedNote, TimelineActionType.REMINDER, now, snoozedTime)
    }

    private suspend fun snoozeTodoReminder(itemId: Long) {
        val todo = todoDao.getById(itemId).first() ?: run {
            reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.TODO, itemId)
            return
        }
        reminderNotificationHelper.cancelReminderNotifications(ReminderItemType.TODO, itemId)
        if (todo.status != TodoStatus.ACTIVE) return

        val now = System.currentTimeMillis()
        val snoozedTime = now + SNOOZE_REMINDER_OFFSET_MS
        val updatedTodo = todo.copy(
            reminderTime = snoozedTime,
            updatedAt = now,
        )
        todoDao.update(updatedTodo)
        syncTodo(updatedTodo)
        logTodoEvent(updatedTodo, TimelineActionType.REMINDER, now, snoozedTime)
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

    private suspend fun resyncUser(userId: Long) {
        if (userId < 0L) return
        val notes = noteDao.getAllByUser(userId).first()
        val todos = todoDao.getAllByUser(userId).first()

        notes.forEach { syncNote(it) }
        todos.forEach { syncTodo(it) }

        cleanupOrphanedCalendarMappings(notes, todos)
    }

    private suspend fun clearScheduledStateForUser(userId: Long) {
        if (userId < 0L) return
        val notes = noteDao.getAllByUser(userId).first()
        val todos = todoDao.getAllByUser(userId).first()

        notes.forEach { note ->
            cancelReminderAlarms(ReminderItemType.NOTE, note.id)
            removeCalendarEntry(ReminderItemType.NOTE, note.id)
        }
        todos.forEach { todo ->
            cancelReminderAlarms(ReminderItemType.TODO, todo.id)
            removeCalendarEntry(ReminderItemType.TODO, todo.id)
        }
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
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val showIntent = createReminderEntryPendingIntent(itemType, itemId)
                    if (showIntent != null) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                            pendingIntent,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                else -> {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
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

    private fun createReminderEntryPendingIntent(
        itemType: ReminderItemType,
        itemId: Long,
    ): PendingIntent? {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
            ?.let { ReminderDeepLinkContract.attach(it, itemType, itemId) }
            ?: return null

        return PendingIntent.getActivity(
            appContext,
            buildLaunchRequestCode(itemType, itemId),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun parseActionPayload(intent: Intent): ActionPayload? {
        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)?.let {
            runCatching { ReminderItemType.valueOf(it) }.getOrNull()
        } ?: return null
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it >= 0L } ?: return null
        return ActionPayload(
            itemType = itemType,
            itemId = itemId,
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

    private fun buildLaunchRequestCode(
        itemType: ReminderItemType,
        itemId: Long,
    ): Int {
        val raw = "alarm_entry_${itemType.name}_$itemId".hashCode()
        return if (raw == Int.MIN_VALUE) 0 else kotlin.math.abs(raw)
    }

    private suspend fun logNoteEvent(
        note: Note,
        actionType: TimelineActionType,
        occurredAt: Long = System.currentTimeMillis(),
        referenceTime: Long? = note.reminderTime,
    ) {
        timelineEventRepository.insert(
            TimelineEvent(
                itemId = note.id,
                itemType = TimelineItemType.NOTE,
                actionType = actionType,
                categoryId = note.categoryId,
                categoryName = resolveCategoryName(note.userId, note.categoryId, "\u672a\u5206\u7c7b"),
                title = note.title.trim(),
                contentPreview = buildNoteContentPreview(note),
                referenceTime = referenceTime,
                occurredAt = occurredAt,
                userId = note.userId,
            ),
        )
    }

    private suspend fun logTodoEvent(
        todo: Todo,
        actionType: TimelineActionType,
        occurredAt: Long = System.currentTimeMillis(),
        referenceTime: Long? = todo.reminderTime,
    ) {
        timelineEventRepository.insert(
            TimelineEvent(
                itemId = todo.id,
                itemType = TimelineItemType.TODO,
                actionType = actionType,
                categoryId = todo.categoryId,
                categoryName = resolveCategoryName(todo.userId, todo.categoryId, "\u672a\u5206\u7c7b"),
                title = todo.title.trim(),
                contentPreview = buildTodoContentPreview(todo),
                referenceTime = referenceTime,
                occurredAt = occurredAt,
                userId = todo.userId,
            ),
        )
    }

    private suspend fun resolveCategoryName(userId: Long, categoryId: Long?, fallback: String): String {
        if (categoryId == null) return fallback
        return categoryDao.getAllByUser(userId).first().firstOrNull { it.id == categoryId }?.name ?: fallback
    }

    private fun buildNoteContentPreview(note: Note): String {
        val trimmedContent = note.content.trim()
        return when {
            trimmedContent.isNotBlank() -> trimmedContent
            note.imageUris.isNotBlank() -> "\u5305\u542b\u56fe\u7247"
            note.title.isNotBlank() -> note.title.trim()
            else -> ""
        }
    }

    private fun buildTodoContentPreview(todo: Todo): String {
        return todo.description?.trim()?.takeIf { it.isNotBlank() } ?: todo.title.trim()
    }

    private fun buildNoteDisplayTitle(note: Note): String {
        return note.title.trim().ifBlank {
            note.content.trim().takeIf { it.isNotBlank() }
                ?: if (note.imageUris.isNotBlank()) "\u56fe\u7247\u7b14\u8bb0" else "\u7b14\u8bb0"
        }
    }

    private fun buildNoteDetailText(note: Note): String {
        return note.content.trim().takeIf { it.isNotBlank() }
            ?: if (note.imageUris.isNotBlank()) "\u5305\u542b\u56fe\u7247\u5185\u5bb9" else "\u8fd9\u662f\u4e00\u6761\u7b14\u8bb0\u63d0\u9192"
    }

    private fun buildTodoDisplayTitle(todo: Todo): String {
        return todo.title.trim().ifBlank {
            todo.description?.trim()?.takeIf { it.isNotBlank() } ?: "\u5f85\u529e"
        }
    }

    private fun buildTodoDetailText(todo: Todo): String {
        return todo.description?.trim()?.takeIf { it.isNotBlank() }
            ?: "\u8fd9\u662f\u4e00\u6761\u5f85\u529e\u63d0\u9192"
    }

    private fun buildCalendarTitle(itemType: ReminderItemType, title: String): String {
        val prefix = if (itemType == ReminderItemType.NOTE) {
            "[\u4fbf\u73a9\u5f55][\u7b14\u8bb0]"
        } else {
            "[\u4fbf\u73a9\u5f55][\u5f85\u529e]"
        }
        return "$prefix $title"
    }

    private fun buildCalendarDescription(detailText: String, reminderTime: Long): String {
        return buildString {
            append(detailText)
            append("\n\u63d0\u9192\u65f6\u95f4\uff1a")
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

    private data class ActionPayload(
        val itemType: ReminderItemType,
        val itemId: Long,
    )

    companion object {
        private const val ACTION_REMINDER_ALARM = "com.swu.bianwanlu2_0.action.REMINDER_ALARM"
        private const val ACTION_REMINDER_COMPLETE = "com.swu.bianwanlu2_0.action.REMINDER_COMPLETE"
        private const val ACTION_REMINDER_SNOOZE = "com.swu.bianwanlu2_0.action.REMINDER_SNOOZE"
        private const val EXTRA_ITEM_TYPE = "extra_item_type"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_SCHEDULED_REMINDER_TIME = "extra_scheduled_reminder_time"
        private const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
        private const val EARLY_REMINDER_OFFSET_MS = 15 * 60 * 1000L
        private const val SNOOZE_REMINDER_OFFSET_MS = 10 * 60 * 1000L
    }
}
