package com.swu.bianwanlu2_0.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.ReminderSettingsStore
import com.swu.bianwanlu2_0.data.reminder.CalendarSyncToggleResult
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val reminderSettingsStore: ReminderSettingsStore,
    private val reminderCoordinator: ReminderCoordinator,
) : ViewModel() {
    val vibrationEnabled: StateFlow<Boolean> = reminderSettingsStore.vibrationEnabled
    val calendarSyncEnabled: StateFlow<Boolean> = reminderSettingsStore.calendarSyncEnabled

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setVibrationEnabled(enabled: Boolean) {
        reminderSettingsStore.setVibrationEnabled(enabled)
    }

    fun enableCalendarSync() {
        viewModelScope.launch {
            when (reminderCoordinator.setCalendarSyncEnabled(true)) {
                CalendarSyncToggleResult.ENABLED -> {
                    _message.value = "系统日历同步已开启"
                }
                CalendarSyncToggleResult.PERMISSION_DENIED -> {
                    _message.value = "请先授权日历权限"
                }
                CalendarSyncToggleResult.NO_WRITABLE_CALENDAR -> {
                    _message.value = "未找到可写入的系统日历"
                }
                CalendarSyncToggleResult.DISABLED -> Unit
            }
        }
    }

    fun disableCalendarSync() {
        viewModelScope.launch {
            reminderCoordinator.setCalendarSyncEnabled(false)
            _message.value = "系统日历同步已关闭"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
