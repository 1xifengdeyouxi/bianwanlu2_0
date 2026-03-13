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
import androidx.compose.material3.MaterialTheme
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
import com.swu.bianwanlu2_0.presentation.screens.profile.AboutBianwanluScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.AccountViewModel
import com.swu.bianwanlu2_0.presentation.screens.profile.AuthMode
import com.swu.bianwanlu2_0.presentation.screens.profile.AuthScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.DataAndSyncScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.GeneralSettingsScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.MyMenuAction
import com.swu.bianwanlu2_0.presentation.screens.profile.MyScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.ProfileAvatarImage
import com.swu.bianwanlu2_0.presentation.screens.profile.ProfileInfoScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.ReminderSettingsScreen
import com.swu.bianwanlu2_0.presentation.screens.timeline.TimelineScreen
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.presentation.screens.todo.AddTodoScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoListScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoViewModel
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint
import com.swu.bianwanlu2_0.ui.theme.NavUnselected
import kotlinx.coroutines.launch

private enum class MyPageDestination {
    Root,
    Profile,
    Auth,
    CategoryManage,
    ReminderSettings,
    DataAndSync,
    GeneralSettings,
    About,
}

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
    val accountViewModel: AccountViewModel = hiltViewModel()

    val noteCount by noteViewModel.noteCount.collectAsStateWithLifecycle()
    val todoCount by todoViewModel.todoCount.collectAsStateWithLifecycle()

    val noteCategoryName by noteViewModel.selectedCategoryName.collectAsStateWithLifecycle()
    val todoCategoryName by todoViewModel.selectedCategoryName.collectAsStateWithLifecycle()

    val noteCategories by noteViewModel.categories.collectAsStateWithLifecycle()
    val todoCategories by todoViewModel.categories.collectAsStateWithLifecycle()

    val noteSelectedCategory by noteViewModel.selectedCategory.collectAsStateWithLifecycle()
    val todoSelectedCategory by todoViewModel.selectedCategory.collectAsStateWithLifecycle()
    val noteSelectionMode by noteViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedNoteIds by noteViewModel.selectedNoteIds.collectAsStateWithLifecycle()
    val selectedNotes by noteViewModel.selectedNotes.collectAsStateWithLifecycle()
    val filteredNotes by noteViewModel.filteredNotes.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
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
    var showMyPage by remember { mutableStateOf(false) }
    var myPageDestination by remember { mutableStateOf(MyPageDestination.Root) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showAddTodo by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var showBatchNoteReminderDialog by remember { mutableStateOf(false) }
    var showDeleteNotesConfirm by remember { mutableStateOf(false) }
    var showBatchTodoReminderDialog by remember { mutableStateOf(false) }
    var showDeleteTodosConfirm by remember { mutableStateOf(false) }

    val allFilteredNotesSelected =
        filteredNotes.isNotEmpty() && filteredNotes.all { it.id in selectedNoteIds }
    val allFilteredTodosSelected =
        filteredTodos.isNotEmpty() && filteredTodos.all { it.id in selectedTodoIds }

    if (!accountState.hasSeenAuthChoice) {
        AuthScreen(
            initialMode = if (accountState.hasLocalAccount) AuthMode.Login else AuthMode.Register,
            allowSkip = true,
            onBack = null,
            onSkip = { accountViewModel.skipLogin() },
            onLogin = { account, password -> accountViewModel.login(account, password) },
            onRegister = { account, password -> accountViewModel.register(account, password) },
            onAuthSuccess = {},
        )
        return
    }

    if (showMyPage) {
        when (myPageDestination) {
            MyPageDestination.Root -> {
                MyScreen(
                    displayName = accountState.displayName,
                    subtitle = if (accountState.isLoggedIn) "点击查看和修改个人信息" else "点击登录或完善个人信息",
                    avatarUri = accountState.avatarUri,
                    onBack = {
                        showContactDialog = false
                        myPageDestination = MyPageDestination.Root
                        showMyPage = false
                    },
                    onProfileClick = {
                        showContactDialog = false
                        myPageDestination = if (accountState.isLoggedIn) {
                            MyPageDestination.Profile
                        } else {
                            MyPageDestination.Auth
                        }
                    },
                    onMenuClick = { action ->
                        when (action) {
                            MyMenuAction.CategoryManage -> {
                                showContactDialog = false
                                myPageDestination = MyPageDestination.CategoryManage
                            }
                            MyMenuAction.ReminderSettings -> {
                                showContactDialog = false
                                myPageDestination = MyPageDestination.ReminderSettings
                            }
                            MyMenuAction.DataAndSync -> {
                                showContactDialog = false
                                myPageDestination = MyPageDestination.DataAndSync
                            }
                            MyMenuAction.GeneralSettings -> {
                                showContactDialog = false
                                myPageDestination = MyPageDestination.GeneralSettings
                            }
                            MyMenuAction.ContactUs -> showContactDialog = true
                            MyMenuAction.About -> {
                                showContactDialog = false
                                myPageDestination = MyPageDestination.About
                            }
                        }
                    }
                )
            }
            MyPageDestination.Profile -> {
                ProfileInfoScreen(
                    state = accountState,
                    onBack = { myPageDestination = MyPageDestination.Root },
                    onOpenAuth = { myPageDestination = MyPageDestination.Auth },
                    onNicknameConfirm = { value -> accountViewModel.updateNickname(value) },
                    onAccountConfirm = { value -> accountViewModel.updateAccount(value) },
                    onAvatarChange = { uri -> accountViewModel.updateAvatar(uri) },
                    onLogout = { accountViewModel.logout() },
                    onCancelAccount = { accountViewModel.cancelAccount() },
                )
            }
            MyPageDestination.Auth -> {
                AuthScreen(
                    initialMode = if (accountState.hasLocalAccount) AuthMode.Login else AuthMode.Register,
                    allowSkip = false,
                    onBack = { myPageDestination = MyPageDestination.Profile },
                    onSkip = {},
                    onLogin = { account, password -> accountViewModel.login(account, password) },
                    onRegister = { account, password -> accountViewModel.register(account, password) },
                    onAuthSuccess = { myPageDestination = MyPageDestination.Profile },
                )
            }
            MyPageDestination.CategoryManage -> {
                CategoryManageScreen(
                    viewModel = categoryViewModel,
                    onBack = { myPageDestination = MyPageDestination.Root }
                )
            }
            MyPageDestination.ReminderSettings -> {
                ReminderSettingsScreen(onBack = { myPageDestination = MyPageDestination.Root })
            }
            MyPageDestination.DataAndSync -> {
                DataAndSyncScreen(onBack = { myPageDestination = MyPageDestination.Root })
            }
            MyPageDestination.GeneralSettings -> {
                GeneralSettingsScreen(onBack = { myPageDestination = MyPageDestination.Root })
            }
            MyPageDestination.About -> {
                AboutBianwanluScreen(onBack = { myPageDestination = MyPageDestination.Root })
            }
        }

        if (showContactDialog) {
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                title = { Text("联系我们") },
                text = { Text("如需反馈问题或建议，可后续通过官方联系方式联系我们。当前先完成页面交互。", color = Color(0xFF616161)) },
                confirmButton = {
                    TextButton(onClick = { showContactDialog = false }) {
                        Text("我知道了", color = Color(0xFF212121))
                    }
                }
            )
        }
        return
    }

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

    if (showBatchNoteReminderDialog) {
        ReminderDialog(
            onDismiss = { showBatchNoteReminderDialog = false },
            onSelect = { time ->
                noteViewModel.updateReminderForSelectedNotes(time)
                showBatchNoteReminderDialog = false
            },
            showClearAction = selectedNotes.any { it.reminderTime != null },
            onClear = {
                noteViewModel.updateReminderForSelectedNotes(null)
                showBatchNoteReminderDialog = false
            }
        )
    }

    if (showDeleteNotesConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteNotesConfirm = false },
            title = { Text("删除笔记") },
            text = { Text("确定删除已选择的笔记吗？", color = Color(0xFF616161)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteViewModel.deleteSelectedNotes()
                        showDeleteNotesConfirm = false
                    }
                ) {
                    Text("删除", color = Color(0xFFE65E4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteNotesConfirm = false }) {
                    Text("取消", color = Color(0xFF212121))
                }
            }
        )
    }

    if (showBatchTodoReminderDialog) {
        ReminderDialog(
            onDismiss = { showBatchTodoReminderDialog = false },
            onSelect = { time ->
                todoViewModel.updateReminderForSelectedTodos(time)
                showBatchTodoReminderDialog = false
            },
            showClearAction = selectedTodos.any { it.reminderTime != null },
            onClear = {
                todoViewModel.updateReminderForSelectedTodos(null)
                showBatchTodoReminderDialog = false
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
                userDisplayName = accountState.displayName,
                userSecondaryText = accountState.secondaryText,
                avatarUri = accountState.avatarUri,
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
                onMyClick = {
                    myPageDestination = MyPageDestination.Root
                    showContactDialog = false
                    showMyPage = true
                    scope.launch { drawerState.close() }
                },
                onSyncClick = { scope.launch { drawerState.close() } },
                onGameClick = { scope.launch { drawerState.close() } }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                if ((currentDestination == AppDestination.Notes && noteSelectionMode) ||
                    (currentDestination == AppDestination.Todo && todoSelectionMode)
                ) {
                    TodoSelectionTopBar(
                        selectedCount = if (currentDestination == AppDestination.Notes) selectedNoteIds.size else selectedTodoIds.size,
                        allSelected = if (currentDestination == AppDestination.Notes) allFilteredNotesSelected else allFilteredTodosSelected,
                        onCancel = {
                            if (currentDestination == AppDestination.Notes) {
                                noteViewModel.clearSelection()
                            } else {
                                todoViewModel.clearSelection()
                            }
                        },
                        onSelectAllToggle = {
                            if (currentDestination == AppDestination.Notes) {
                                if (allFilteredNotesSelected) {
                                    noteViewModel.clearSelection()
                                } else {
                                    noteViewModel.selectAllFilteredNotes()
                                }
                            } else {
                                if (allFilteredTodosSelected) {
                                    todoViewModel.clearSelection()
                                } else {
                                    todoViewModel.selectAllFilteredTodos()
                                }
                            }
                        }
                    )
                } else {
                    MainTopBar(
                        title = title,
                        itemCount = itemCount,
                        showArrow = hasCategoryDropdown,
                        isDropdownOpen = showCategoryDropdown,
                        avatarUri = accountState.avatarUri,
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
                if ((currentDestination == AppDestination.Notes && noteSelectionMode) ||
                    (currentDestination == AppDestination.Todo && todoSelectionMode)
                ) {
                    TodoSelectionBottomBar(
                        onPriorityClick = {
                            if (currentDestination == AppDestination.Notes) {
                                noteViewModel.applyPriorityToSelectedNotes()
                            } else {
                                todoViewModel.applyPriorityToSelectedTodos()
                            }
                        },
                        onReminderClick = {
                            if (currentDestination == AppDestination.Notes) {
                                showBatchNoteReminderDialog = true
                            } else {
                                showBatchTodoReminderDialog = true
                            }
                        },
                        onDeleteClick = {
                            if (currentDestination == AppDestination.Notes) {
                                showDeleteNotesConfirm = true
                            } else {
                                showDeleteTodosConfirm = true
                            }
                        }
                    )
                } else {
                    BottomNavBar(
                        currentDestination = currentDestination,
                        onDestinationSelected = {
                            if (currentDestination == AppDestination.Notes && it != currentDestination) {
                                noteViewModel.clearSelection()
                            }
                            if (currentDestination == AppDestination.Todo && it != currentDestination) {
                                todoViewModel.clearSelection()
                            }
                            if (it != currentDestination) showCategoryDropdown = false
                            currentDestination = it
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
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
    avatarUri: String?,
    onTitleClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = LocalAppIconTint.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 4.dp)
    ) {
        AvatarButton(
            avatarUri = avatarUri,
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showArrow) {
                        Icon(
                            imageVector = if (isDropdownOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "展开分类",
                            tint = iconTint,
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
                Icon(Icons.Outlined.Search, "搜索", tint = iconTint)
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.MoreVert, "更多", tint = iconTint)
            }
        }
    }
}

@Composable
private fun AvatarButton(
    avatarUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        ProfileAvatarImage(
            avatarUri = avatarUri,
            modifier = Modifier.fillMaxSize(),
            iconSize = 22.dp,
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
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(text = "取消", fontSize = 18.sp, color = primaryColor)
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
            Text(text = if (allSelected) "取消全选" else "全选", fontSize = 18.sp, color = primaryColor)
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
            .background(MaterialTheme.colorScheme.surface)
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
    val iconTint = LocalAppIconTint.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
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
            avatarUri = null,
            onTitleClick = {}, onAvatarClick = {},
            onSearchClick = {}, onMenuClick = {}
        )
    }
}




