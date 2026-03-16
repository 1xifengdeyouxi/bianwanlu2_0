package com.swu.bianwanlu2_0.presentation.screens.profile

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swu.bianwanlu2_0.R
import com.swu.bianwanlu2_0.ui.theme.AppFontSizeOption
import com.swu.bianwanlu2_0.ui.theme.AppSkinOption
import com.swu.bianwanlu2_0.ui.theme.AppThemeMode
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ReminderSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenExactAlarmGuide: () -> Unit = {},
    viewModel: ReminderSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val calendarSyncEnabled by viewModel.calendarSyncEnabled.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var notificationEnabled by remember { mutableStateOf(isNotificationPermissionEnabled(context)) }
    var exactAlarmEnabled by remember { mutableStateOf(isExactAlarmPermissionEnabled(context)) }
    var batteryOptimizationIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var lastExactAlarmEnabled by remember { mutableStateOf(exactAlarmEnabled) }

    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        notificationEnabled = isNotificationPermissionEnabled(context)
    }

    val batteryOptimizationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationEnabled = isNotificationPermissionEnabled(context)
        if (granted && notificationEnabled) {
            Toast.makeText(context, "???????", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "?????????????", Toast.LENGTH_SHORT).show()
            notificationSettingsLauncher.launch(createNotificationSettingsIntent(context))
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted && hasCalendarPermissions(context)) {
            viewModel.enableCalendarSync()
        } else {
            Toast.makeText(context, "?????????????", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notificationEnabled = isNotificationPermissionEnabled(context)
                exactAlarmEnabled = isExactAlarmPermissionEnabled(context)
                batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(message) {
        message?.let { toastText ->
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(exactAlarmEnabled) {
        if (!lastExactAlarmEnabled && exactAlarmEnabled) {
            viewModel.onExactAlarmPermissionGranted()
        }
        lastExactAlarmEnabled = exactAlarmEnabled
    }

    ProfileScaffold(
        title = "????",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingActionRow(
            title = "????",
            value = if (notificationEnabled) "???" else "???",
            onClick = {
                if (notificationEnabled) {
                    Toast.makeText(context, "?????????", Toast.LENGTH_SHORT).show()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val runtimePermissionGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (runtimePermissionGranted) {
                        notificationSettingsLauncher.launch(createNotificationSettingsIntent(context))
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    notificationSettingsLauncher.launch(createNotificationSettingsIntent(context))
                }
            },
        )
        SettingActionRow(
            title = "????",
            value = if (exactAlarmEnabled) "???" else "???",
            onClick = onOpenExactAlarmGuide,
        )
        SettingActionRow(
            title = "\u540e\u53f0\u7701\u7535\u4fdd\u62a4",
            value = if (batteryOptimizationIgnored) "\u5df2\u5141\u8bb8" else "\u53bb\u8bbe\u7f6e",
            onClick = {
                if (batteryOptimizationIgnored) {
                    Toast.makeText(context, "\u5f53\u524d\u5df2\u5141\u8bb8\u4fbf\u73a9\u5f55\u540e\u53f0\u8fd0\u884c", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "\u8bf7\u5728\u7cfb\u7edf\u7701\u7535\u7ba1\u7406\u9875\u9762\u5141\u8bb8\u4fbf\u73a9\u5f55\u540e\u53f0\u8fd0\u884c", Toast.LENGTH_LONG).show()
                    batteryOptimizationSettingsLauncher.launch(createBatteryOptimizationSettingsIntent(context))
                }
            },
        )
        SettingSwitchRow(
            title = "??",
            checked = vibrationEnabled,
            onCheckedChange = viewModel::setVibrationEnabled,
        )
        SettingSwitchRow(
            title = "???????",
            checked = calendarSyncEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (hasCalendarPermissions(context)) {
                        viewModel.enableCalendarSync()
                    } else {
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR,
                            ),
                        )
                    }
                } else {
                    viewModel.disableCalendarSync()
                }
            },
        )
        ReminderSettingsHintCard()
    }
}

