package com.swu.bianwanlu2_0.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.presentation.components.AddCategoryDialog
import com.swu.bianwanlu2_0.presentation.components.AppDrawerContent
import com.swu.bianwanlu2_0.presentation.components.CategoryDropdown
import com.swu.bianwanlu2_0.presentation.components.ReminderDialog
import com.swu.bianwanlu2_0.presentation.screens.calendar.CalendarScreen
import com.swu.bianwanlu2_0.presentation.screens.category.CategoryManageScreen
import com.swu.bianwanlu2_0.presentation.screens.category.CategoryViewModel
import com.swu.bianwanlu2_0.presentation.screens.notes.AddNoteScreen
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteListScreen
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteViewModel
import com.swu.bianwanlu2_0.presentation.screens.timeline.TimelineScreen
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.presentation.screens.todo.AddTodoScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoListScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoViewModel
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.NavUnselected
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    var currentDestination by remember {
        mutableStateOf<AppDestination>(AppDestination.Notes)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val noteViewModel: NoteViewModel = hiltViewModel()
    val todoViewModel: TodoViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()

    val noteCount by noteViewModel.noteCount.collectAsStateWithLifecycle()
    val todoCount by todoViewModel.todoCount.collectAsStateWithLifecycle()

    val noteCategoryName by noteViewModel.selectedCategoryName.collectAsStateWithLifecycle()
    val todoCategoryName by todoViewModel.selectedCategoryName.collectAsStateWithLifecycle()

    val noteCategories by noteViewModel.categories.collectAsStateWithLifecycle()
    val todoCategories by todoViewModel.categories.collectAsStateWithLifecycle()

    val noteSelectedCategory by noteViewModel.selectedCategory.collectAsStateWithLifecycle()
    val todoSelectedCategory by todoViewModel.selectedCategory.collectAsStateWithLifecycle()
    val todoSelectionMode by todoViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTodoIds by todoViewModel.selectedTodoIds.collectAsStateWithLifecycle()
    val selectedTodos by todoViewModel.selectedTodos.collectAsStateWithLifecycle()
    val filteredTodos by todoViewModel.filteredTodos.collectAsStateWithLifecycle()

    val title = when (currentDestination) {
        AppDestination.Notes -> noteCategoryName
        AppDestination.Todo -> todoCategoryName
        else -> currentDestination.label
    }
    val itemCount = when (currentDestination) {
        AppDestination.Notes -> noteCount
        AppDestination.Todo -> todoCount
        else -> -1
    }
    val hasCategoryDropdown =
        currentDestination == AppDestination.Notes || currentDestination == AppDestination.Todo
    val categories: List<Category> = when (currentDestination) {
        AppDestination.Notes -> noteCategories
        AppDestination.Todo -> todoCategories
        else -> emptyList()
    }
    val selectedCategory: Category? = when (currentDestination) {
        AppDestination.Notes -> noteSelectedCategory
        AppDestination.Todo -> todoSelectedCategory
        else -> null
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showAddTodo by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var showBatchReminderDialog by remember { mutableStateOf(false) }
    var showDeleteTodosConfirm by remember { mutableStateOf(false) }

    val allFilteredTodosSelected =
        filteredTodos.isNotEmpty() && filteredTodos.all { it.id in selectedTodoIds }

    // 新建笔记全屏页面
    if (showAddNote) {
        AddNoteScreen(
            onCancel = { showAddNote = false },
            onConfirm = { noteTitle, noteContent, reminderTime, isPriority, cardColor, textColor, imageUris ->
                noteViewModel.addNote(
                    title = noteTitle,
                    content = noteContent,
                    reminderTime = reminderTime,
                    isPriority = isPriority,
                    cardColor = cardColor,
                    textColor = textColor,
                    imageUris = imageUris
                )
                showAddNote = false
            }
        )
        return
    }

    // 编辑笔记全屏页面
    if (editingNote != null) {
        val note = editingNote!!
        AddNoteScreen(
            existingNote = note,
            onCancel = { editingNote = null },
            onConfirm = { noteTitle, noteContent, reminderTime, isPriority, cardColor, textColor, imageUris ->
                noteViewModel.updateNote(
                    existing = note,
                    title = noteTitle,
                    content = noteContent,
                    reminderTime = reminderTime,
                    isPriority = isPriority,
                    cardColor = cardColor,
                    textColor = textColor,
                    imageUris = imageUris
                )
                editingNote = null
            }
        )
        return
    }

    // 新建待办全屏页面
    if (showAddTodo) {
        AddTodoScreen(
            onCancel = { showAddTodo = false },
            onConfirm = { todoTitle, reminderTime, isPriority, cardColor ->
                todoViewModel.addTodo(todoTitle, reminderTime, isPriority, cardColor)
                showAddTodo = false
            }
        )
        return
    }

    // 编辑待办全屏页面
    if (editingTodo != null) {
        val todo = editingTodo!!
        AddTodoScreen(
            existingTodo = todo,
            onCancel = { editingTodo = null },
            onConfirm = { todoTitle, reminderTime, isPriority, cardColor ->
                todoViewModel.updateTodo(todo, todoTitle, reminderTime, isPriority, cardColor)
                editingTodo = null
            }
        )
        return
    }

    if (showBatchReminderDialog) {
        ReminderDialog(
            onDismiss = { showBatchReminderDialog = false },
            onSelect = { time ->
                todoViewModel.updateReminderForSelectedTodos(time)
                showBatchReminderDialog = false
            },
            showClearAction = selectedTodos.any { it.reminderTime != null },
            onClear = {
                todoViewModel.updateReminderForSelectedTodos(null)
                showBatchReminderDialog = false
            }
        )
    }

    if (showDeleteTodosConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteTodosConfirm = false },
            title = { Text("删除待办") },
            text = { Text("确定删除已选择的待办吗？", color = Color(0xFF616161)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        todoViewModel.deleteSelectedTodos()
                        showDeleteTodosConfirm = false
                    }
                ) {
                    Text("删除", color = Color(0xFFE65E4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTodosConfirm = false }) {
                    Text("取消", color = Color(0xFF212121))
                }
            }
        )
    }

    // 分类管理全屏页面
    if (showCategoryManage) {
        CategoryManageScreen(
            viewModel = categoryViewModel,
            onBack = { showCategoryManage = false }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                noteCategories = noteCategories,
                todoCategories = todoCategories,
                selectedCategory = selectedCategory,
                onCategorySelect = { cat ->
                    when (cat.type) {
                        CategoryType.NOTE -> {
                            currentDestination = AppDestination.Notes
                            noteViewModel.selectCategory(cat)
                        }
                        CategoryType.TODO -> {
                            currentDestination = AppDestination.Todo
                            todoViewModel.selectCategory(cat)
                        }
                    }
                    scope.launch { drawerState.close() }
                },
                onMyClick = { scope.launch { drawerState.close() } },
                onSyncClick = { scope.launch { drawerState.close() } },
                onGameClick = { scope.launch { drawerState.close() } }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                if (currentDestination == AppDestination.Todo && todoSelectionMode) {
                    TodoSelectionTopBar(
                        selectedCount = selectedTodoIds.size,
                        allSelected = allFilteredTodosSelected,
                        onCancel = { todoViewModel.clearSelection() },
                        onSelectAllToggle = {
                            if (allFilteredTodosSelected) {
                                todoViewModel.clearSelection()
                            } else {
                                todoViewModel.selectAllFilteredTodos()
                            }
                        }
                    )
                } else {
                    MainTopBar(
                        title = title,
                        itemCount = itemCount,
                        showArrow = hasCategoryDropdown,
                        isDropdownOpen = showCategoryDropdown,
                        onTitleClick = {
                            if (hasCategoryDropdown) showCategoryDropdown = !showCategoryDropdown
                        },
                        onAvatarClick = { scope.launch { drawerState.open() } },
                        onSearchClick = {},
                        onMenuClick = {}
                    )
                }
            },
            bottomBar = {
                if (currentDestination == AppDestination.Todo && todoSelectionMode) {
                    TodoSelectionBottomBar(
                        onPriorityClick = { todoViewModel.applyPriorityToSelectedTodos() },
                        onReminderClick = { showBatchReminderDialog = true },
                        onDeleteClick = { showDeleteTodosConfirm = true }
                    )
                } else {
                    BottomNavBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = {
                            if (currentDestination == AppDestination.Todo && it != currentDestination) {
                                todoViewModel.clearSelection()
                            }
                            if (it != currentDestination) showCategoryDropdown = false
                            currentDestination = it
                        }
                    )
                }
            },
            containerColor = Color.White
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxSize(),
                    label = "screen_transition"
                ) { destination ->
                    when (destination) {
                        AppDestination.Notes -> NoteListScreen(
                            viewModel = noteViewModel,
                            onAddNote = { showAddNote = true },
                            onEditNote = { editingNote = it }
                        )
                        AppDestination.Todo -> TodoListScreen(
                            viewModel = todoViewModel,
                            onAddTodo = { showAddTodo = true },
                            onEditTodo = { editingTodo = it }
                        )
                        AppDestination.Timeline -> TimelineScreen()
                        AppDestination.Calendar -> CalendarScreen()
                    }
                }

                if (hasCategoryDropdown && showCategoryDropdown && !(currentDestination == AppDestination.Todo && todoSelectionMode)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showCategoryDropdown = false }
                            )
                            .zIndex(5f)
                    )
                }

                if (hasCategoryDropdown && !(currentDestination == AppDestination.Todo && todoSelectionMode)) {
                    CategoryDropdown(
                        visible = showCategoryDropdown,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelect = { cat ->
                            when (currentDestination) {
                                AppDestination.Notes -> noteViewModel.selectCategory(cat)
                                AppDestination.Todo -> todoViewModel.selectCategory(cat)
                                else -> {}
                            }
                        },
                        onDismiss = { showCategoryDropdown = false },
                        onAddCategory = { showAddCategoryDialog = true },
                        onManageCategory = { showCategoryManage = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(10f)
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        val dialogDefaultType = when (currentDestination) {
            AppDestination.Notes -> CategoryType.NOTE
            AppDestination.Todo -> CategoryType.TODO
            else -> CategoryType.NOTE
        }
        AddCategoryDialog(
            defaultType = dialogDefaultType,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, type ->
                categoryViewModel.addCategory(name, type)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun MainTopBar(
    title: String,
    itemCount: Int,
    showArrow: Boolean,
    isDropdownOpen: Boolean,
    onTitleClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 4.dp)
    ) {
        AvatarButton(
            onClick = onAvatarClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTitleClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF212121)
                    )
                    if (showArrow) {
                        Icon(
                            imageVector = if (isDropdownOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "展开分类",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = if (itemCount >= 0) "共${itemCount}条" else "共···条",
                    fontSize = 11.sp,
                    color = NavUnselected,
                    lineHeight = 14.sp
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, "搜索", tint = Color(0xFF424242))
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.MoreVert, "更多", tint = Color(0xFF424242))
            }
        }
    }
}

