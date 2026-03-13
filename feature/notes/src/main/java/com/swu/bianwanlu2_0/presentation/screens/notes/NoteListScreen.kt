package com.swu.bianwanlu2_0.presentation.screens.notes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.feature.notes.R
import com.swu.bianwanlu2_0.presentation.components.SwipeRevealDeleteItem
import com.swu.bianwanlu2_0.ui.theme.EmptyIconColor
import com.swu.bianwanlu2_0.ui.theme.EmptyTextColor
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint
import com.swu.bianwanlu2_0.ui.theme.LocalAppListContentMaxLines
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
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsStateWithLifecycle()
    val accentColor = LocalAppIconTint.current
    var openedNoteId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isSelectionMode && openedNoteId != null) {
                        openedNoteId = null
                    }
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isSelectionMode) {
                NoteFilterTabRow(
                    currentFilter = currentFilter,
                    onFilterSelected = viewModel::setFilter
                )
            }

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
                    selectionMode = isSelectionMode,
                    selectedNoteIds = selectedNoteIds,
                    openedNoteId = openedNoteId,
                    onOpenedNoteChange = { openedNoteId = it },
                    onDelete = viewModel::deleteNote,
                    onEdit = onEditNote,
                    onEnterSelection = {
                        openedNoteId = null
                        viewModel.enterSelection(it.id)
                    },
                    onToggleSelection = viewModel::toggleSelection,
                    onReorder = viewModel::reorderNotes,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = onAddNote,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
                containerColor = accentColor
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加笔记",
                    tint = Color.White
                )
            }
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
    selectionMode: Boolean,
    selectedNoteIds: Set<Long>,
    openedNoteId: Long?,
    onOpenedNoteChange: (Long?) -> Unit,
    onDelete: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onEnterSelection: (Note) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onReorder: (List<Note>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (selectionMode) {
            item(key = "note_selection_multi") {
                ReorderableNoteSelectionContent(
                    notes = activeNotes + completedNotes,
                    selectedNoteIds = selectedNoteIds,
                    onToggleSelection = onToggleSelection,
                    onReorder = onReorder
                )
            }
        } else {
            items(activeNotes, key = { "a_${it.id}" }) { note ->
                SwipeNoteItem(
                    note = note,
                    hasAnyRevealed = openedNoteId != null,
                    isRevealed = openedNoteId == note.id,
                    onRevealedChange = { revealed -> onOpenedNoteChange(if (revealed) note.id else null) },
                    onCloseRequested = { onOpenedNoteChange(null) },
                    onDelete = { onDelete(note) },
                    onEdit = { onEdit(note) },
                    onLongPress = { onEnterSelection(note) }
                )
            }
            items(completedNotes, key = { "c_${it.id}" }) { note ->
                SwipeNoteItem(
                    note = note,
                    hasAnyRevealed = openedNoteId != null,
                    isRevealed = openedNoteId == note.id,
                    onRevealedChange = { revealed -> onOpenedNoteChange(if (revealed) note.id else null) },
                    onCloseRequested = { onOpenedNoteChange(null) },
                    onDelete = { onDelete(note) },
                    onEdit = { onEdit(note) },
                    onLongPress = { onEnterSelection(note) }
                )
            }
        }
    }
}

