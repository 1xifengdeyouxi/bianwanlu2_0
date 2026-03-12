package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.feature.todo.R
import com.swu.bianwanlu2_0.presentation.components.SwipeRevealDeleteItem
import com.swu.bianwanlu2_0.ui.theme.EmptyIconColor
import com.swu.bianwanlu2_0.ui.theme.EmptyTextColor
import com.swu.bianwanlu2_0.ui.theme.NoteRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    onAddTodo: () -> Unit,
    onEditTodo: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTodos by viewModel.activeTodos.collectAsStateWithLifecycle()
    val completedTodos by viewModel.completedTodos.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTodoIds by viewModel.selectedTodoIds.collectAsStateWithLifecycle()
    var openedTodoId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isSelectionMode && openedTodoId != null) {
                        openedTodoId = null
                    }
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isSelectionMode) {
                FilterTabRow(
                    currentFilter = currentFilter,
                    onFilterSelected = viewModel::setFilter
                )
            }

            if (activeTodos.isEmpty() && completedTodos.isEmpty()) {
                TodoEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                TodoContent(
                    activeTodos = activeTodos,
                    completedTodos = completedTodos,
                    selectionMode = isSelectionMode,
                    selectedTodoIds = selectedTodoIds,
                    openedTodoId = openedTodoId,
                    onOpenedTodoChange = { openedTodoId = it },
                    onEnterSelection = {
                        openedTodoId = null
                        viewModel.enterSelection(it.id)
                    },
                    onToggleSelection = viewModel::toggleSelection,
                    onToggle = viewModel::toggleComplete,
                    onDelete = viewModel::deleteTodo,
                    onEdit = onEditTodo,
                    onReorder = viewModel::reorderTodos,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = onAddTodo,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
                containerColor = NoteRed
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加待办",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FilterTabRow(
    currentFilter: TodoFilter,
    onFilterSelected: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(TodoFilter.entries) { filter ->
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
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFF5F5F5) else Color.Transparent)
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
private fun TodoContent(
    activeTodos: List<Todo>,
    completedTodos: List<Todo>,
    selectionMode: Boolean,
    selectedTodoIds: Set<Long>,
    openedTodoId: Long?,
    onOpenedTodoChange: (Long?) -> Unit,
    onEnterSelection: (Todo) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onEdit: (Todo) -> Unit,
    onReorder: (List<Todo>) -> Unit,
    modifier: Modifier = Modifier
) {
    var completedExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (selectionMode) {
            item(key = "selection_multi") {
                ReorderableTodoSelectionContent(
                    activeTodos = activeTodos,
                    completedTodos = completedTodos,
                    selectedTodoIds = selectedTodoIds,
                    onToggleSelection = onToggleSelection,
                    onReorder = onReorder
                )
            }
        } else {
            items(activeTodos, key = { "a_${it.id}" }) { todo ->
                SwipeToDeleteTodoItem(
                    todo = todo,
                    hasAnyRevealed = openedTodoId != null,
                    isRevealed = openedTodoId == todo.id,
                    onRevealedChange = { revealed -> onOpenedTodoChange(if (revealed) todo.id else null) },
                    onCloseRequested = { onOpenedTodoChange(null) },
                    onToggle = { onToggle(todo) },
                    onDelete = { onDelete(todo) },
                    onEdit = { onEdit(todo) },
                    onLongPress = { onEnterSelection(todo) }
                )
            }
        }

        if (!selectionMode && completedTodos.isNotEmpty()) {
            item(key = "completed_header") {
                Spacer(modifier = Modifier.height(4.dp))
                CompletedSectionHeader(
                    count = completedTodos.size,
                    expanded = completedExpanded,
                    onClick = { completedExpanded = !completedExpanded }
                )
            }
            if (completedExpanded) {
                if (!selectionMode) {
                    items(completedTodos, key = { "c_${it.id}" }) { todo ->
                        SwipeToDeleteTodoItem(
                            todo = todo,
                            hasAnyRevealed = openedTodoId != null,
                            isRevealed = openedTodoId == todo.id,
                            onRevealedChange = { revealed -> onOpenedTodoChange(if (revealed) todo.id else null) },
                            onCloseRequested = { onOpenedTodoChange(null) },
                            onToggle = { onToggle(todo) },
                            onDelete = { onDelete(todo) },
                            onEdit = { onEdit(todo) },
                            onLongPress = { onEnterSelection(todo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableTodoSelectionContent(
    activeTodos: List<Todo>,
    completedTodos: List<Todo>,
    selectedTodoIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onReorder: (List<Todo>) -> Unit
) {
    val todos = remember(activeTodos, completedTodos) { activeTodos + completedTodos }
    val localTodos = remember { mutableStateListOf<Todo>() }
    var activeCount by remember { mutableIntStateOf(activeTodos.size) }
    var isDragging by remember { mutableStateOf(false) }
    var draggingTodoId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var pendingReorderedIds by remember { mutableStateOf<List<Long>?>(null) }
    var pendingActiveCount by remember { mutableStateOf<Int?>(null) }
    val itemHeights = remember { mutableStateMapOf<Long, Float>() }
    val swapOffsets = remember { mutableStateMapOf<Long, Float>() }
    val swapVersions = remember { mutableStateMapOf<Long, Int>() }
    val defaultItemStepPx = with(LocalDensity.current) { 86.dp.toPx() }
    val completedGapPx = with(LocalDensity.current) { 56.dp.toPx() }

    LaunchedEffect(todos, activeTodos.size, isDragging, pendingReorderedIds, pendingActiveCount) {
        if (isDragging) return@LaunchedEffect

        val currentOrderIds = todos.map { it.id }
        val pendingIds = pendingReorderedIds
        if (pendingIds != null && (currentOrderIds != pendingIds || activeTodos.size != pendingActiveCount)) {
            return@LaunchedEffect
        }

        pendingReorderedIds = null
        pendingActiveCount = null
        localTodos.clear()
        localTodos.addAll(todos)
        activeCount = activeTodos.size
    }

    fun recordSwap(todoId: Long, startOffset: Float) {
        swapOffsets[todoId] = startOffset
        swapVersions[todoId] = (swapVersions[todoId] ?: 0) + 1
    }

    fun itemStep(todoId: Long): Float = itemHeights[todoId] ?: defaultItemStepPx

    fun handleDragStart(todo: Todo) {
        isDragging = true
        draggingTodoId = todo.id
        dragOffsetY = 0f
    }

    fun handleDrag(todo: Todo, deltaY: Float) {
        if (draggingTodoId != todo.id) return

        dragOffsetY += deltaY
        var currentIndex = localTodos.indexOfFirst { it.id == todo.id }

        while (currentIndex < localTodos.lastIndex) {
            val crossingBoundary = currentIndex == activeCount - 1 && activeCount in 1..<localTodos.size
            val moveThreshold = itemStep(todo.id) / 2f + if (crossingBoundary) completedGapPx else 0f
            if (dragOffsetY < moveThreshold) break
            val movedTodo = localTodos[currentIndex + 1]
            val movedOffset = itemStep(movedTodo.id) + if (crossingBoundary) completedGapPx else 0f
            localTodos.move(currentIndex, currentIndex + 1)
            dragOffsetY -= movedOffset
            recordSwap(movedTodo.id, movedOffset)
            if (crossingBoundary) {
                activeCount = (activeCount - 1).coerceAtLeast(0)
            }
            currentIndex++
        }

        while (currentIndex > 0) {
            val crossingBoundary = currentIndex == activeCount && activeCount in 1..localTodos.lastIndex
            val moveThreshold = itemStep(todo.id) / 2f + if (crossingBoundary) completedGapPx else 0f
            if (dragOffsetY > -moveThreshold) break
            val movedTodo = localTodos[currentIndex - 1]
            val movedOffset = itemStep(movedTodo.id) + if (crossingBoundary) completedGapPx else 0f
            localTodos.move(currentIndex, currentIndex - 1)
            dragOffsetY += movedOffset
            recordSwap(movedTodo.id, -movedOffset)
            if (crossingBoundary) {
                activeCount = (activeCount + 1).coerceAtMost(localTodos.size)
            }
            currentIndex--
        }
    }

    fun handleDragEnd() {
        val now = System.currentTimeMillis()
        val reordered = localTodos.mapIndexed { index, todo ->
            val targetStatus = if (index < activeCount) TodoStatus.ACTIVE else TodoStatus.COMPLETED
            todo.copy(
                status = targetStatus,
                completedAt = if (targetStatus == TodoStatus.COMPLETED) todo.completedAt ?: now else null
            )
        }
        pendingReorderedIds = reordered.map { it.id }
        pendingActiveCount = activeCount
        isDragging = false
        draggingTodoId = null
        dragOffsetY = 0f
        onReorder(reordered)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        localTodos.forEachIndexed { index, todo ->
            key(todo.id) {
                if (index == activeCount && localTodos.size > activeCount) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CompletedSectionHeader(
                        count = localTodos.size - activeCount,
                        expanded = true,
                        onClick = {}
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (draggingTodoId == todo.id) 20f else 0f)
                ) {
                    ReorderableTodoRow(
                        todo = todo,
                        isSelected = todo.id in selectedTodoIds,
                        isDragging = draggingTodoId == todo.id,
                        dragOffsetY = if (draggingTodoId == todo.id) dragOffsetY else 0f,
                        swapOffset = swapOffsets[todo.id] ?: 0f,
                        swapVersion = swapVersions[todo.id] ?: 0,
                        onHeightChanged = { height -> itemHeights[todo.id] = height },
                        onToggleSelection = { onToggleSelection(todo.id) },
                        onLongDragStart = { handleDragStart(todo) },
                        onLongDrag = { deltaY -> handleDrag(todo, deltaY) },
                        onLongDragEnd = ::handleDragEnd
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已完成",
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${count}条",
            fontSize = 13.sp,
            color = Color(0xFFBDBDBD)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SwipeToDeleteTodoItem(
    todo: Todo,
    hasAnyRevealed: Boolean,
    isRevealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onCloseRequested: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit
) {
    SwipeRevealDeleteItem(
        onDelete = onDelete,
        actionWidth = 88.dp,
        contentEndGap = 0.dp,
        actionPaddingStart = 0.dp,
        actionPaddingEnd = 0.dp,
        animateDeleteFromRight = true,
        isRevealed = isRevealed,
        onRevealedChange = onRevealedChange,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isRevealed) 6f else 0f)
    ) {
        TodoCard(
            todo = todo,
            selectionMode = false,
            isSelected = false,
            onToggleComplete = onToggle,
            onClick = {
                if (hasAnyRevealed) {
                    onCloseRequested()
                } else {
                    onEdit()
                }
            },
            onLongPress = onLongPress
        )
    }
}

@Composable
private fun ReorderableTodoRow(
    todo: Todo,
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
        label = "todo_drag_scale"
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

    TodoCard(
        todo = todo,
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
private fun TodoCard(
    todo: Todo,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onHeightChanged: ((Float) -> Unit)? = null,
    onLongDragStart: (() -> Unit)? = null,
    onLongDrag: ((Float) -> Unit)? = null,
    onLongDragEnd: (() -> Unit)? = null
) {
    val isCompleted = todo.status == TodoStatus.COMPLETED
    val bgColor = if (isCompleted) Color(0xFFF0F0F0) else Color(todo.cardColor)
    val now = System.currentTimeMillis()
    val reminderTime = todo.reminderTime
    val isExpired = reminderTime != null && reminderTime < now && !isCompleted

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
                    Modifier.pointerInput(todo.id) {
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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    if (!selectionMode) {
                        onLongPress?.invoke()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                DragHandle(
                    onLongDragStart = onLongDragStart,
                    onLongDrag = onLongDrag,
                    onLongDragEnd = onLongDragEnd
                )
                Spacer(modifier = Modifier.width(14.dp))
            } else {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (reminderTime != null) 48.dp else 40.dp)
                        .background(
                            color = if (isCompleted) Color(0xFFBDBDBD)
                            else if (todo.isPriority) NoteRed
                            else Color(0xFFFFB74D),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCompleted) Color(0xFFBDBDBD) else Color(0xFF212121),
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (reminderTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatReminderDisplay(reminderTime, isExpired),
                        fontSize = 12.sp,
                        color = if (isExpired) NoteRed else Color(0xFF9E9E9E)
                    )
                }
            }

            if (todo.isPriority && !isCompleted) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = "高优先级",
                    tint = NoteRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (selectionMode) {
                SelectionBox(
                    selected = isSelected,
                    onClick = onClick
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .then(
                            if (isCompleted) {
                                Modifier.background(Color(0xFFBDBDBD), CircleShape)
                            } else {
                                Modifier.border(1.5.dp, Color(0xFFBDBDBD), CircleShape)
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleComplete
                        )
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已完成",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = "拖动排序",
        tint = Color(0xFFD2CCBC),
        modifier = Modifier
            .size(24.dp)
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) {
                    Modifier.background(Color(0xFF1976D2), RoundedCornerShape(8.dp))
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
private fun TodoEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_todo_empty),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = EmptyIconColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "目前没有待办哦~\n点击下方\"+\"号来添加吧!",
            color = EmptyTextColor,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatReminderDisplay(reminderTime: Long, isExpired: Boolean): String {
    val reminderCal = Calendar.getInstance().apply { timeInMillis = reminderTime }
    val nowCal = Calendar.getInstance()

    val isToday = reminderCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            reminderCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = reminderCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            reminderCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFmt.format(Date(reminderTime))

    return when {
        isToday && isExpired -> "今天 $timeStr 已过期"
        isToday -> "今天 $timeStr"
        isTomorrow -> "明天 $timeStr"
        else -> {
            val dateFmt = SimpleDateFormat("M/dd HH:mm", Locale.getDefault())
            dateFmt.format(Date(reminderTime))
        }
    }
}

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    add(toIndex, removeAt(fromIndex))
}
