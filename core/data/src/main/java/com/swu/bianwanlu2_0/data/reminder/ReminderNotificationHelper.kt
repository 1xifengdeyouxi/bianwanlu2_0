package com.swu.bianwanlu2_0.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.swu.bianwanlu2_0.data.reminder.ReminderTriggerType
import android.os.Build
import com.swu.bianwanlu2_0.data.local.ReminderSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderSettingsStore: ReminderSettingsStore,
) {
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val silentChannel = NotificationChannel(
            CHANNEL_SILENT,
            "便玩录提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "到时间后发送笔记和待办提醒"
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val vibrationChannel = NotificationChannel(
            CHANNEL_VIBRATION,
            "便玩录提醒（震动）",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "到时间后发送笔记和待办提醒，并带震动"
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(silentChannel)
        manager.createNotificationChannel(vibrationChannel)
    }

    fun showReminderNotification(
        itemType: ReminderItemType,
        itemId: Long,
        displayTitle: String,
        detailText: String,
        reminderTime: Long,
        isEarlyReminder: Boolean,
    ) {
        if (!canPostNotifications()) return

        ensureChannels()
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = createBuilder(isEarlyReminder)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(buildNotificationTitle(itemType, isEarlyReminder))
            .setContentText(buildNotificationText(displayTitle, isEarlyReminder))
            .setStyle(
                Notification.BigTextStyle().bigText(
                    buildFullNotificationText(
                        displayTitle = displayTitle,
                        detailText = detailText,
                        reminderTime = reminderTime,
                        isEarlyReminder = isEarlyReminder,
                    ),
                ),
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setSubText("\u4fbf\u73a9\u5f55")
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setCategory(Notification.CATEGORY_ALARM)
                    setVisibility(Notification.VISIBILITY_PUBLIC)
                }
                createContentIntent(itemType, itemId)?.let(::setContentIntent)
                createSnoozeActionIntent(itemType, itemId)?.let { addAction(android.R.drawable.ic_popup_reminder, "\u7a0d\u540e10\u5206\u949f", it) }
                createCompleteActionIntent(itemType, itemId)?.let { addAction(android.R.drawable.checkbox_on_background, "\u5b8c\u6210", it) }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && reminderSettingsStore.isVibrationEnabled()) {
                    setVibrate(VIBRATION_PATTERN)
                }
            }
            .build()

        manager.notify(buildNotificationId(itemType, itemId, isEarlyReminder), notification)
    }

    private fun createBuilder(isEarlyReminder: Boolean): Notification.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(
                context,
                if (reminderSettingsStore.isVibrationEnabled()) CHANNEL_VIBRATION else CHANNEL_SILENT,
            )
        } else {
            Notification.Builder(context).apply {
                setPriority(
                    if (isEarlyReminder) {
                        Notification.PRIORITY_MAX
                    } else {
                        Notification.PRIORITY_HIGH
                    },
                )
            }
        }
    }

    private fun buildNotificationTitle(
        itemType: ReminderItemType,
        isEarlyReminder: Boolean,
    ): String {
        return when {
            isEarlyReminder -> "优先级提前提醒"
            itemType == ReminderItemType.NOTE -> "笔记提醒"
            else -> "待办提醒"
        }
    }

    private fun buildNotificationText(
        displayTitle: String,
        isEarlyReminder: Boolean,
    ): String {
        return if (isEarlyReminder) {
            "$displayTitle 将在15分钟后提醒"
        } else {
            displayTitle
        }
    }

    private fun buildFullNotificationText(
        displayTitle: String,
        detailText: String,
        reminderTime: Long,
        isEarlyReminder: Boolean,
    ): String {
        val pieces = mutableListOf<String>()
        pieces += if (isEarlyReminder) {
            "$displayTitle 将在15分钟后提醒"
        } else {
            displayTitle
        }
        detailText.takeIf { it.isNotBlank() }?.let(pieces::add)
        pieces += "提醒时间：${formatReminderTime(reminderTime)}"
        return pieces.joinToString(separator = "\n")
    }

    fun cancelReminderNotifications(itemType: ReminderItemType, itemId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ReminderTriggerType.entries.forEach { triggerType ->
            manager.cancel(buildNotificationId(itemType, itemId, triggerType == ReminderTriggerType.EARLY))
        }
    }

    private fun createCompleteActionIntent(itemType: ReminderItemType, itemId: Long): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_COMPLETE
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            context,
            buildActionRequestCode(itemType, itemId, ACTION_REMINDER_COMPLETE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createSnoozeActionIntent(itemType: ReminderItemType, itemId: Long): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_SNOOZE
            putExtra(EXTRA_ITEM_TYPE, itemType.name)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            context,
            buildActionRequestCode(itemType, itemId, ACTION_REMINDER_SNOOZE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createContentIntent(itemType: ReminderItemType, itemId: Long): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            ?.let { ReminderDeepLinkContract.attach(it, itemType, itemId) }
            ?: return null
        return PendingIntent.getActivity(
            context,
            buildContentRequestCode(itemType, itemId),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    private fun buildNotificationId(
        itemType: ReminderItemType,
        itemId: Long,
        isEarlyReminder: Boolean,
    ): Int {
        val raw = "notification_${itemType.name}_${itemId}_${if (isEarlyReminder) "early" else "due"}".hashCode()
        return if (raw == Int.MIN_VALUE) 0 else kotlin.math.abs(raw)
    }

    private fun buildActionRequestCode(
        itemType: ReminderItemType,
        itemId: Long,
        action: String,
    ): Int {
        val raw = "notification_action_${itemType.name}_${itemId}_$action".hashCode()
        return if (raw == Int.MIN_VALUE) 0 else kotlin.math.abs(raw)
    }

    private fun buildContentRequestCode(
        itemType: ReminderItemType,
        itemId: Long,
    ): Int {
        val raw = "notification_content_${itemType.name}_$itemId".hashCode()
        return if (raw == Int.MIN_VALUE) 0 else kotlin.math.abs(raw)
    }

    private fun formatReminderTime(reminderTime: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(reminderTime))
    }

    private companion object {
        const val CHANNEL_SILENT = "bianwanlu_reminder_silent"
        const val CHANNEL_VIBRATION = "bianwanlu_reminder_vibration"
        const val ACTION_REMINDER_COMPLETE = "com.swu.bianwanlu2_0.action.REMINDER_COMPLETE"
        const val ACTION_REMINDER_SNOOZE = "com.swu.bianwanlu2_0.action.REMINDER_SNOOZE"
        const val EXTRA_ITEM_TYPE = "extra_item_type"
        const val EXTRA_ITEM_ID = "extra_item_id"
        val VIBRATION_PATTERN = longArrayOf(0L, 180L, 120L, 220L)
    }
}
