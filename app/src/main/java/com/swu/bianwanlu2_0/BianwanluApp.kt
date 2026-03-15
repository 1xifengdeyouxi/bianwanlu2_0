package com.swu.bianwanlu2_0

import android.app.Application
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import com.swu.bianwanlu2_0.data.reminder.ReminderNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BianwanluApp : Application() {
    @Inject
    lateinit var reminderCoordinator: ReminderCoordinator

    @Inject
    lateinit var reminderNotificationHelper: ReminderNotificationHelper

    override fun onCreate() {
        super.onCreate()
        reminderNotificationHelper.ensureChannels()
        reminderCoordinator.resyncAllAsync()
    }
}