@Composable
private fun AvatarButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = "用户头像",
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TodoSelectionTopBar(
    selectedCount: Int,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onSelectAllToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(text = "取消", fontSize = 18.sp, color = Color(0xFF1976D2))
        }
        Text(
            text = "已选择 ${selectedCount} 项",
            modifier = Modifier.align(Alignment.Center),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 18.sp,
            color = Color(0xFF212121),
            fontWeight = FontWeight.Medium
        )
        TextButton(
            onClick = onSelectAllToggle,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(text = if (allSelected) "取消全选" else "全选", fontSize = 18.sp, color = Color(0xFF1976D2))
        }
    }
}

@Composable
private fun TodoSelectionBottomBar(
    onPriorityClick: () -> Unit,
    onReminderClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TodoSelectionAction(
            icon = Icons.Outlined.Flag,
            label = "优先级",
            onClick = onPriorityClick
        )
        TodoSelectionAction(
            icon = Icons.Outlined.NotificationsNone,
            label = "提醒",
            onClick = onReminderClick
        )
        TodoSelectionAction(
            icon = Icons.Outlined.Delete,
            label = "删除",
            onClick = onDeleteClick
        )
    }
}

@Composable
private fun TodoSelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF212121),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF212121),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainTopBarPreview() {
    Bianwanlu2_0Theme {
        MainTopBar(
            title = "笔记", itemCount = 3,
            showArrow = true, isDropdownOpen = false,
            onTitleClick = {}, onAvatarClick = {},
            onSearchClick = {}, onMenuClick = {}
        )
    }
}
