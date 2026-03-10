package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.presentation.components.ReminderDialog
import com.swu.bianwanlu2_0.ui.theme.NoteRed

private val cardColorOptions = listOf(
    0xFFFFF8E1L, // 淡黄 (默认)
    0xFFFCE4ECL, // 淡粉
    0xFFE8F5E9L, // 淡绿
    0xFFE3F2FDL, // 淡蓝
    0xFFF3E5F5L, // 淡紫
    0xFFFFF3E0L, // 淡橙
    0xFFECEFF1L, // 淡灰
    0xFFFFFFFF   // 白色
)

@Composable
fun AddTodoScreen(
    onCancel: () -> Unit,
    onConfirm: (title: String, reminderTime: Long?, isPriority: Boolean, cardColor: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var isPriority by remember { mutableStateOf(false) }
    var cardColor by remember { mutableLongStateOf(Todo.DEFAULT_CARD_COLOR) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
    ) {
        // TopBar
        AddTodoTopBar(
            onCancel = onCancel,
            onConfirm = {
                if (title.isNotBlank()) {
                    onConfirm(title, reminderTime, isPriority, cardColor)
                }
            },
            confirmEnabled = title.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 输入卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(cardColor)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF1976D2)),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = "请输入要做的事情",
                                    fontSize = 16.sp,
                                    color = Color(0xFFBDBDBD)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 底部三个按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 日历/提醒按钮
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "设置提醒",
                        tint = if (reminderTime != null) Color(0xFF1976D2) else Color(0xFF757575),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showReminderDialog = true }
                    )

                    // 优先级旗帜按钮
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "设置优先级",
                        tint = if (isPriority) NoteRed else Color(0xFF757575),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isPriority = !isPriority }
                    )

                    // 颜色选择按钮
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC107))
                            .border(1.5.dp, Color(0xFFE0E0E0), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showColorPicker = !showColorPicker }
                    )
                }
            }
        }

        // 颜色选择器
        if (showColorPicker) {
            Spacer(modifier = Modifier.height(12.dp))
            ColorPickerRow(
                selectedColor = cardColor,
                onColorSelected = {
                    cardColor = it
                    showColorPicker = false
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    if (showReminderDialog) {
        ReminderDialog(
            onDismiss = { showReminderDialog = false },
            onSelect = { time ->
                if (time > 0) reminderTime = time
                showReminderDialog = false
            }
        )
    }
}

@Composable
private fun AddTodoTopBar(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "取消",
            fontSize = 16.sp,
            color = Color(0xFF1976D2),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            )
        )

        Text(
            text = "新建",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Text(
            text = "完成",
            fontSize = 16.sp,
            color = if (confirmEnabled) Color(0xFF1976D2) else Color(0xFFBDBDBD),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = confirmEnabled,
                onClick = onConfirm
            )
        )
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        cardColorOptions.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (selectedColor == color) {
                            Modifier.border(2.5.dp, NoteRed, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        }
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}
