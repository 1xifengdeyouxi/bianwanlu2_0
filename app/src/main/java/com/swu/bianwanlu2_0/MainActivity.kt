package com.swu.bianwanlu2_0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.presentation.navigation.AppNavHost
import com.swu.bianwanlu2_0.presentation.screens.profile.GeneralSettingsViewModel
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.AppThemeMode
import com.swu.bianwanlu2_0.ui.theme.ProvideAppUiSettings
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val generalSettingsViewModel: GeneralSettingsViewModel = hiltViewModel()
            val settings = generalSettingsViewModel.settings.collectAsStateWithLifecycle().value
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
                    AppNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
