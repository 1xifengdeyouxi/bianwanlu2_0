package com.swu.bianwanlu2_0.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

@Singleton
class SystemCalendarSyncHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasCalendarPermissions(): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWritableCalendar(): Boolean = resolveWritableCalendarId() != null

    suspend fun upsertReminderEvent(
        title: String,
        description: String,
        reminderTime: Long,
        existingEventId: Long?,
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext null
        val calendarId = resolveWritableCalendarId() ?: return@withContext null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, reminderTime)
            put(CalendarContract.Events.DTEND, reminderTime + DEFAULT_EVENT_DURATION_MS)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 0)
        }

        if (existingEventId != null) {
            val updatedRows = runCatching {
                context.contentResolver.update(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId),
                    values,
                    null,
                    null,
                )
            }.getOrDefault(0)
            if (updatedRows > 0) {
                return@withContext existingEventId
            }
        }

        val insertedUri = runCatching {
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        }.getOrNull() ?: return@withContext null

        ContentUris.parseId(insertedUri)
    }

    suspend fun deleteEvent(eventId: Long?) = withContext(Dispatchers.IO) {
        if (eventId == null || !hasCalendarPermissions()) return@withContext
        runCatching {
            context.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null,
                null,
            )
        }
    }

    @SuppressLint("Range")
    private fun resolveWritableCalendarId(): Long? {
        if (!hasCalendarPermissions()) return null
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val selection = buildString {
            append(CalendarContract.Calendars.VISIBLE)
            append(" = 1 AND ")
            append(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            append(" >= ?")
        }
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val sortOrder = buildString {
            append(CalendarContract.Calendars.IS_PRIMARY)
            append(" DESC, ")
            append(CalendarContract.Calendars._ID)
            append(" ASC")
        }

        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private companion object {
        const val DEFAULT_EVENT_DURATION_MS = 30 * 60 * 1000L
    }
}
