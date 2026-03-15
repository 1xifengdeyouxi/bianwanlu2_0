package com.swu.bianwanlu2_0.presentation.screens.calendar

import android.widget.NumberPicker
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CALENDAR_MONTH_SWIPE_THRESHOLD = 84f
private const val CALENDAR_DAY_REMINDER_PREVIEW_LIMIT = 2

private fun calendarFadeContentTransform(
    enterDurationMillis: Int = 180,
    enterDelayMillis: Int = 40,
    exitDurationMillis: Int = 120,
): ContentTransform {
    return fadeIn(
        animationSpec = tween(
            durationMillis = enterDurationMillis,
            delayMillis = enterDelayMillis,
        ),
    ).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = exitDurationMillis),
        ),
    )
}

enum class CalendarReminderItemType {
    NOTE,
    TODO,
}

enum class CalendarDayBadgeType {
    FESTIVAL,
    REST_DAY,
    WORK_DAY,
}

data class CalendarReminderItemUi(
    val uniqueKey: String,
    val itemId: Long,
    val type: CalendarReminderItemType,
    val title: String,
    val content: String,
    val categoryName: String,
    val reminderTime: Long,
    val createdAt: Long,
    val isCompleted: Boolean,
    val isPriority: Boolean,
    val note: Note? = null,
    val todo: Todo? = null,
)

data class CalendarDayCellUi(
    val key: String,
    val dateMillis: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val badgeLabel: String? = null,
    val badgeType: CalendarDayBadgeType? = null,
    val reminders: List<CalendarReminderItemUi> = emptyList(),
    val hiddenReminderCount: Int = 0,
    val totalReminderCount: Int = 0,
)

data class CalendarUiState(
    val monthStartMillis: Long,
    val toolbarTitle: String,
    val monthDescription: String,
    val summaryText: String,
    val year: Int,
    val month: Int,
    val weekLabels: List<String>,
    val days: List<CalendarDayCellUi>,
    val isCurrentMonthDisplayed: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    isRefreshing: Boolean,
    isMonthPickerVisible: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDismissMonthPicker: () -> Unit,
    onSelectMonth: (Int, Int) -> Unit,
    onJumpToToday: () -> Unit,
    onRefresh: () -> Unit,
    onOpenReminder: (CalendarReminderItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedReminder by remember { mutableStateOf<CalendarReminderItemUi?>(null) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }
    var dayDialogKey by remember { mutableStateOf<String?>(null) }
    var dragDistance by remember(uiState.monthStartMillis) { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.monthStartMillis) {
        selectedDayKey = uiState.days.firstOrNull { it.isToday && it.isCurrentMonth }?.key
            ?: uiState.days.firstOrNull { it.isCurrentMonth }?.key
            ?: uiState.days.firstOrNull()?.key
        dayDialogKey = null
        dragDistance = 0f
    }

    LaunchedEffect(uiState.days) {
        if (selectedDayKey != null && uiState.days.none { it.key == selectedDayKey }) {
            selectedDayKey = uiState.days.firstOrNull { it.isCurrentMonth }?.key
                ?: uiState.days.firstOrNull()?.key
        }
        if (dayDialogKey != null && uiState.days.none { it.key == dayDialogKey }) {
            dayDialogKey = null
        }
    }

    val selectedDay = remember(uiState.days, selectedDayKey) {
        uiState.days.firstOrNull { it.key == selectedDayKey }
            ?: uiState.days.firstOrNull { it.isToday && it.isCurrentMonth }
            ?: uiState.days.firstOrNull { it.isCurrentMonth }
            ?: uiState.days.firstOrNull()
    }
    val dayDialogItem = remember(uiState.days, dayDialogKey) {
        uiState.days.firstOrNull { it.key == dayDialogKey }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarMonthHeader(
                monthDescription = uiState.monthDescription,
                summaryText = uiState.summaryText,
                showTodayAction = !uiState.isCurrentMonthDisplayed,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onJumpToToday = onJumpToToday,
            )
            selectedDay?.let { day ->
                CalendarSelectedDayCard(
                    day = day,
                    onClick = { dayDialogKey = day.key },
                )
            }
            CalendarWeekHeader(weekLabels = uiState.weekLabels)
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val calendarWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val swipeThresholdPx = if (calendarWidthPx > 0f) {
                    maxOf(CALENDAR_MONTH_SWIPE_THRESHOLD, calendarWidthPx * 0.16f)
                } else {
                    CALENDAR_MONTH_SWIPE_THRESHOLD
                }
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = { calendarFadeContentTransform() },
                    contentKey = { it.monthStartMillis },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uiState.monthStartMillis, swipeThresholdPx, calendarWidthPx) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    val maxDrag = if (calendarWidthPx > 0f) calendarWidthPx else 520f
                                    dragDistance = (dragDistance + dragAmount)
                                        .coerceIn(-maxDrag, maxDrag)
                                },
                                onDragEnd = {
                                    val finalDragDistance = dragDistance
                                    when {
                                        finalDragDistance >= swipeThresholdPx -> onPreviousMonth()
                                        finalDragDistance <= -swipeThresholdPx -> onNextMonth()
                                    }
                                    dragDistance = 0f
                                },
                                onDragCancel = {
                                    dragDistance = 0f
                                },
                            )
                        },
                    label = "calendar_month_content",
                ) { animatedUiState ->
                    val weeks = remember(animatedUiState.days) { animatedUiState.days.chunked(7) }
                    val rowHeight = if (weeks.isNotEmpty()) maxHeight / weeks.size else 0.dp
                    Column(modifier = Modifier.fillMaxSize()) {
                        weeks.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight),
                            ) {
                                week.forEach { day ->
                                    CalendarDayCell(
                                        day = day,
                                        isSelected = day.key == selectedDayKey,
                                        modifier = Modifier.weight(1f),
                                        onDayClick = {
                                            selectedDayKey = day.key
                                            dayDialogKey = day.key
                                        },
                                        onReminderClick = { reminder ->
                                            selectedDayKey = day.key
                                            selectedReminder = reminder
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isMonthPickerVisible) {
        CalendarMonthPickerDialog(
            initialYear = uiState.year,
            initialMonth = uiState.month,
            onDismiss = onDismissMonthPicker,
            onConfirm = onSelectMonth,
        )
    }

    dayDialogItem?.let { day ->
        CalendarDayOverviewDialog(
            day = day,
            onDismiss = { dayDialogKey = null },
            onReminderClick = { reminder ->
                dayDialogKey = null
                selectedReminder = reminder
            },
        )
    }

    selectedReminder?.let { reminder ->
        CalendarReminderDetailDialog(
            item = reminder,
            onDismiss = { selectedReminder = null },
            onOpen = {
                selectedReminder = null
                onOpenReminder(reminder)
            },
        )
    }
}

@Composable
private fun CalendarMonthHeader(
    monthDescription: String,
    summaryText: String,
    showTodayAction: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monthDescription,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summaryText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onJumpToToday,
                enabled = showTodayAction,
                modifier = Modifier
                    .width(72.dp)
                    .graphicsLayer { alpha = if (showTodayAction) 1f else 0f },
            ) {
                Text(text = "\u4eca\u5929", fontSize = 14.sp)
            }
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "上个月",
                )
            }
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "下个月",
                )
            }
        }
    }
}

