package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.R
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.ui.theme.EmptyIconColor
import com.swu.bianwanlu2_0.ui.theme.EmptyTextColor
import com.swu.bianwanlu2_0.ui.theme.NoteRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val completedNotes by viewModel.completedNotes.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NoteFilterTabRow(
                currentFilter = currentFilter,
                onFilterSelected = viewModel::setFilter
            )

            if (activeNotes.isEmpty() && completedNotes.isEmpty()) {
                NoteEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                NoteContent(
                    activeNotes = activeNotes,
                    completedNotes = completedNotes,
                    onToggle = viewModel::toggleComplete,
                    onDelete = viewModel::deleteNote,
                    onEdit = onEditNote,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FloatingActionButton(
            onClick = onAddNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            containerColor = NoteRed
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加笔记",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun NoteFilterTabRow(
    currentFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(NoteFilter.entries) { filter ->
            FilterChip(
                label = filter.label,
                isSelected = currentFilter == filter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(
                color = if (isSelected) Color(0xFFF5F5F5) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .then(
                if (!isSelected) Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color(0xFF212121) else Color(0xFF757575)
        )
    }
}

@Composable
private fun NoteContent(
    activeNotes: List<Note>,
    completedNotes: List<Note>,
    onToggle: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(activeNotes, key = { "a_${it.id}" }) { note ->
            SwipeNoteItem(
                note = note,
                onToggle = { onToggle(note) },
                onDelete = { onDelete(note) },
                onEdit = { onEdit(note) }
            )
        }
        items(completedNotes, key = { "c_${it.id}" }) { note ->
            SwipeNoteItem(
                note = note,
                onToggle = { onToggle(note) },
                onDelete = { onDelete(note) },
                onEdit = { onEdit(note) }
            )
        }
    }
}

@Composable
private fun SwipeNoteItem(
    note: Note,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onToggle()
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val targetColor = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF66BB6A)
                SwipeToDismissBoxValue.EndToStart -> NoteRed
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val color by animateColorAsState(targetColor, label = "swipe_bg")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
            ) {
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(Icons.Default.Check, contentDescription = "完成", tint = Color.White)
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                    }
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        NoteCard(note = note, onEdit = onEdit)
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = note.status == NoteStatus.COMPLETED
    val showPriority = note.isPriority && !isCompleted
    val showTitle = note.title.isNotBlank()
    val hasContent = note.content.isNotBlank()
    val isExpired = note.status == NoteStatus.ACTIVE &&
            note.reminderTime != null &&
            note.reminderTime < System.currentTimeMillis()
    val reminderText = note.reminderTime?.let { formatReminderDisplay(it, isExpired) }
    val centeredContentMode = !showTitle && !showPriority && reminderText == null && hasContent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF0F0F0) else Color(note.cardColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (showTitle || showPriority) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showTitle) {
                        Text(
                            text = note.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) Color(0xFFBDBDBD) else Color(0xFF212121),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (showPriority) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "高优先级",
                            tint = NoteRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (hasContent) {
                if (centeredContentMode) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (showTitle || showPriority) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = note.content,
                    fontSize = if (showTitle) 14.sp else 15.sp,
                    color = if (isCompleted) Color(0xFFBDBDBD) else Color(note.textColor),
                    maxLines = if (centeredContentMode) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (centeredContentMode) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (reminderText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminderText,
                    fontSize = 12.sp,
                    color = if (isExpired) NoteRed else Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
private fun NoteEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_note_empty),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EmptyIconColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "目前没有笔记哦~\n点击下方\"+\"号来添加吧!",
            color = EmptyTextColor,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatReminderDisplay(reminder: Long, isExpired: Boolean): String {
    val reminderCal = Calendar.getInstance().apply { timeInMillis = reminder }
    val nowCal = Calendar.getInstance()
    val isToday = reminderCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            reminderCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFmt.format(Date(reminder))
    return if (isToday) {
        if (isExpired) "今天 $timeStr 已过期" else "今天 $timeStr"
    } else {
        formatTime(reminder)
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
