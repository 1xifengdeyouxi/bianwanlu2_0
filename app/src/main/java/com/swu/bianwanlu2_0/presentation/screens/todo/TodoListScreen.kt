package com.swu.bianwanlu2_0.presentation.screens.todo

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.ui.theme.EmptyIconColor
import com.swu.bianwanlu2_0.ui.theme.EmptyTextColor
import com.swu.bianwanlu2_0.ui.theme.NoteRed

@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    onAddTodo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTodos by viewModel.activeTodos.collectAsStateWithLifecycle()
    val completedTodos by viewModel.completedTodos.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterTabRow(
                currentFilter = currentFilter,
                onFilterSelected = viewModel::setFilter
            )

            if (activeTodos.isEmpty() && completedTodos.isEmpty()) {
                TodoEmptyState(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth())
            } else {
                TodoContent(
                    activeTodos = activeTodos,
                    completedTodos = completedTodos,
                    onToggle = viewModel::toggleComplete,
                    onDelete = viewModel::deleteTodo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

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
            .background(
                if (isSelected) Color(0xFFF5F5F5) else Color.Transparent
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
private fun TodoContent(
    activeTodos: List<Todo>,
    completedTodos: List<Todo>,
    onToggle: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
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
        // 进行中的待办
        items(activeTodos, key = { "a_${it.id}" }) { todo ->
            SwipeToDeleteTodoItem(
                todo = todo,
                onToggle = { onToggle(todo) },
                onDelete = { onDelete(todo) }
            )
        }

        // 已完成分割栏
        if (completedTodos.isNotEmpty()) {
            item(key = "completed_header") {
                Spacer(modifier = Modifier.height(4.dp))
                CompletedSectionHeader(
                    count = completedTodos.size,
                    expanded = completedExpanded,
                    onClick = { completedExpanded = !completedExpanded }
                )
            }

            if (completedExpanded) {
                items(completedTodos, key = { "c_${it.id}" }) { todo ->
                    SwipeToDeleteTodoItem(
                        todo = todo,
                        onToggle = { onToggle(todo) },
                        onDelete = { onDelete(todo) }
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
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    NoteRed else Color.Transparent,
                label = "bg_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        TodoCard(todo = todo, onToggle = onToggle)
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = todo.status == TodoStatus.COMPLETED
    val bgColor = if (isCompleted) Color(0xFFF0F0F0) else Color(todo.cardColor)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧颜色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        color = if (isCompleted) Color(0xFFBDBDBD)
                        else if (todo.isPriority) NoteRed
                        else Color(0xFFFFB74D),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 标题+描述
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
            }

            // 优先级标记
            if (todo.isPriority && !isCompleted) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = "高优先级",
                    tint = NoteRed,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 右侧圆形勾选
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
                        onClick = onToggle
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
