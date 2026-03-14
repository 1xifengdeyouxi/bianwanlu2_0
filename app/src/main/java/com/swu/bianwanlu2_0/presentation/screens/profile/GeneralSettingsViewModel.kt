package com.swu.bianwanlu2_0.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.swu.bianwanlu2_0.data.local.GeneralSettingsStore
import com.swu.bianwanlu2_0.ui.theme.AppFontSizeOption
import com.swu.bianwanlu2_0.ui.theme.AppSkinOption
import com.swu.bianwanlu2_0.ui.theme.AppThemeMode
import com.swu.bianwanlu2_0.ui.theme.AppUiSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val generalSettingsStore: GeneralSettingsStore,
) : ViewModel() {
    val settings: StateFlow<AppUiSettings> = generalSettingsStore.settings

    fun setFontSizeOption(option: AppFontSizeOption) {
        generalSettingsStore.setFontSizeOption(option)
    }

    fun setListContentMaxLines(maxLines: Int) {
        generalSettingsStore.setListContentMaxLines(maxLines)
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        generalSettingsStore.setThemeMode(themeMode)
    }

    fun setSkinOption(skinOption: AppSkinOption) {
        generalSettingsStore.setSkinOption(skinOption)
    }
}