@Composable
private fun ReorderableNoteSelectionContent(
    notes: List<Note>,
    selectedNoteIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onReorder: (List<Note>) -> Unit
) {
    val localNotes = remember { mutableStateListOf<Note>() }
    var isDragging by remember { mutableStateOf(false) }
    var draggingNoteId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var pendingReorderedIds by remember { mutableStateOf<List<Long>?>(null) }
    val itemHeights = remember { mutableStateMapOf<Long, Float>() }
    val swapOffsets = remember { mutableStateMapOf<Long, Float>() }
    val swapVersions = remember { mutableStateMapOf<Long, Int>() }
    val defaultItemStepPx = with(LocalDensity.current) { 96.dp.toPx() }

    LaunchedEffect(notes, isDragging, pendingReorderedIds) {
        if (isDragging) return@LaunchedEffect

        val currentOrderIds = notes.map { it.id }
        val pendingIds = pendingReorderedIds
        if (pendingIds != null && currentOrderIds != pendingIds) {
            return@LaunchedEffect
        }

        pendingReorderedIds = null
        localNotes.clear()
        localNotes.addAll(notes)
    }

    fun recordSwap(noteId: Long, startOffset: Float) {
        swapOffsets[noteId] = startOffset
        swapVersions[noteId] = (swapVersions[noteId] ?: 0) + 1
    }

    fun itemStep(noteId: Long): Float = itemHeights[noteId] ?: defaultItemStepPx

    fun handleDragStart(note: Note) {
        isDragging = true
        draggingNoteId = note.id
        dragOffsetY = 0f
    }

    fun handleDrag(note: Note, deltaY: Float) {
        if (draggingNoteId != note.id) return

        dragOffsetY += deltaY
        var currentIndex = localNotes.indexOfFirst { it.id == note.id }

        while (currentIndex < localNotes.lastIndex) {
            val moveThreshold = itemStep(note.id) / 2f
            if (dragOffsetY < moveThreshold) break
            val movedNote = localNotes[currentIndex + 1]
            val movedOffset = itemStep(movedNote.id)
            localNotes.move(currentIndex, currentIndex + 1)
            dragOffsetY -= movedOffset
            recordSwap(movedNote.id, movedOffset)
            currentIndex++
        }

        while (currentIndex > 0) {
            val moveThreshold = itemStep(note.id) / 2f
            if (dragOffsetY > -moveThreshold) break
            val movedNote = localNotes[currentIndex - 1]
            val movedOffset = itemStep(movedNote.id)
            localNotes.move(currentIndex, currentIndex - 1)
            dragOffsetY += movedOffset
            recordSwap(movedNote.id, -movedOffset)
            currentIndex--
        }
    }

    fun handleDragEnd() {
        val reordered = localNotes.toList()
        pendingReorderedIds = reordered.map { it.id }
        isDragging = false
        draggingNoteId = null
        dragOffsetY = 0f
        onReorder(reordered)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        localNotes.forEach { note ->
            key(note.id) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (draggingNoteId == note.id) 20f else 0f)
                ) {
                    ReorderableNoteRow(
                        note = note,
                        isSelected = note.id in selectedNoteIds,
                        isDragging = draggingNoteId == note.id,
                        dragOffsetY = if (draggingNoteId == note.id) dragOffsetY else 0f,
                        swapOffset = swapOffsets[note.id] ?: 0f,
                        swapVersion = swapVersions[note.id] ?: 0,
                        onHeightChanged = { height -> itemHeights[note.id] = height },
                        onToggleSelection = { onToggleSelection(note.id) },
                        onLongDragStart = { handleDragStart(note) },
                        onLongDrag = { deltaY -> handleDrag(note, deltaY) },
                        onLongDragEnd = ::handleDragEnd
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeNoteItem(
    note: Note,
    hasAnyRevealed: Boolean,
    isRevealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onCloseRequested: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit
) {
    SwipeRevealDeleteItem(
        onDelete = onDelete,
        isRevealed = isRevealed,
        onRevealedChange = onRevealedChange,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isRevealed) 6f else 0f)
    ) {
        NoteCard(
            note = note,
            selectionMode = false,
            isSelected = false,
            onClick = {
                if (hasAnyRevealed) {
                    onCloseRequested()
                } else {
                    onEdit()
                }
            },
            onLongPress = {
                if (hasAnyRevealed) {
                    onCloseRequested()
                } else {
                    onLongPress()
                }
            }
        )
    }
}

@Composable
private fun ReorderableNoteRow(
    note: Note,
    isSelected: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    swapOffset: Float,
    swapVersion: Int,
    onHeightChanged: (Float) -> Unit,
    onToggleSelection: () -> Unit,
    onLongDragStart: () -> Unit,
    onLongDrag: (Float) -> Unit,
    onLongDragEnd: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "note_drag_scale"
    )
    val swapAnimatable = remember { Animatable(0f) }

    LaunchedEffect(swapVersion) {
        if (swapVersion > 0) {
            swapAnimatable.snapTo(swapOffset)
            swapAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    NoteCard(
        note = note,
        selectionMode = true,
        isSelected = isSelected,
        onClick = onToggleSelection,
        onLongPress = null,
        onHeightChanged = onHeightChanged,
        modifier = Modifier
            .graphicsLayer { translationY = if (isDragging) dragOffsetY else swapAnimatable.value }
            .scale(scale),
        onLongDragStart = onLongDragStart,
        onLongDrag = onLongDrag,
        onLongDragEnd = onLongDragEnd
    )
}

@Composable
private fun NoteCard(
    note: Note,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onHeightChanged: ((Float) -> Unit)? = null,
    onLongDragStart: (() -> Unit)? = null,
    onLongDrag: ((Float) -> Unit)? = null,
    onLongDragEnd: (() -> Unit)? = null
) {
    val isCompleted = note.status == NoteStatus.COMPLETED
    val accentColor = LocalAppIconTint.current
    val listContentMaxLines = LocalAppListContentMaxLines.current
    val showPriority = note.isPriority && !isCompleted
    val showTitle = note.title.isNotBlank()
    val hasContent = note.content.isNotBlank()
    val reminderTime = note.reminderTime
    val isExpired = note.status == NoteStatus.ACTIVE &&
        reminderTime != null &&
        reminderTime < System.currentTimeMillis()
    val reminderText = reminderTime?.let { formatReminderDisplay(it, isExpired) }
    val centeredContentMode = !showTitle && !showPriority && reminderText == null && hasContent
    val contentMaxLines = if (showTitle) {
        (listContentMaxLines - 1).coerceAtLeast(1)
    } else {
        listContentMaxLines
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onHeightChanged != null) {
                    Modifier.onSizeChanged { onHeightChanged(it.height.toFloat()) }
                } else {
                    Modifier
                }
            )
            .then(
                if (selectionMode && onLongDragStart != null && onLongDrag != null && onLongDragEnd != null) {
                    Modifier.pointerInput(note.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onLongDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onLongDrag(dragAmount.y)
                            },
                            onDragEnd = onLongDragEnd,
                            onDragCancel = onLongDragEnd
                        )
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (selectionMode) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = { onLongPress?.invoke() }
                    )
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF0F0F0) else Color(note.cardColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                DragHandle(
                    onLongDragStart = onLongDragStart,
                    onLongDrag = onLongDrag,
                    onLongDragEnd = onLongDragEnd
                )
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
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
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (hasContent) {
                    if (centeredContentMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (showTitle || showPriority) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = note.content,
                        fontSize = if (showTitle) 14.sp else 15.sp,
                        color = if (isCompleted) Color(0xFFBDBDBD) else Color(note.textColor),
                        maxLines = if (centeredContentMode) listContentMaxLines else contentMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (centeredContentMode) {
                        Spacer(modifier = Modifier.height(4.dp))
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

            if (selectionMode) {
                Spacer(modifier = Modifier.width(14.dp))
                SelectionBox(
                    selected = isSelected,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun DragHandle(
    onLongDragStart: (() -> Unit)?,
    onLongDrag: ((Float) -> Unit)?,
    onLongDragEnd: (() -> Unit)?
) {
    val iconTint = LocalAppIconTint.current

    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = "拖动排序",
        tint = iconTint,
        modifier = Modifier
            .size(24.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {}
            )
            .then(
                if (onLongDragStart != null && onLongDrag != null && onLongDragEnd != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onLongDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onLongDrag(dragAmount.y)
                            },
                            onDragEnd = onLongDragEnd,
                            onDragCancel = onLongDragEnd
                        )
                    }
                } else {
                    Modifier
                }
            )
    )
}

@Composable
private fun SelectionBox(
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) {
                    Modifier.background(accentColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(1.5.dp, Color(0xFFC7C0B0), RoundedCornerShape(8.dp))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
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

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    add(toIndex, removeAt(fromIndex))
}
