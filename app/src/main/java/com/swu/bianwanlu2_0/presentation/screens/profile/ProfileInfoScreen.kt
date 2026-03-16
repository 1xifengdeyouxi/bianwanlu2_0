package com.swu.bianwanlu2_0.presentation.screens.profile

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.AccountSession
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint

@Composable
fun ProfileInfoScreen(
    state: AccountSession,
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onNicknameConfirm: (String) -> String?,
    onAccountConfirm: (String) -> String?,
    onAvatarChange: (String?) -> Unit,
    onLogout: () -> Unit,
    onCancelAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconTint = LocalAppIconTint.current
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showCancelAccountConfirm by remember { mutableStateOf(false) }

    val guideToAuth: () -> Unit = {
        Toast.makeText(context, "请先登录或注册", Toast.LENGTH_SHORT).show()
        onOpenAuth()
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onAvatarChange(uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "个人信息",
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ProfileInfoRow(
                    title = "头像",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatarImage(
                                avatarUri = state.avatarUri,
                                modifier = Modifier.size(56.dp),
                                iconSize = 28.dp,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = iconTint.copy(alpha = 0.7f),
                            )
                        }
                    },
                    onClick = {
                        if (state.isLoggedIn) {
                            avatarPickerLauncher.launch(arrayOf("image/*"))
                        } else {
                            guideToAuth()
                        }
                    },
                )
                ProfileInfoRow(
                    title = "昵称",
                    value = state.nickname.ifBlank { "未设置" },
                    onClick = {
                        if (state.isLoggedIn) {
                            showNicknameDialog = true
                        } else {
                            guideToAuth()
                        }
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProfileInfoRow(
                    title = "设置账号",
                    value = when {
                        state.isLoggedIn && state.account.isNotBlank() -> state.account
                        state.hasLocalAccount -> "去登录"
                        else -> "登录/注册"
                    },
                    onClick = {
                        if (state.isLoggedIn) {
                            showAccountDialog = true
                        } else {
                            guideToAuth()
                        }
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                ProfileInfoRow(
                    title = "账号注销",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        if (state.hasLocalAccount) {
                            showCancelAccountConfirm = true
                        } else {
                            Toast.makeText(context, "当前暂无可注销账号", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        Button(
            onClick = {
                if (state.isLoggedIn) {
                    showLogoutConfirm = true
                } else {
                    Toast.makeText(context, "当前未登录", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .height(52.dp),
            enabled = state.isLoggedIn,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(text = "退出登录", fontSize = 18.sp)
        }
    }

    if (showNicknameDialog) {
        EditTextDialog(
            title = "修改昵称",
            initialValue = state.nickname,
            placeholder = "请输入昵称",
            onDismiss = { showNicknameDialog = false },
            onConfirm = { value -> onNicknameConfirm(value) },
            onSuccess = { showNicknameDialog = false },
        )
    }

    if (showAccountDialog) {
        EditTextDialog(
            title = "设置账号",
            initialValue = state.account,
            placeholder = "请输入账号",
            onDismiss = { showAccountDialog = false },
            onConfirm = { value -> onAccountConfirm(value) },
            onSuccess = { showAccountDialog = false },
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("确定退出当前账号吗？", color = Color(0xFF616161)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogout()
                        showLogoutConfirm = false
                    },
                ) {
                    Text("确定", color = Color(0xFF5A9CF0))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }

    if (showCancelAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelAccountConfirm = false },
            title = { Text("账号注销") },
            text = {
                Text(
                    "\u786e\u5b9a\u6ce8\u9500\u672c\u5730\u8d26\u53f7\u5417\uff1f\u6ce8\u9500\u540e\u8be5\u8d26\u53f7\u4e0b\u7684\u7b14\u8bb0\u3001\u5f85\u529e\u3001\u5206\u7c7b\u3001\u65f6\u95f4\u8f74\u8bb0\u5f55\u4e0e\u641c\u7d22\u5386\u53f2\u90fd\u4f1a\u88ab\u5220\u9664\uff0c\u4e14\u4e0d\u53ef\u6062\u590d\uff0c\u5e94\u7528\u4f1a\u5207\u6362\u56de\u6e38\u5ba2\u6a21\u5f0f\u3002",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelAccount()
                        Toast.makeText(context, "\u672c\u5730\u8d26\u53f7\u5df2\u6ce8\u9500\uff0c\u5df2\u5207\u6362\u5230\u6e38\u5ba2\u6a21\u5f0f", Toast.LENGTH_SHORT).show()
                        showCancelAccountConfirm = false
                    },
                ) {
                    Text("注销", color = Color(0xFFE65E4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAccountConfirm = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface)
                }
            },
        )
    }
}

@Composable
private fun ProfileInfoRow(
    title: String,
    value: String? = null,
    titleColor: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val iconTint = LocalAppIconTint.current
    val resolvedTitleColor = titleColor ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = resolvedTitleColor,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                trailing()
            } else {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = 0.7f),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 24.dp))
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> String?,
    onSuccess: () -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = onConfirm(value)
                    if (result == null) {
                        onSuccess()
                    } else {
                        errorMessage = result
                    }
                },
            ) {
                Text("保存", color = Color(0xFF5A9CF0))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurface)
            }
        },
    )
}