@Composable
private fun CalendarSelectedDayCard(
    day: CalendarDayCellUi,
    onClick: () -> Unit,
) {
    val description = when {
        day.totalReminderCount > 0 -> "当天共 ${day.totalReminderCount} 条提醒，点击查看全部"
        day.badgeLabel != null -> "当天暂无提醒 · ${day.badgeLabel}"
        else -> "当天暂无提醒，点击查看详情"
    }
    val firstReminder = day.reminders.firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSelectedDayText(day.dateMillis),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (day.isToday) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CalendarInlineTag(
                        text = "今天",
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
                day.badgeLabel?.let { label ->
                    Spacer(modifier = Modifier.width(8.dp))
                    CalendarInlineTag(
                        text = label,
                        backgroundColor = calendarBadgeBackground(day.badgeType),
                        contentColor = calendarBadgeContent(day.badgeType),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (day.totalReminderCount > 0) "查看全部" else "查看",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            firstReminder?.let {
                Text(
                    text = "最近提醒：${formatReminderClock(it.reminderTime)} · ${it.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CalendarWeekHeader(weekLabels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        weekLabels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (index >= 5) Color(0xFFE06C5C) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayCellUi,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onDayClick: () -> Unit,
    onReminderClick: (CalendarReminderItemUi) -> Unit,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        day.isCurrentMonth -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .border(width = if (isSelected) 1.1.dp else 0.5.dp, color = borderColor)
            .background(backgroundColor)
            .clickable(onClick = onDayClick)
            .padding(horizontal = 5.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (day.isToday || isSelected) 24.dp else 22.dp)
                    .background(
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            else -> Color.Transparent
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (day.isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isSelected || day.isToday -> MaterialTheme.colorScheme.onPrimary
                        day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            day.badgeLabel?.let { label ->
                CalendarDayBadge(
                    label = label,
                    type = day.badgeType,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (day.reminders.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                day.reminders.take(CALENDAR_DAY_REMINDER_PREVIEW_LIMIT).forEach { reminder ->
                    CalendarReminderChip(
                        item = reminder,
                        onClick = { onReminderClick(reminder) },
                    )
                }
                if (day.hiddenReminderCount > 0) {
                    Text(
                        text = "\u8fd8\u6709${day.hiddenReminderCount}\u9879",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayBadge(
    label: String,
    type: CalendarDayBadgeType?,
) {
    val backgroundColor = when (type) {
        CalendarDayBadgeType.FESTIVAL -> Color(0xFFFFE3E0)
        CalendarDayBadgeType.REST_DAY -> Color(0xFFEAF4FF)
        CalendarDayBadgeType.WORK_DAY -> Color(0xFFFFF0DA)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (type) {
        CalendarDayBadgeType.FESTIVAL -> Color(0xFFD65A4A)
        CalendarDayBadgeType.REST_DAY -> Color(0xFF4C8DFF)
        CalendarDayBadgeType.WORK_DAY -> Color(0xFFCC8A28)
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun CalendarReminderChip(
    item: CalendarReminderItemUi,
    onClick: () -> Unit,
) {
    val backgroundColor = when (item.type) {
        CalendarReminderItemType.NOTE -> Color(0xFFF0E7FF)
        CalendarReminderItemType.TODO -> Color(0xFFE1F5EC)
    }.copy(alpha = if (item.isCompleted) 0.55f else 1f)
    val contentColor = when (item.type) {
        CalendarReminderItemType.NOTE -> Color(0xFF6F4CC3)
        CalendarReminderItemType.TODO -> Color(0xFF2F8F62)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(contentColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        )
        if (item.isPriority) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "!",
                fontSize = 11.sp,
                color = Color(0xFFE76F51),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CalendarMonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember(currentYear, initialYear) {
        val startYear = minOf(currentYear, initialYear) - 5
        val endYear = maxOf(currentYear, initialYear) + 5
        (startYear..endYear).toList()
    }
    var selectedYear by remember(initialYear) { mutableIntStateOf(initialYear) }
    var selectedMonth by remember(initialMonth) { mutableIntStateOf(initialMonth) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onDismiss),
                        )
                        Text(
                            text = "确定",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onConfirm(selectedYear, selectedMonth) },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CalendarWheelPicker(
                            values = years,
                            selectedValue = selectedYear,
                            formatter = { "${it}年" },
                            modifier = Modifier.weight(1f),
                            onValueSelected = { selectedYear = it },
                        )
                        CalendarWheelPicker(
                            values = (1..12).toList(),
                            selectedValue = selectedMonth,
                            formatter = { "%02d月".format(it) },
                            modifier = Modifier.weight(1f),
                            onValueSelected = { selectedMonth = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarWheelPicker(
    values: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    modifier: Modifier = Modifier,
    onValueSelected: (Int) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = false
            }
        },
        update = { picker ->
            val displayedValues = values.map(formatter).toTypedArray()
            picker.minValue = 0
            picker.maxValue = displayedValues.lastIndex
            picker.displayedValues = null
            picker.displayedValues = displayedValues
            val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
            if (picker.value != selectedIndex) {
                picker.value = selectedIndex
            }
            picker.setOnValueChangedListener { _, _, newValue ->
                values.getOrNull(newValue)?.let(onValueSelected)
            }
        },
    )
}

@Composable
private fun CalendarDayOverviewDialog(
    day: CalendarDayCellUi,
    onDismiss: () -> Unit,
    onReminderClick: (CalendarReminderItemUi) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatDayDialogTitle(day.dateMillis),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (day.totalReminderCount > 0) {
                                        "\u5f53\u5929\u5171\u6709 ${day.totalReminderCount} \u6761\u63d0\u9192"
                                    } else {
                                        "\u5f53\u5929\u6682\u65e0\u63d0\u9192"
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = onDismiss) {
                                Text("\u5173\u95ed")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (day.isToday) {
                                CalendarInlineTag(
                                    text = "\u4eca\u5929",
                                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                )
                            }
                            day.badgeLabel?.let { label ->
                                CalendarInlineTag(
                                    text = label,
                                    backgroundColor = calendarBadgeBackground(day.badgeType),
                                    contentColor = calendarBadgeContent(day.badgeType),
                                )
                            }
                            CalendarInlineTag(
                                text = if (day.totalReminderCount > 0) "\u5217\u8868\u5df2\u6309\u63d0\u9192\u65f6\u95f4\u6392\u5e8f" else "\u4e0b\u62c9\u53ef\u5237\u65b0\u540c\u6b65\u6700\u65b0\u5185\u5bb9",
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (day.reminders.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsNone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "\u8fd9\u4e00\u5929\u8fd8\u6ca1\u6709\u63d0\u9192\u5b89\u6392",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = day.reminders,
                            key = { it.uniqueKey },
                        ) { reminder ->
                            CalendarDayReminderItem(
                                item = reminder,
                                onClick = { onReminderClick(reminder) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayReminderItem(
    item: CalendarReminderItemUi,
    onClick: () -> Unit,
) {
    val iconTint = when (item.type) {
        CalendarReminderItemType.NOTE -> Color(0xFF6F4CC3)
        CalendarReminderItemType.TODO -> Color(0xFF2F8F62)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (item.type) {
                        CalendarReminderItemType.NOTE -> Icons.AutoMirrored.Outlined.StickyNote2
                        CalendarReminderItemType.TODO -> Icons.Outlined.CheckCircle
                    },
                    contentDescription = null,
                    tint = iconTint,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    CalendarInlineTag(
                        text = formatReminderClock(item.reminderTime),
                        backgroundColor = iconTint.copy(alpha = 0.12f),
                        contentColor = iconTint,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.isPriority) {
                        CalendarInlineTag(
                            text = "\u4f18\u5148",
                            backgroundColor = Color(0xFFE76F51).copy(alpha = 0.12f),
                            contentColor = Color(0xFFE76F51),
                        )
                    }
                    if (item.categoryName.isNotBlank()) {
                        CalendarInlineTag(
                            text = item.categoryName,
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    CalendarInlineTag(
                        text = if (item.type == CalendarReminderItemType.NOTE) "\u7b14\u8bb0" else "\u5f85\u529e",
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = buildReminderMeta(item),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.content.isNotBlank()) {
                    Text(
                        text = item.content,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarInlineTag(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalendarReminderDetailDialog(
    item: CalendarReminderItemUi,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (item.type) {
                    CalendarReminderItemType.NOTE -> Icons.AutoMirrored.Outlined.StickyNote2
                    CalendarReminderItemType.TODO -> Icons.Outlined.CheckCircle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMetaRow(
                    label = "类型",
                    value = if (item.type == CalendarReminderItemType.NOTE) "笔记" else "待办",
                )
                if (item.categoryName.isNotBlank()) {
                    DetailMetaRow(label = "分类", value = item.categoryName)
                }
                DetailMetaRow(label = "提醒", value = formatDetailDateTime(item.reminderTime))
                DetailMetaRow(label = "创建", value = formatDetailDateTime(item.createdAt))
                if (item.isCompleted) {
                    DetailMetaRow(label = "状态", value = "已完成")
                }
                if (item.content.isNotBlank()) {
                    Text(
                        text = item.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Text("打开")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
private fun DetailMetaRow(
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label：",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun buildReminderMeta(item: CalendarReminderItemUi): String {
    val parts = buildList {
        add(if (item.type == CalendarReminderItemType.NOTE) "笔记" else "待办")
        add(formatReminderClock(item.reminderTime))
        if (item.categoryName.isNotBlank()) add(item.categoryName)
        if (item.isCompleted) add("已完成")
    }
    return parts.joinToString(" · ")
}

@Composable
private fun calendarBadgeBackground(dayBadgeType: CalendarDayBadgeType?): Color = when (dayBadgeType) {
    CalendarDayBadgeType.FESTIVAL -> Color(0xFFFFE3E0)
    CalendarDayBadgeType.REST_DAY -> Color(0xFFEAF4FF)
    CalendarDayBadgeType.WORK_DAY -> Color(0xFFFFF0DA)
    null -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun calendarBadgeContent(dayBadgeType: CalendarDayBadgeType?): Color = when (dayBadgeType) {
    CalendarDayBadgeType.FESTIVAL -> Color(0xFFD65A4A)
    CalendarDayBadgeType.REST_DAY -> Color(0xFF4C8DFF)
    CalendarDayBadgeType.WORK_DAY -> Color(0xFFCC8A28)
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatReminderClock(timeMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))

private fun formatDetailDateTime(timeMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timeMillis))

private fun formatSelectedDayText(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 ${weekdayLabel(calendar.get(Calendar.DAY_OF_WEEK))}"
}

private fun formatDayDialogTitle(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return "${calendar.get(Calendar.YEAR)}年${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 ${weekdayLabel(calendar.get(Calendar.DAY_OF_WEEK))}"
}

private fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "周一"
    Calendar.TUESDAY -> "周二"
    Calendar.WEDNESDAY -> "周三"
    Calendar.THURSDAY -> "周四"
    Calendar.FRIDAY -> "周五"
    Calendar.SATURDAY -> "周六"
    Calendar.SUNDAY -> "周日"
    else -> ""
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CalendarScreenPreview() {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val first = todayStart.timeInMillis
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val sampleDays = buildList {
        val offset = (todayStart.get(Calendar.DAY_OF_WEEK) + 5) % 7
        todayStart.add(Calendar.DAY_OF_MONTH, -offset)
        repeat(42) { index ->
            val dayMillis = Calendar.getInstance().apply {
                timeInMillis = todayStart.timeInMillis
                add(Calendar.DAY_OF_MONTH, index)
            }.timeInMillis
            add(
                CalendarDayCellUi(
                    key = "preview_$index",
                    dateMillis = dayMillis,
                    dayOfMonth = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.MONTH) == currentMonth,
                    isToday = index == 12,
                    badgeLabel = if (index == 0) "\u52b3\u52a8" else if (index % 7 == 5 || index % 7 == 6) "\u4f11" else null,
                    badgeType = if (index == 0) CalendarDayBadgeType.FESTIVAL else if (index % 7 == 5 || index % 7 == 6) CalendarDayBadgeType.REST_DAY else null,
                    reminders = if (index == 12) {
                        listOf(
                            CalendarReminderItemUi(
                                uniqueKey = "note_preview",
                                itemId = 1,
                                type = CalendarReminderItemType.NOTE,
                                title = "\u5f00\u4f1a\u8bb0\u5f55",
                                content = "\u4e0b\u5348\u4e24\u70b9\u9879\u76ee\u540c\u6b65\uff0c\u8bb0\u5f97\u8865\u5145\u5468\u62a5\u3002",
                                categoryName = "\u5de5\u4f5c",
                                reminderTime = today.timeInMillis,
                                createdAt = today.timeInMillis,
                                isCompleted = false,
                                isPriority = true,
                            ),
                            CalendarReminderItemUi(
                                uniqueKey = "todo_preview",
                                itemId = 2,
                                type = CalendarReminderItemType.TODO,
                                title = "\u4ea4\u65e5\u62a5",
                                content = "18:00 \u524d\u63d0\u4ea4",
                                categoryName = "\u5f85\u529e",
                                reminderTime = today.timeInMillis,
                                createdAt = today.timeInMillis,
                                isCompleted = false,
                                isPriority = false,
                            ),
                            CalendarReminderItemUi(
                                uniqueKey = "todo_preview_2",
                                itemId = 3,
                                type = CalendarReminderItemType.TODO,
                                title = "\u4e70\u5496\u5561\u8c46",
                                content = "\u665a\u4e0a\u4e0b\u73ed\u987a\u8def\u5e26\u56de\u5bb6",
                                categoryName = "\u751f\u6d3b",
                                reminderTime = today.timeInMillis,
                                createdAt = today.timeInMillis,
                                isCompleted = true,
                                isPriority = false,
                            ),
                        )
                    } else {
                        emptyList()
                    },
                    hiddenReminderCount = if (index == 12) 1 else 0,
                    totalReminderCount = if (index == 12) 3 else 0,
                ),
            )
        }
    }

    Bianwanlu2_0Theme {
        CalendarScreen(
            uiState = CalendarUiState(
                monthStartMillis = first,
                toolbarTitle = "2026\u5e745\u6708",
                monthDescription = "2026\u5e745\u6708",
                summaryText = "\u672c\u6708 12 \u6761\u63d0\u9192 \u00b7 \u5de6\u53f3\u6ed1\u52a8\u5207\u6362\u6708\u4efd",
                year = 2026,
                month = 5,
                weekLabels = listOf("\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5"),
                days = sampleDays,
                isCurrentMonthDisplayed = false,
            ),
            isRefreshing = false,
            isMonthPickerVisible = false,
            onPreviousMonth = {},
            onNextMonth = {},
            onDismissMonthPicker = {},
            onSelectMonth = { _, _ -> },
            onJumpToToday = {},
            onRefresh = {},
            onOpenReminder = {},
        )
    }
}
