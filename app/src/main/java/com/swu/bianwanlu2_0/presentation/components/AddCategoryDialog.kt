package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.ui.theme.NoteRed

/**
 * 新增分类弹窗
 * @param defaultType 默认选中的分类类型（从笔记页进入默认 NOTE，从待办页进入默认 TODO）
 */
@Composable
fun AddCategoryDialog(
    defaultType: CategoryType = CategoryType.NOTE,
    initialName: String = "",
    titleText: String = "新增分类",
    confirmText: String = "新增",
    typeEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: CategoryType) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedType by remember(defaultType) { mutableStateOf(defaultType) }
    var isLocked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column {
                // 密码锁定
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("密码锁定", fontSize = 15.sp, color = Color(0xFF212121))
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NoteRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 分类类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("分类类型", fontSize = 15.sp, color = Color(0xFF212121))
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == CategoryType.NOTE,
                            onClick = { if (typeEnabled) selectedType = CategoryType.NOTE },
                            enabled = typeEnabled,
                            colors = RadioButtonDefaults.colors(selectedColor = NoteRed)
                        )
                        Text("便签", fontSize = 14.sp, color = Color(0xFF212121))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedType == CategoryType.TODO,
                            onClick = { if (typeEnabled) selectedType = CategoryType.TODO },
                            enabled = typeEnabled,
                            colors = RadioButtonDefaults.colors(selectedColor = NoteRed)
                        )
                        Text("待办", fontSize = 14.sp, color = Color(0xFF212121))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 名称输入框
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("请输入分类名称", color = Color(0xFFBDBDBD)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF9E9E9E)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedType) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmText, color = if (name.isNotBlank()) NoteRed else Color(0xFFBDBDBD), fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", fontSize = 16.sp, color = Color(0xFF212121))
            }
        }
    )
}
