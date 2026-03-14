package com.swu.bianwanlu2_0.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class AppFontSizeOption(
    val level: Int,
    val label: String,
    val scaleFactor: Float,
) {
    Small(level = 0, label = "较小", scaleFactor = 0.92f),
    Standard(level = 1, label = "标准", scaleFactor = 1.0f),
    Large(level = 2, label = "大", scaleFactor = 1.08f),
    Larger(level = 3, label = "较大", scaleFactor = 1.16f),
    Largest(level = 4, label = "特大", scaleFactor = 1.24f),
    ;

    companion object {
        fun fromLevel(level: Int): AppFontSizeOption {
            return entries.firstOrNull { it.level == level } ?: Standard
        }
    }
}

enum class AppThemeMode(
    val value: String,
    val label: String,
) {
    FollowSystem(value = "system", label = "默认"),
    Light(value = "light", label = "白天模式"),
    Dark(value = "dark", label = "夜间模式"),
    ;

    companion object {
        fun fromValue(value: String?): AppThemeMode {
            return entries.firstOrNull { it.value == value } ?: FollowSystem
        }
    }
}

enum class AppSkinOption(
    val value: String,
    val colorValue: Long,
) {
    Graphite(value = "graphite", colorValue = 0xFF454545),
    SlateBlue(value = "slate_blue", colorValue = 0xFF8C9EB2),
    Orange(value = "orange", colorValue = 0xFFF6A043),
    Wheat(value = "wheat", colorValue = 0xFFEFBE63),
    Coral(value = "coral", colorValue = 0xFFFA6A6A),
    Lime(value = "lime", colorValue = 0xFF9AC33F),
    Mint(value = "mint", colorValue = 0xFF2DC9A2),
    Pink(value = "pink", colorValue = 0xFFDE6FAF),
    Cyan(value = "cyan", colorValue = 0xFF25B2D0),
    Sky(value = "sky", colorValue = 0xFF5A9CF0),
    RoyalBlue(value = "royal_blue", colorValue = 0xFF4269C8),
    BrickRed(value = "brick_red", colorValue = 0xFFD14F4F),
    Rose(value = "rose", colorValue = 0xFFC25782),
    Lavender(value = "lavender", colorValue = 0xFF7E80DA),
    ;

    val color: Color
        get() = Color(colorValue)

    companion object {
        val default = Sky

        fun fromValue(value: String?): AppSkinOption {
            return entries.firstOrNull { it.value == value } ?: default
        }
    }
}

@Immutable
data class AppUiSettings(
    val fontSizeOption: AppFontSizeOption = AppFontSizeOption.Standard,
    val listContentMaxLines: Int = 3,
    val themeMode: AppThemeMode = AppThemeMode.FollowSystem,
    val skinOption: AppSkinOption = AppSkinOption.default,
)

val LocalAppUiSettings = staticCompositionLocalOf { AppUiSettings() }
val LocalAppListContentMaxLines = staticCompositionLocalOf { 3 }
val LocalAppIconTint = staticCompositionLocalOf { AppSkinOption.default.color }

@Composable
fun ProvideAppUiSettings(
    settings: AppUiSettings,
    content: @Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    val adjustedDensity = remember(baseDensity, settings.fontSizeOption) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * settings.fontSizeOption.scaleFactor,
        )
    }

    CompositionLocalProvider(
        LocalDensity provides adjustedDensity,
        LocalAppUiSettings provides settings,
        LocalAppListContentMaxLines provides settings.listContentMaxLines,
        LocalAppIconTint provides settings.skinOption.color,
        content = content,
    )
}
