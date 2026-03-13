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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.presentation.components.cardColorOptions
import com.swu.bianwanlu2_0.presentation.components.ReminderDialog
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint
import com.swu.bianwanlu2_0.ui.theme.NoteRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTodoScreen(
    onCancel: () -> Unit,
    onConfirm: (title: String, reminderTime: Long?, isPriority: Boolean, cardColor: Long) -> Unit,
    existingTodo: Todo? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val iconTint = LocalAppIconTint.current
    var title by remember { mutableStateOf(existingTodo?.title ?: "") }
    var reminderTime by remember { mutableStateOf(existingTodo?.reminderTime) }
    var isPriority by remember { mutableStateOf(existingTodo?.isPriority ?: false) }
    var cardColor by remember { mutableLongStateOf(existingTodo?.cardColor ?: Todo.DEFAULT_CARD_COLOR) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val isEditMode = existingTodo != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        AddTodoTopBar(
            titleText = if (isEditMode) "编辑" else "新建",
            onCancel = onCancel,
            onConfirm = {
                if (title.isNotBlank()) {
                    onConfirm(title, reminderTime, isPriority, cardColor)
                }
            },
            confirmEnabled = title.isNotBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                    cursorBrush = SolidColor(accentColor),
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

                // 已设置的提醒时间提示
                if (reminderTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "提醒: ${formatReminderTime(reminderTime!!)}",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "设置提醒",
                        tint = if (reminderTime != null) accentColor else Color(0xFF757575),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showReminderDialog = true }
                    )

                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "设置优先级",
                        tint = if (isPriority) iconTint else Color(0xFF757575),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isPriority = !isPriority }
                    )

                    // 颜色圆圈跟随当前卡片颜色
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(cardColor))
                            .border(1.5.dp, Color(0xFFBDBDBD), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showColorPicker = !showColorPicker }
                    )
                }
            }
        }

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
                reminderTime = time
                showReminderDialog = false
            },
            showClearAction = reminderTime != null,
            onClear = {
                reminderTime = null
                showReminderDialog = false
            }
        )
    }
}

@Composable
private fun AddTodoTopBar(
    titleText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "取消",
            fontSize = 16.sp,
            color = accentColor,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            )
        )

        Text(
            text = titleText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text = "完成",
            fontSize = 16.sp,
            color = if (confirmEnabled) accentColor else Color(0xFFBDBDBD),
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
    val iconTint = LocalAppIconTint.current

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
                            Modifier.border(2.5.dp, iconTint, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        }
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

private fun formatReminderTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
