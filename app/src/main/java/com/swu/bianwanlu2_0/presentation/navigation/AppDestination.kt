package com.swu.bianwanlu2_0.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 应用底部导航目的地定义
 */
sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Notes : AppDestination(
        route = "notes",
        label = "笔记",
        icon = Icons.Outlined.StickyNote2
    )

    data object Todo : AppDestination(
        route = "todo",
        label = "待办",
        icon = Icons.Outlined.CheckBox
    )

    data object Timeline : AppDestination(
        route = "timeline",
        label = "时间",
        icon = Icons.Outlined.Schedule
    )

    data object Calendar : AppDestination(
        route = "calendar",
        label = "日历",
        icon = Icons.Outlined.CalendarMonth
    )
}

val bottomNavDestinations = listOf(
    AppDestination.Notes,
    AppDestination.Todo,
    AppDestination.Timeline,
    AppDestination.Calendar
)
