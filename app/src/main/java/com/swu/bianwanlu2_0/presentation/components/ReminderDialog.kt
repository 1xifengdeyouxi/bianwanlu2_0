package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar

data class ReminderOption(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val getTime: () -> Long
)

@Composable
fun ReminderDialog(
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val options = remember { buildReminderOptions() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "设置提醒",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(options) { option ->
                        ReminderOptionItem(
                            option = option,
                            onClick = { onSelect(option.getTime()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderOptionItem(
    option: ReminderOption,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFF5F5F5), CircleShape)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = option.iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            fontSize = 12.sp,
            color = Color(0xFF616161),
            textAlign = TextAlign.Center
        )
    }
}

private fun buildReminderOptions(): List<ReminderOption> {
    return listOf(
        ReminderOption("30分钟", Icons.Outlined.Schedule, Color(0xFFE53935)) {
            System.currentTimeMillis() + 30 * 60 * 1000L
        },
        ReminderOption("4小时", Icons.Outlined.Alarm, Color(0xFFE53935)) {
            System.currentTimeMillis() + 4 * 60 * 60 * 1000L
        },
        ReminderOption("明天9点", Icons.Outlined.LightMode, Color(0xFF1976D2)) {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        },
        ReminderOption("本周日", Icons.Outlined.Weekend, Color(0xFF6D4C41)) {
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        },
        ReminderOption("下周一", Icons.Outlined.CalendarMonth, Color(0xFF2E7D32)) {
            Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, 1)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        },
        ReminderOption("自定义", Icons.Outlined.EditCalendar, Color(0xFF1565C0)) {
            -1L // 自定义标记，UI 层处理
        }
    )
}
