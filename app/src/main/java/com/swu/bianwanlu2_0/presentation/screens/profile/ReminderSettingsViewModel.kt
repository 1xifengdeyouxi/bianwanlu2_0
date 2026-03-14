package com.swu.bianwanlu2_0.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.swu.bianwanlu2_0.data.local.ReminderSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val reminderSettingsStore: ReminderSettingsStore,
) : ViewModel() {
    val vibrationEnabled: StateFlow<Boolean> = reminderSettingsStore.vibrationEnabled

    fun setVibrationEnabled(enabled: Boolean) {
        reminderSettingsStore.setVibrationEnabled(enabled)
    }
}
