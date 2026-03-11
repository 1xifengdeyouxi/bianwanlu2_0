package com.swu.bianwanlu2_0.presentation.screens.category

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.presentation.components.AddCategoryDialog
import com.swu.bianwanlu2_0.presentation.components.SwipeActionItem
import com.swu.bianwanlu2_0.presentation.components.SwipeRevealActionsItem

private val modifyActionColor = Color(0xFF5A9BEA)
private val clearActionColor = Color(0xFFBDBCC4)
private val deleteActionColor = Color(0xFFE65E4F)

private data class DeleteCategoryRequest(
    val category: Category,
    val fallbackCategory: Category
)

@Composable
fun CategoryManageScreen(
    viewModel: CategoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noteCategories by viewModel.noteCategories.collectAsStateWithLifecycle()
    val todoCategories by viewModel.todoCategories.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogDefaultType by remember { mutableStateOf(CategoryType.NOTE) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var clearingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<DeleteCategoryRequest?>(null) }
    var cannotDeleteMessage by remember { mutableStateOf<String?>(null) }
    var noteExpanded by remember { mutableStateOf(true) }
    var todoExpanded by remember { mutableStateOf(true) }
    var openedCategoryId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ManageTopBar(
                onBack = onBack,
                onAdd = {
                    addDialogDefaultType = CategoryType.NOTE
                    showAddDialog = true
                }
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item(key = "note_header") {
                    SectionHeader(
                        title = "笔记",
                        expanded = noteExpanded,
                        onClick = { noteExpanded = !noteExpanded }
                    )
                }
                if (noteExpanded) {
                    item(key = "note_section") {
                        CategoryManageSection(
                            categories = noteCategories,
                            type = CategoryType.NOTE,
                            viewModel = viewModel,
                            icon = Icons.Outlined.BookmarkBorder,
                            hasAnyRevealed = openedCategoryId != null,
                            openedCategoryId = openedCategoryId,
                            onOpenedCategoryChange = { openedCategoryId = it },
                            onEdit = { editingCategory = it },
                            onClear = { clearingCategory = it },
                            onDelete = { category, fallbackCategory ->
                                if (fallbackCategory == null) {
                                    cannotDeleteMessage = "笔记分类至少保留一个，最后一个分类不能删除。"
                                } else {
                                    deletingCategory = DeleteCategoryRequest(category, fallbackCategory)
                                }
                            }
                        )
                    }
                }

                item(key = "todo_header") {
                    SectionHeader(
                        title = "待办",
                        expanded = todoExpanded,
                        onClick = { todoExpanded = !todoExpanded }
                    )
                }
                if (todoExpanded) {
                    item(key = "todo_section") {
                        CategoryManageSection(
                            categories = todoCategories,
                            type = CategoryType.TODO,
                            viewModel = viewModel,
                            icon = Icons.Outlined.CheckBox,
                            hasAnyRevealed = openedCategoryId != null,
                            openedCategoryId = openedCategoryId,
                            onOpenedCategoryChange = { openedCategoryId = it },
                            onEdit = { editingCategory = it },
                            onClear = { clearingCategory = it },
                            onDelete = { category, fallbackCategory ->
                                if (fallbackCategory == null) {
                                    cannotDeleteMessage = "待办分类至少保留一个，最后一个分类不能删除。"
                                } else {
                                    deletingCategory = DeleteCategoryRequest(category, fallbackCategory)
                                }
                            }
                        )
                }
            }
        }

        if (openedCategoryId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { openedCategoryId = null }
                    .zIndex(5f)
            )
        }
    }

    }

    if (showAddDialog) {
        AddCategoryDialog(
            defaultType = addDialogDefaultType,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                viewModel.addCategory(name, type)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        AddCategoryDialog(
            defaultType = category.type,
            initialName = category.name,
            titleText = "修改分类",
            confirmText = "保存",
            typeEnabled = false,
            onDismiss = { editingCategory = null },
            onConfirm = { name, _ ->
                viewModel.updateCategory(category, name)
                editingCategory = null
            }
        )
    }

    clearingCategory?.let { category ->
        val itemLabel = when (category.type) {
            CategoryType.NOTE -> "笔记"
            CategoryType.TODO -> "待办"
        }
        ConfirmDialog(
            title = "清空分类内容",
            message = "确定清空“${category.name}”下的所有${itemLabel}吗？此操作不可恢复。",
            confirmText = "清空",
            confirmColor = clearActionColor,
            onDismiss = { clearingCategory = null },
            onConfirm = {
                viewModel.clearCategoryItems(category)
                clearingCategory = null
            }
        )
    }

    deletingCategory?.let { request ->
        val itemLabel = when (request.category.type) {
            CategoryType.NOTE -> "笔记"
            CategoryType.TODO -> "待办"
        }
        ConfirmDialog(
            title = "删除分类",
            message = "确定删除“${request.category.name}”吗？该分类下的${itemLabel}会移动到“${request.fallbackCategory.name}”。",
            confirmText = "删除",
            confirmColor = deleteActionColor,
            onDismiss = { deletingCategory = null },
            onConfirm = {
                viewModel.deleteCategory(request.category, request.fallbackCategory)
                deletingCategory = null
            }
        )
    }

    cannotDeleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { cannotDeleteMessage = null },
            title = { Text("无法删除") },
            text = { Text(message, color = Color(0xFF616161)) },
            confirmButton = {
                TextButton(onClick = { cannotDeleteMessage = null }) {
                    Text("知道了", color = Color(0xFF212121))
                }
            }
        )
    }
}

