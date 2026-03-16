package com.swu.bianwanlu2_0

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.reminder.ReminderDeepLink
import com.swu.bianwanlu2_0.data.reminder.ReminderDeepLinkContract
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import com.swu.bianwanlu2_0.presentation.navigation.AppNavHost
import com.swu.bianwanlu2_0.presentation.screens.profile.GeneralSettingsViewModel
import com.swu.bianwanlu2_0.ui.theme.AppThemeMode
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.ProvideAppUiSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var reminderCoordinator: ReminderCoordinator

    private val pendingReminderDeepLink = MutableStateFlow<ReminderDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReminderDeepLink.value = ReminderDeepLinkContract.parse(intent)
        enableEdgeToEdge()
        setContent {
            val generalSettingsViewModel: GeneralSettingsViewModel = hiltViewModel()
            val settings = generalSettingsViewModel.settings.collectAsStateWithLifecycle().value
            val reminderDeepLink = pendingReminderDeepLink.asStateFlow().collectAsStateWithLifecycle().value
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                AppThemeMode.FollowSystem -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            ProvideAppUiSettings(settings = settings) {
                Bianwanlu2_0Theme(
                    darkTheme = darkTheme,
                    accentColor = settings.skinOption.color,
                    dynamicColor = false,
                ) {
                    AppNavHost(
                        modifier = Modifier.fillMaxSize(),
                        pendingReminderDeepLink = reminderDeepLink,
                        onReminderDeepLinkConsumed = ::consumeReminderDeepLink,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingReminderDeepLink.value = ReminderDeepLinkContract.parse(intent)
    }

    override fun onStart() {
        super.onStart()
        reminderCoordinator.resyncAllAsync()
    }

    private fun consumeReminderDeepLink() {
        pendingReminderDeepLink.value = null
        ReminderDeepLinkContract.clear(intent)
        setIntent(intent)
    }
}
