package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.presentation.components.ReminderDialog
import com.swu.bianwanlu2_0.presentation.screens.todo.cardColorOptions
import com.swu.bianwanlu2_0.ui.theme.NoteRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteTextColorOptions = listOf(
    0xFF212121,
    0xFFE53935,
    0xFF1565C0,
    0xFF2E7D32,
    0xFF6A1B9A,
    0xFFEF6C00,
    0xFF455A64,
    0xFF795548
)

@Composable
fun AddNoteScreen(
    onCancel: () -> Unit,
    onConfirm: (
        title: String,
        content: String,
        reminderTime: Long?,
        isPriority: Boolean,
        cardColor: Long,
        textColor: Long,
        imageUris: String
    ) -> Unit,
    existingNote: Note? = null,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    var reminderTime by remember { mutableStateOf(existingNote?.reminderTime) }
    var isPriority by remember { mutableStateOf(existingNote?.isPriority ?: false) }
    var cardColor by remember { mutableLongStateOf(existingNote?.cardColor ?: Note.DEFAULT_CARD_COLOR) }
    var textColor by remember { mutableLongStateOf(existingNote?.textColor ?: Note.DEFAULT_TEXT_COLOR) }
    val imageUris = remember {
        mutableStateListOf<String>().apply {
            addAll(existingNote?.imageUris?.lines()?.filter { it.isNotBlank() } ?: emptyList())
        }
    }

    var showReminderDialog by remember { mutableStateOf(false) }
    var showCardColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }

    val isEditMode = existingNote != null
    val confirmEnabled = title.isNotBlank() || content.isNotBlank() || imageUris.isNotEmpty()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUris.add(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            AddNoteTopBar(
                titleText = if (isEditMode) "编辑" else "新建",
                onCancel = onCancel,
                onConfirm = {
                    if (confirmEnabled) {
                        onConfirm(
                            title,
                            content,
                            reminderTime,
                            isPriority,
                            cardColor,
                            textColor,
                            imageUris.joinToString("\n")
                        )
                    }
                },
                confirmEnabled = confirmEnabled
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
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121)
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF1976D2)),
                        decorationBox = { innerTextField ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        text = "标题",
                                        fontSize = 20.sp,
                                        color = Color(0xFFBDBDBD)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFEEEEEE))
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color(textColor),
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF1976D2)),
                        decorationBox = { innerTextField ->
                            Box {
                                if (content.isEmpty()) {
                                    Text(
                                        text = "内容",
                                        fontSize = 16.sp,
                                        color = Color(0xFFBDBDBD)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (reminderTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "提醒: ${formatReminderTime(reminderTime!!)}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    if (imageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "已添加 ${imageUris.size} 张图片",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "设置提醒",
                            tint = if (reminderTime != null) Color(0xFF1976D2) else Color(0xFF757575),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showReminderDialog = true }
                                )
                        )

                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "设置优先级",
                            tint = if (isPriority) NoteRed else Color(0xFF757575),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isPriority = !isPriority }
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(cardColor))
                                .border(1.5.dp, Color(0xFFBDBDBD), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showCardColorPicker = !showCardColorPicker }
                                )
                        )
                    }
                }
            }

            if (showCardColorPicker) {
                Spacer(modifier = Modifier.height(12.dp))
                NoteCardColorPickerRow(
                    selectedColor = cardColor,
                    onColorSelected = {
                        cardColor = it
                        showCardColorPicker = false
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            if (showTextColorPicker) {
                NoteTextColorPickerRow(
                    selectedColor = textColor,
                    onColorSelected = {
                        textColor = it
                        showTextColorPicker = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "添加图片",
                    tint = Color(0xFF616161),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                )

                Icon(
                    imageVector = Icons.Outlined.FormatColorText,
                    contentDescription = "修改文字颜色",
                    tint = Color(textColor),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showTextColorPicker = !showTextColorPicker }
                        )
                )
            }
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
private fun AddNoteTopBar(
    titleText: String,
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
private fun NoteCardColorPickerRow(
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
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (selectedColor == color) {
                            Modifier.border(2.dp, NoteRed, CircleShape)
                        } else {
                            Modifier.border(1.dp, Color(0xFFE0E0E0), CircleShape)
                        }
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun NoteTextColorPickerRow(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        noteTextColorOptions.forEach { color ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (selectedColor == color) {
                            Modifier.border(2.dp, NoteRed, CircleShape)
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