@Composable
private fun CategoryManageSection(
    categories: List<Category>,
    type: CategoryType,
    viewModel: CategoryViewModel,
    icon: ImageVector,
    hasAnyRevealed: Boolean,
    openedCategoryId: Long?,
    onOpenedCategoryChange: (Long?) -> Unit,
    onEdit: (Category) -> Unit,
    onClear: (Category) -> Unit,
    onDelete: (Category, Category?) -> Unit
) {
    val localCategories = remember { mutableStateListOf<Category>() }
    var isDragging by remember { mutableStateOf(false) }
    var draggingCategoryId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val swapOffsets = remember { mutableStateMapOf<Long, Float>() }
    val swapVersions = remember { mutableStateMapOf<Long, Int>() }
    val itemStepPx = with(LocalDensity.current) { 88.dp.toPx() }

    LaunchedEffect(categories, isDragging) {
        if (!isDragging) {
            localCategories.clear()
            localCategories.addAll(categories)
        }
    }

    fun recordSwap(categoryId: Long, startOffset: Float) {
        swapOffsets[categoryId] = startOffset
        swapVersions[categoryId] = (swapVersions[categoryId] ?: 0) + 1
    }

    fun handleDragStart(category: Category) {
        isDragging = true
        draggingCategoryId = category.id
        dragOffsetY = 0f
        onOpenedCategoryChange(null)
    }

    fun handleDrag(category: Category, deltaY: Float) {
        if (draggingCategoryId != category.id) return

        dragOffsetY += deltaY
        var currentIndex = localCategories.indexOfFirst { it.id == category.id }

        while (dragOffsetY >= itemStepPx / 2f && currentIndex < localCategories.lastIndex) {
            val movedCategory = localCategories[currentIndex + 1]
            localCategories.move(currentIndex, currentIndex + 1)
            dragOffsetY -= itemStepPx
            recordSwap(movedCategory.id, -itemStepPx)
            currentIndex++
        }

        while (dragOffsetY <= -itemStepPx / 2f && currentIndex > 0) {
            val movedCategory = localCategories[currentIndex - 1]
            localCategories.move(currentIndex, currentIndex - 1)
            dragOffsetY += itemStepPx
            recordSwap(movedCategory.id, itemStepPx)
            currentIndex--
        }
    }

    fun handleDragEnd() {
        val reordered = localCategories.toList()
        isDragging = false
        draggingCategoryId = null
        dragOffsetY = 0f
        viewModel.reorderCategories(type, reordered)
    }

    Column {
        localCategories.forEach { category ->
            key(category.id) {
                CategoryManageRow(
                    category = category,
                    viewModel = viewModel,
                    icon = icon,
                    hasAnyRevealed = hasAnyRevealed,
                    isRevealed = openedCategoryId == category.id,
                    onRevealedChange = { revealed ->
                        onOpenedCategoryChange(if (revealed) category.id else null)
                    },
                    isDragging = draggingCategoryId == category.id,
                    dragOffsetY = if (draggingCategoryId == category.id) dragOffsetY else 0f,
                    swapOffset = swapOffsets[category.id] ?: 0f,
                    swapVersion = swapVersions[category.id] ?: 0,
                    onEdit = { onEdit(category) },
                    onClear = { onClear(category) },
                    onDelete = {
                        onDelete(category, localCategories.firstOrNull { it.id != category.id })
                    },
                    onLongDragStart = { handleDragStart(category) },
                    onLongDrag = { deltaY -> handleDrag(category, deltaY) },
                    onLongDragEnd = ::handleDragEnd
                )
                HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun ManageTopBar(
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF212121)
            )
        }
        Text(
            text = "分类管理",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121),
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "新增分类",
                tint = Color(0xFF212121)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起" else "展开",
            tint = Color(0xFF757575),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CategoryManageRow(
    category: Category,
    viewModel: CategoryViewModel,
    icon: ImageVector,
    hasAnyRevealed: Boolean,
    isRevealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    isDragging: Boolean,
    dragOffsetY: Float,
    swapOffset: Float,
    swapVersion: Int,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onLongDragStart: () -> Unit,
    onLongDrag: (Float) -> Unit,
    onLongDragEnd: () -> Unit
) {
    val count by viewModel.countItemsInCategory(category).collectAsStateWithLifecycle()
    val typeLabel = when (category.type) {
        CategoryType.NOTE -> "便签"
        CategoryType.TODO -> "待办"
    }
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "category_drag_scale"
    )
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isDragging) Color(0xFFF3F3F3) else Color.White,
        animationSpec = tween(durationMillis = 180),
        label = "category_drag_color"
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

    val translationY = if (isDragging) dragOffsetY else swapAnimatable.value
    val swipeActions = if (isDragging) {
        emptyList()
    } else {
        listOf(
            SwipeActionItem(label = "修改", backgroundColor = modifyActionColor, onClick = onEdit),
            SwipeActionItem(label = "清除", backgroundColor = clearActionColor, onClick = onClear),
            SwipeActionItem(label = "删除", backgroundColor = deleteActionColor, onClick = onDelete)
        )
    }

    SwipeRevealActionsItem(
        actions = swipeActions,
        actionWidth = 56.dp,
        isRevealed = isRevealed && !isDragging,
        onRevealedChange = onRevealedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .graphicsLayer { this.translationY = translationY }
                .scale(scale)
                .zIndex(if (isDragging) 8f else if (isRevealed) 6f else 0f)
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        if (hasAnyRevealed) {
                            onRevealedChange(false)
                        }
                    }
                ),
            color = containerColor,
            shadowElevation = if (isDragging) 10.dp else 0.dp,
            tonalElevation = if (isDragging) 1.dp else 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (category.type == CategoryType.TODO) Color(0xFF42A5F5) else Color(0xFF757575),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "${count}条${typeLabel}",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
                ReorderHandle(
                    onLongDragStart = onLongDragStart,
                    onLongDrag = onLongDrag,
                    onLongDragEnd = onLongDragEnd
                )
            }
        }
    }
}

@Composable
private fun ReorderHandle(
    onLongDragStart: () -> Unit,
    onLongDrag: (Float) -> Unit,
    onLongDragEnd: () -> Unit
) {
    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = "拖动排序",
        tint = Color(0xFFBDBDBD),
        modifier = Modifier
            .size(22.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onLongDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onLongDrag(dragAmount.y)
                    },
                    onDragEnd = onLongDragEnd,
                    onDragCancel = onLongDragEnd
                )
            }
            .padding(1.dp)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = Color(0xFF616161)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF212121))
            }
        }
    )
}

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    add(toIndex, removeAt(fromIndex))
}
