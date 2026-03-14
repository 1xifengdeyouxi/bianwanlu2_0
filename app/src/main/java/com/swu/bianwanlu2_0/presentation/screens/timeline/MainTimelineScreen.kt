package com.swu.bianwanlu2_0.presentation.screens.timeline

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainTimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: MainTimelineViewModel = hiltViewModel(),
) {
    val sourceTab by viewModel.sourceTab.collectAsStateWithLifecycle()
    val actionFilter by viewModel.actionFilter.collectAsStateWithLifecycle()
    val categoryOptions by viewModel.categoryOptions.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val events by viewModel.filteredEvents.collectAsStateWithLifecycle()

    var showActionMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<TimelineDatePickerTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TimelineTabRow(
            selectedTab = sourceTab,
            onTabSelected = viewModel::setSourceTab,
        )
        TimelineFilterRow(
            startDate = startDate,
            endDate = endDate,
            actionLabel = actionFilter.label,
            categoryLabel = categoryOptions.firstOrNull { it.id == selectedCategoryId }?.label ?: "全部分类",
            showActionMenu = showActionMenu,
            showCategoryMenu = showCategoryMenu,
            onStartClick = { datePickerTarget = TimelineDatePickerTarget.START },
            onEndClick = { datePickerTarget = TimelineDatePickerTarget.END },
            onActionExpandedChange = { showActionMenu = it },
            onCategoryExpandedChange = { showCategoryMenu = it },
            onActionSelected = {
                viewModel.setActionFilter(it)
                showActionMenu = false
            },
            onCategorySelected = {
                viewModel.setCategoryFilter(it)
                showCategoryMenu = false
            },
            actionFilter = actionFilter,
            categoryOptions = categoryOptions,
            selectedCategoryId = selectedCategoryId,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        if (events.isEmpty()) {
            TimelineEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = events,
                    key = { event -> "timeline_${event.id}_${event.itemType}_${event.itemId}" },
                ) { event ->
                    TimelineEventRow(event = event)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    datePickerTarget?.let { target ->
        TimelineDatePickerDialog(
            initialDate = if (target == TimelineDatePickerTarget.START) startDate else endDate,
            onDismiss = { datePickerTarget = null },
            onConfirm = { selectedDate ->
                if (target == TimelineDatePickerTarget.START) {
                    viewModel.setStartDate(selectedDate)
                } else {
                    viewModel.setEndDate(selectedDate)
                }
                datePickerTarget = null
            },
            onClear = {
                if (target == TimelineDatePickerTarget.START) {
                    viewModel.setStartDate(null)
                } else {
                    viewModel.setEndDate(null)
                }
                datePickerTarget = null
            },
        )
    }
}

@Composable
private fun TimelineTabRow(
    selectedTab: TimelineSourceTab,
    onTabSelected: (TimelineSourceTab) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        TimelineSourceTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

@Composable
private fun TimelineFilterRow(
    startDate: Long?,
    endDate: Long?,
    actionLabel: String,
    categoryLabel: String,
    showActionMenu: Boolean,
    showCategoryMenu: Boolean,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onActionExpandedChange: (Boolean) -> Unit,
    onCategoryExpandedChange: (Boolean) -> Unit,
    onActionSelected: (TimelineActionFilter) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    actionFilter: TimelineActionFilter,
    categoryOptions: List<TimelineCategoryFilterOption>,
    selectedCategoryId: Long?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TimelineFilterButton(
            label = startDate?.let(::formatCompactFilterDate) ?: "开始时间",
            expanded = false,
            modifier = Modifier.weight(1f),
            onClick = onStartClick,
        )
        TimelineFilterButton(
            label = endDate?.let(::formatCompactFilterDate) ?: "结束时间",
            expanded = false,
            modifier = Modifier.weight(1f),
            onClick = onEndClick,
        )
        Box(modifier = Modifier.weight(1f)) {
            TimelineFilterButton(
                label = actionLabel,
                expanded = showActionMenu,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onActionExpandedChange(!showActionMenu) },
            )
            DropdownMenu(
                expanded = showActionMenu,
                onDismissRequest = { onActionExpandedChange(false) },
            ) {
                TimelineActionFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = filter.label,
                                color = if (filter == actionFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = { onActionSelected(filter) },
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            TimelineFilterButton(
                label = compressTimelineCategoryLabel(categoryLabel),
                expanded = showCategoryMenu,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCategoryExpandedChange(!showCategoryMenu) },
            )
            DropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { onCategoryExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "全部分类",
                            color = if (selectedCategoryId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onCategorySelected(null) },
                )
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (selectedCategoryId == option.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = { onCategorySelected(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineFilterButton(
    label: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEvent,
) {
    val actionColor = event.actionType.displayColor()
    val icon = event.actionType.displayIcon()
    val titleText = event.title.trim()
    val contentText = event.contentPreview.trim().takeIf { it.isNotBlank() && it != titleText }
    val metaLabel = buildString {
        append(event.itemType.displayLabel())
        if (event.categoryName.isNotBlank()) {
            append(" · ")
            append(event.categoryName)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
    ) {
        Column(
            modifier = Modifier.widthIn(min = 56.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = formatTime(event.occurredAt),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDay(event.occurredAt),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = actionColor,
                shadowElevation = 1.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = event.actionType.label,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = metaLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.actionType.label,
                    fontSize = 14.sp,
                    color = actionColor,
                )
            }

            if (titleText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!contentText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = contentText,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val reminderReferenceTime = event.referenceTime
            if (event.actionType == TimelineActionType.REMINDER && reminderReferenceTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "提醒时间 ${formatReminderDateTime(reminderReferenceTime)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun TimelineEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "当前筛选条件下还没有时间轴记录",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "你可以调整开始时间、结束时间、操作类型或分类后再看看。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineDatePickerDialog(
    initialDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val calendar = remember(initialDate) {
        Calendar.getInstance().apply {
            timeInMillis = initialDate ?: System.currentTimeMillis()
        }
    }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember(currentYear) { (currentYear - 5..currentYear + 5).toList() }

    var selectedYear by remember(calendar) { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember(calendar) { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember(calendar) { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val days = remember(selectedYear, selectedMonth) {
        (1..daysInMonth(selectedYear, selectedMonth)).toList()
    }
    LaunchedEffect(days) {
        if (selectedDay > days.last()) {
            selectedDay = days.last()
        }
    }

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
                            text = "清空",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onClear),
                        )
                        Text(
                            text = "确认",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onConfirm(
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, selectedYear)
                                        set(Calendar.MONTH, selectedMonth - 1)
                                        set(Calendar.DAY_OF_MONTH, selectedDay)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                )
                            },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WheelPicker(
                            values = years,
                            selectedValue = selectedYear,
                            formatter = { "${it}年" },
                            modifier = Modifier.weight(1f),
                            onValueSelected = { selectedYear = it },
                        )
                        WheelPicker(
                            values = (1..12).toList(),
                            selectedValue = selectedMonth,
                            formatter = { "%02d月".format(it) },
                            modifier = Modifier.weight(1f),
                            onValueSelected = { selectedMonth = it },
                        )
                        WheelPicker(
                            values = days,
                            selectedValue = selectedDay,
                            formatter = { "%02d日".format(it) },
                            modifier = Modifier.weight(1f),
                            onValueSelected = { selectedDay = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPicker(
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

private fun TimelineActionType.displayColor(): Color {
    return when (this) {
        TimelineActionType.CREATE -> Color(0xFF4C8DFF)
        TimelineActionType.UPDATE -> Color(0xFF5C6BC0)
        TimelineActionType.DELETE -> Color(0xFFE53935)
        TimelineActionType.COMPLETE -> Color(0xFF43A047)
        TimelineActionType.REMINDER -> Color(0xFFFF9800)
    }
}

private fun TimelineActionType.displayIcon() = when (this) {
    TimelineActionType.CREATE -> Icons.Default.Add
    TimelineActionType.UPDATE -> Icons.Default.Edit
    TimelineActionType.DELETE -> Icons.Default.Delete
    TimelineActionType.COMPLETE -> Icons.Default.Check
    TimelineActionType.REMINDER -> Icons.Outlined.NotificationsNone
}

private fun TimelineItemType.displayLabel(): String {
    return when (this) {
        TimelineItemType.NOTE -> "笔记"
        TimelineItemType.TODO -> "待办"
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun formatTime(timeMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))

private fun formatDay(timeMillis: Long): String =
    SimpleDateFormat("M/d", Locale.getDefault()).format(Date(timeMillis))

private fun formatFilterDate(timeMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timeMillis))

private fun formatCompactFilterDate(timeMillis: Long): String =
    SimpleDateFormat("yy/MM/dd", Locale.getDefault()).format(Date(timeMillis))

private fun formatReminderDateTime(timeMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timeMillis))

private fun compressTimelineCategoryLabel(label: String): String {
    if (label == "全部分类") return label
    val compact = label.replace(" · ", "/")
    val suffix = compact.substringAfter('/', compact)
    return if (compact.length <= 8) compact else suffix
}

private enum class TimelineDatePickerTarget {
    START,
    END,
}