@Composable
fun ExactAlarmGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val message by viewModel.message.collectAsStateWithLifecycle()
    var exactAlarmEnabled by remember { mutableStateOf(isExactAlarmPermissionEnabled(context)) }
    var lastExactAlarmEnabled by remember { mutableStateOf(exactAlarmEnabled) }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        exactAlarmEnabled = isExactAlarmPermissionEnabled(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                exactAlarmEnabled = isExactAlarmPermissionEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(message) {
        message?.let { toastText ->
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(exactAlarmEnabled) {
        if (!lastExactAlarmEnabled && exactAlarmEnabled) {
            viewModel.onExactAlarmPermissionGranted()
        }
        lastExactAlarmEnabled = exactAlarmEnabled
    }

    ProfileScaffold(
        title = "????",
        onBack = onBack,
        modifier = modifier,
    ) {
        ExactAlarmGuideStatusCard(enabled = exactAlarmEnabled)
        ExactAlarmGuideActionCard(
            enabled = exactAlarmEnabled,
            onClick = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Toast.makeText(context, "??????????????", Toast.LENGTH_SHORT).show()
                } else {
                    exactAlarmSettingsLauncher.launch(createExactAlarmSettingsIntent(context))
                }
            },
        )
    }
}

@Composable
private fun ExactAlarmGuideStatusCard(enabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "???????",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        ReminderSettingsHintLine(text = "??????????????????????")
        ReminderSettingsHintLine(text = "??????????????15?????????????")
        ReminderSettingsHintLine(text = "??????????????????????????????")
        Spacer(modifier = Modifier.height(14.dp))
        ExactAlarmStatusBadge(
            text = if (enabled) "????????" else "????????",
            background = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            contentColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ExactAlarmGuideActionCard(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "????",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (enabled) "?????????????????????????" else "?????????????????????????",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledGuideButton(
            text = if (enabled) "????" else "????",
            onClick = onClick,
        )
    }
}

@Composable
private fun ExactAlarmStatusBadge(
    text: String,
    background: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = contentColor,
        )
    }
}

@Composable
private fun FilledGuideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReminderSettingsHintCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "????",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        ReminderSettingsHintLine(text = "????????????????15?????????")
        ReminderSettingsHintLine(text = "????????????????????????")
        ReminderSettingsHintLine(text = "????????????????????????????")
        ReminderSettingsHintLine(text = "??????????????????????????????????")
        ReminderSettingsHintLine(text = "\u5982\u679c\u606f\u5c4f\u6216\u9000\u5230\u540e\u53f0\u540e\u63d0\u9192\u4ecd\u4e0d\u7a33\u5b9a\uff0c\u8bf7\u540c\u65f6\u5173\u95ed\u7cfb\u7edf\u7684\u7701\u7535\u9650\u5236")
    }
}

@Composable
private fun ReminderSettingsHintLine(text: String) {
    Text(
        text = "? $text",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
fun DataAndSyncScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    launchAction: DataSyncLaunchAction? = null,
    onLaunchActionConsumed: () -> Unit = {},
    viewModel: DataAndSyncViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val inputStream = runCatching {
            context.contentResolver.openInputStream(uri)
        }.getOrNull()
        viewModel.prepareImport(inputStream)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val outputStream = runCatching {
            context.contentResolver.openOutputStream(uri, "w")
        }.getOrNull()
        viewModel.exportBackup(outputStream)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(launchAction) {
        when (launchAction) {
            DataSyncLaunchAction.Import -> {
                onLaunchActionConsumed()
                importLauncher.launch(arrayOf("application/json", "text/plain"))
            }

            DataSyncLaunchAction.Export -> {
                onLaunchActionConsumed()
                exportLauncher.launch(generateBackupFileName())
            }

            null -> Unit
        }
    }

    ProfileScaffold(
        title = "数据与同步",
        onBack = onBack,
        modifier = modifier,
    ) {
        DataSyncNoticeCard()
        SettingActionRow(
            title = "数据导入",
            value = "覆盖恢复",
            onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain"))
            },
        )
        SettingActionRow(
            title = "数据导出",
            value = "JSON 备份",
            onClick = {
                exportLauncher.launch(generateBackupFileName())
            },
        )
        SettingActionRow(
            title = "删除数据",
            value = "清空并重建默认分类",
            onClick = {
                showClearDialog = true
            },
        )
    }

    uiState.importPreview?.let { preview ->
        DataImportPreviewDialog(
            preview = preview,
            onDismiss = viewModel::dismissImportPreview,
            onConfirm = viewModel::confirmImport,
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "确认删除本地数据",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "删除后会清空当前设备上的笔记、待办、时间轴和分类数据，并自动重建默认分类，此操作不可撤销。",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Text(
                    text = "确认删除",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showClearDialog = false },
                    ),
                )
            },
        )
    }

    if (uiState.isLoading) {
        DataSyncLoadingDialog(message = uiState.loadingMessage ?: "处理中…")
    }
}

