package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.ui.theme.AppFontSizeOption
import com.swu.bianwanlu2_0.ui.theme.AppSkinOption
import com.swu.bianwanlu2_0.ui.theme.AppThemeMode
import com.swu.bianwanlu2_0.ui.theme.AppUiSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GeneralSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())

    val settings: StateFlow<AppUiSettings> = _settings.asStateFlow()

    fun setFontSizeOption(option: AppFontSizeOption) {
        updateSettings {
            putInt(KEY_FONT_SIZE_LEVEL, option.level)
        }
    }

    fun setListContentMaxLines(maxLines: Int) {
        if (maxLines !in VALID_MAX_LINES) return
        updateSettings {
            putInt(KEY_LIST_CONTENT_MAX_LINES, maxLines)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        updateSettings {
            putString(KEY_THEME_MODE, themeMode.value)
        }
    }

    fun setSkinOption(skinOption: AppSkinOption) {
        updateSettings {
            putString(KEY_SKIN_OPTION, skinOption.value)
        }
    }

    private fun updateSettings(block: android.content.SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
        _settings.value = readSettings()
    }

    private fun readSettings(): AppUiSettings {
        val savedLines = preferences.getInt(KEY_LIST_CONTENT_MAX_LINES, DEFAULT_MAX_LINES)
        return AppUiSettings(
            fontSizeOption = AppFontSizeOption.fromLevel(
                preferences.getInt(KEY_FONT_SIZE_LEVEL, AppFontSizeOption.Standard.level),
            ),
            listContentMaxLines = savedLines.takeIf { it in VALID_MAX_LINES } ?: DEFAULT_MAX_LINES,
            themeMode = AppThemeMode.fromValue(
                preferences.getString(KEY_THEME_MODE, AppThemeMode.FollowSystem.value),
            ),
            skinOption = AppSkinOption.fromValue(
                preferences.getString(KEY_SKIN_OPTION, AppSkinOption.default.value),
            ),
        )
    }

    private companion object {
        const val PREFS_NAME = "general_settings"
        const val KEY_FONT_SIZE_LEVEL = "font_size_level"
        const val KEY_LIST_CONTENT_MAX_LINES = "list_content_max_lines"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SKIN_OPTION = "skin_option"
        const val DEFAULT_MAX_LINES = 3
        val VALID_MAX_LINES = setOf(2, 3, 5, 7)
    }
}
