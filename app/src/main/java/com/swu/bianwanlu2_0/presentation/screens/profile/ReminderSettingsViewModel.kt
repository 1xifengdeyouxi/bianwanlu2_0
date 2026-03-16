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
        _message.value = if (enabled) {
            "震动提醒已开启，你可以点一下“震动预览”试试效果"
        } else {
            "震动提醒已关闭，后续通知将只保留铃声和横幅提醒"
        }
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

    fun onExactAlarmPermissionGranted() {
        viewModelScope.launch {
            reminderCoordinator.resyncAll()
            _message.value = "精确提醒已开启，后台提醒已重新同步"
        }
    }

    fun scheduleDiagnosticReminder() {
        val scheduled = reminderCoordinator.scheduleDiagnosticReminder()
        _message.value = if (scheduled) {
            "已安排 5 秒后发送测试提醒，请立即退到后台或锁屏后验证"
        } else {
            "测试提醒安排失败，请先检查精确提醒、通知权限和系统限制"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