@Composable
private fun DataSyncNoticeCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(
            text = "备份说明",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))
        DataSyncHint(text = "导出会保存笔记、待办、分类、时间轴和提醒时间。")
        DataSyncHint(text = "导入会覆盖当前本地数据，导入前建议先导出一次。")
        DataSyncHint(text = "含图片的笔记会保存图片引用，跨设备恢复时图片可能需要重新添加。")
    }
}

@Composable
private fun DataSyncHint(text: String) {
    Text(
        text = "• $text",
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun DataImportPreviewDialog(
    preview: DataImportPreviewUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "确认覆盖恢复",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                preview.exportedAt?.let { exportedAt ->
                    Text(
                        text = "备份时间：${formatDataSyncTime(exportedAt)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                DataSyncPreviewLine(label = "分类", value = preview.categoryCount)
                DataSyncPreviewLine(label = "笔记", value = preview.noteCount)
                DataSyncPreviewLine(label = "待办", value = preview.todoCount)
                DataSyncPreviewLine(label = "时间轴", value = preview.timelineEventCount)
                if (preview.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    preview.warnings.forEach { warning ->
                        DataSyncHint(text = warning)
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = "开始导入",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onConfirm,
                ),
            )
        },
        dismissButton = {
            Text(
                text = "取消",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            )
        },
    )
}

@Composable
private fun DataSyncPreviewLine(
    label: String,
    value: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$value 条",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DataSyncLoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = message,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.4.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "请稍候，正在处理你的本地数据",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
    )
}

private fun generateBackupFileName(): String {
    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return "便玩录备份_${formatter.format(Date())}.json"
}

private fun formatDataSyncTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}

enum class DataSyncLaunchAction {
    Import,
    Export,
}

@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneralSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var currentPage by remember { mutableStateOf(GeneralSettingsPage.Root) }
    var draftFontSize by remember { mutableStateOf(settings.fontSizeOption) }
    var draftSkin by remember { mutableStateOf(settings.skinOption) }
    var showMaxLinesDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    when (currentPage) {
        GeneralSettingsPage.Root -> {
            ProfileScaffold(
                title = "通用设置",
                onBack = onBack,
                modifier = modifier,
            ) {
                SettingActionRow(
                    title = "字体大小",
                    value = settings.fontSizeOption.label,
                    onClick = {
                        draftFontSize = settings.fontSizeOption
                        currentPage = GeneralSettingsPage.FontSize
                    },
                )
                SettingActionRow(
                    title = "单条最大高度",
                    value = "${settings.listContentMaxLines}行",
                    onClick = { showMaxLinesDialog = true },
                )
                SettingActionRow(
                    title = "主题设置",
                    value = settings.themeMode.label,
                    onClick = { showThemeDialog = true },
                )
                SettingActionRow(
                    title = "皮肤设置",
                    value = "当前已选",
                    onClick = {
                        draftSkin = settings.skinOption
                        currentPage = GeneralSettingsPage.Skin
                    },
                )
            }
        }

        GeneralSettingsPage.FontSize -> {
            FontSizeSettingScreen(
                selectedOption = draftFontSize,
                onOptionChange = { draftFontSize = it },
                onBack = { currentPage = GeneralSettingsPage.Root },
                onConfirm = {
                    viewModel.setFontSizeOption(draftFontSize)
                    currentPage = GeneralSettingsPage.Root
                },
                modifier = modifier,
            )
        }

        GeneralSettingsPage.Skin -> {
            SkinSettingScreen(
                selectedSkin = draftSkin,
                onSkinChange = { draftSkin = it },
                onBack = { currentPage = GeneralSettingsPage.Root },
                onConfirm = {
                    viewModel.setSkinOption(draftSkin)
                    currentPage = GeneralSettingsPage.Root
                },
                modifier = modifier,
            )
        }
    }

    if (showMaxLinesDialog) {
        SingleChoiceDialog(
            title = "单条最大高度",
            options = listOf(2, 3, 5, 7).map { GeneralChoiceItem(label = "${it}行", value = it) },
            selectedValue = settings.listContentMaxLines,
            onDismiss = { showMaxLinesDialog = false },
            onValueSelected = {
                viewModel.setListContentMaxLines(it)
                showMaxLinesDialog = false
            },
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "主题设置",
            options = AppThemeMode.entries.map { GeneralChoiceItem(label = it.label, value = it) },
            selectedValue = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onValueSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
        )
    }
}

