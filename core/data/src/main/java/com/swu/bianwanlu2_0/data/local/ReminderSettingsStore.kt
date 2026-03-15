package com.swu.bianwanlu2_0.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ReminderSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _vibrationEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_VIBRATION_ENABLED, true),
    )
    private val _calendarSyncEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_CALENDAR_SYNC_ENABLED, false),
    )

    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    val calendarSyncEnabled: StateFlow<Boolean> = _calendarSyncEnabled.asStateFlow()

    fun isVibrationEnabled(): Boolean = _vibrationEnabled.value

    fun isCalendarSyncEnabled(): Boolean = _calendarSyncEnabled.value

    fun setVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CALENDAR_SYNC_ENABLED, enabled).apply()
        _calendarSyncEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "reminder_settings"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_CALENDAR_SYNC_ENABLED = "calendar_sync_enabled"
    }
}