@Composable
fun AboutBianwanluScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(AboutPage.Root) }
    var showFanDialog by remember { mutableStateOf(false) }
    var showRewardDialog by remember { mutableStateOf(false) }

    when (currentPage) {
        AboutPage.Root -> {
            ProfileScaffold(
                title = "关于便玩录",
                onBack = onBack,
                modifier = modifier,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    AboutHeroCard()
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingActionRow(
                        title = "加入粉丝群",
                        onClick = { showFanDialog = true },
                    )
                    SettingActionRow(
                        title = "意见与反馈",
                        onClick = {
                            openExternalLink(
                                context = context,
                                url = FEEDBACK_URL,
                                failureMessage = "暂时无法打开反馈链接",
                            )
                        },
                    )
                    SettingActionRow(
                        title = "打赏作者",
                        onClick = { showRewardDialog = true },
                    )
                    SettingActionRow(
                        title = "用户协议",
                        onClick = { currentPage = AboutPage.UserAgreement },
                    )
                    SettingActionRow(
                        title = "隐私政策",
                        onClick = { currentPage = AboutPage.PrivacyPolicy },
                    )
                }
            }
        }

        AboutPage.UserAgreement -> {
            PolicyContentScreen(
                title = "用户协议",
                sections = userAgreementSections(),
                onBack = { currentPage = AboutPage.Root },
                modifier = modifier,
            )
        }

        AboutPage.PrivacyPolicy -> {
            PolicyContentScreen(
                title = "隐私政策",
                sections = privacyPolicySections(),
                onBack = { currentPage = AboutPage.Root },
                modifier = modifier,
            )
        }
    }

    if (showFanDialog) {
        ProfileActionDialog(
            title = "加入粉丝群",
            options = listOf("QQ群", "微信公众号"),
            onDismiss = { showFanDialog = false },
            onOptionClick = {
                showFanDialog = false
                Toast.makeText(context, "该入口功能暂未开放", Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (showRewardDialog) {
        ProfileActionDialog(
            title = "打赏作者",
            options = listOf("支付宝打赏", "微信打赏"),
            onDismiss = { showRewardDialog = false },
            onOptionClick = {
                showRewardDialog = false
                Toast.makeText(context, "打赏功能暂未开放", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun ProfileScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val iconTint = LocalAppIconTint.current

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
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(6.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalAppIconTint.current.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 24.dp))
}
@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 24.dp))
}

@Composable
private fun PlaceholderBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前先完成页面 UI，后续再补业务功能",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class AboutPage {
    Root,
    UserAgreement,
    PrivacyPolicy,
}

private data class PolicySection(
    val title: String,
    val body: String,
)

@Composable
private fun AboutHeroCard() {
    val context = LocalContext.current
    val versionName = remember(context) { resolveAppVersion(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "应用图标",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "欢迎来到便玩录",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "版本信息 v$versionName",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "便签、待办、提醒与分类管理，都在这里更顺手地完成。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
    }
}

@Composable
fun ProfileActionDialog(
    title: String,
    message: String? = null,
    options: List<String>,
    dismissText: String = "取消",
    onDismiss: () -> Unit,
    onOptionClick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = if (options.isNotEmpty()) 14.dp else 4.dp),
                    )
                }
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onOptionClick(option) },
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (index != options.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(
                text = dismissText,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            )
        },
    )
}

@Composable
private fun PolicyContentScreen(
    title: String,
    sections: List<PolicySection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "生效日期：$POLICY_EFFECTIVE_DATE",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            sections.forEach { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(22.dp),
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = section.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = section.body,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun userAgreementSections(): List<PolicySection> {
    return listOf(
        PolicySection(
            title = "一、服务说明",
            body = "便玩录是一款以本地记录为主的效率工具，提供笔记、待办、提醒、分类管理等功能。本页面内容用于向你说明应用的基本使用规则，不构成正式法律意见。",
        ),
        PolicySection(
            title = "二、使用规则",
            body = "你应当遵守法律法规及公序良俗，合理使用本应用，不得利用便玩录存储违法违规、侵害他人权益或影响系统安全的内容。",
        ),
        PolicySection(
            title = "三、账号与资料",
            body = "你可以选择登录后使用头像、昵称等个人资料功能，也可以暂不登录体验部分能力。登录后请妥善保管账号信息，并确保资料真实、合法、适度。",
        ),
        PolicySection(
            title = "四、内容与权利",
            body = "你创建的笔记、待办、分类、提醒等内容由你自行管理和负责。除法律法规另有规定外，你保留对自己内容的相应权利，并应确保拥有合法使用权限。",
        ),
        PolicySection(
            title = "五、第三方链接与反馈",
            body = "应用内的意见反馈入口会跳转到 GitHub Issues 页面，该页面由第三方提供与运营。你在第三方页面中的访问、发布和账号行为，需遵守对应平台规则。",
        ),
        PolicySection(
            title = "六、免责声明",
            body = "我们会尽力保障应用稳定可用，但受设备状态、系统限制、网络环境或第三方服务变化影响，部分功能可能出现中断、延迟或暂不可用的情况。",
        ),
        PolicySection(
            title = "七、协议更新",
            body = "当产品功能、法律要求或服务策略发生变化时，本说明可能相应更新。更新后的内容将在应用内展示，继续使用即视为你已知悉并接受调整。",
        ),
    )
}

private fun privacyPolicySections(): List<PolicySection> {
    return listOf(
        PolicySection(
            title = "一、说明与原则",
            body = "我们重视你的个人信息与隐私保护。隐私政策依据个人信息保护相关通用原则整理，用于说明便玩录在当前版本下可能涉及的信息类型、用途与权限场景。",
        ),
        PolicySection(
            title = "二、可能处理的信息",
            body = "在你使用应用过程中，便玩录可能处理你主动填写的昵称、头像、本地笔记内容、待办内容、提醒时间、分类信息等数据。其中大部分内容默认存储在你的本地设备中。",
        ),
        PolicySection(
            title = "三、权限与功能对应关系",
            body = "通知权限用于提醒事项到期时向你发送系统通知；图片选择用于设置头像或插入图片；震动开关用于控制提醒触发时是否伴随振动。我们会按照功能最小必要原则请求相关权限。",
        ),
        PolicySection(
            title = "四、存储、共享与删除",
            body = "当前版本以本地存储为主，不会在未经说明的情况下主动向第三方共享你的笔记、待办和分类内容。你可通过应用内编辑、删除等操作自行管理相关数据。",
        ),
        PolicySection(
            title = "五、第三方服务说明",
            body = "当你点击意见与反馈时，应用会打开 GitHub Issues 页面。该跳转属于前往第三方平台的外部链接，第三方平台可能根据其规则处理你的访问和发布行为。",
        ),
        PolicySection(
            title = "六、你的权利",
            body = "你有权查看、修改、删除你在便玩录中主动创建或填写的相关信息，也可以选择退出登录并恢复默认展示资料。如对隐私说明存在疑问，可先通过反馈入口联系我们。",
        ),
        PolicySection(
            title = "七、未成年人保护与更新",
            body = "如果你是未成年人，建议在监护人指导下使用本应用。随着产品能力与法律要求变化，本隐私说明可能更新，更新内容会继续在应用内展示。",
        ),
    )
}

private fun openExternalLink(
    context: Context,
    url: String,
    failureMessage: String,
) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun resolveAppVersion(context: Context): String {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "--"
    }.getOrDefault("--")
}

private const val FEEDBACK_URL = "https://github.com/1xifengdeyouxi/bianwanlu2_0/issues"
private const val POLICY_EFFECTIVE_DATE = "2026年03月14日"

private enum class GeneralSettingsPage {
    Root,
    FontSize,
    Skin,
}

private data class GeneralChoiceItem<T>(
    val label: String,
    val value: T,
)

@Composable
private fun FontSizeSettingScreen(
    selectedOption: AppFontSizeOption,
    onOptionChange: (AppFontSizeOption) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = AppFontSizeOption.entries.map { it.label }

    DetailSettingScaffold(
        title = "字体大小",
        onBack = onBack,
        onConfirm = onConfirm,
        modifier = modifier,
    ) {
        FontSizePreviewBlock(scaleFactor = selectedOption.scaleFactor)
        Spacer(modifier = Modifier.weight(1f))
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Slider(
                value = selectedOption.level.toFloat(),
                onValueChange = { onOptionChange(AppFontSizeOption.fromLevel(it.roundToInt())) },
                valueRange = 0f..4f,
                steps = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSizePreviewBlock(
    scaleFactor: Float,
) {
    fun scaled(base: Int) = (base * scaleFactor).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 8.dp),
    ) {
        Text(
            text = "拖动下面可设置字体大小",
            fontSize = scaled(18),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "字体调整分为较小、标准、大、较大、特大",
            fontSize = scaled(17),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(
                text = "字体调整后，会改变笔记内容、时间管理和完成列表中的字号大小。欢迎反馈意见给我们",
                fontSize = scaled(19),
                lineHeight = scaled(31),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "2026/03/13 16:03",
                fontSize = scaled(13),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SkinSettingScreen(
    selectedSkin: AppSkinOption,
    onSkinChange: (AppSkinOption) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailSettingScaffold(
        title = "皮肤设置",
        onBack = onBack,
        onConfirm = onConfirm,
        modifier = modifier,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            val itemSpacing = 16.dp
            val itemWidth = (maxWidth - itemSpacing * 3) / 4

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppSkinOption.entries.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    ) {
                        rowItems.forEach { skin ->
                            Box(
                                modifier = Modifier
                                    .width(itemWidth)
                                    .aspectRatio(1.25f)
                                    .background(skin.color, RoundedCornerShape(18.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onSkinChange(skin) },
                                    ),
                            ) {
                                if (skin == selectedSkin) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选中",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 14.dp, bottom = 14.dp)
                                            .size(28.dp),
                                    )
                                }
                            }
                        }
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.width(itemWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSettingScaffold(
    title: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val iconTint = LocalAppIconTint.current

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
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "确定",
                fontSize = 17.sp,
                color = accentColor,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onConfirm,
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp),
            content = content,
        )
    }
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<GeneralChoiceItem<T>>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onValueSelected: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onValueSelected(option.value) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.value == selectedValue,
                            onClick = { onValueSelected(option.value) },
                        )
                        Text(
                            text = option.label,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

private fun hasCalendarPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
}

private fun isNotificationPermissionEnabled(context: Context): Boolean {
    val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun isExactAlarmPermissionEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
    return alarmManager.canScheduleExactAlarms()
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun createExactAlarmSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}

private fun createNotificationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}

private fun createBatteryOptimizationSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}





