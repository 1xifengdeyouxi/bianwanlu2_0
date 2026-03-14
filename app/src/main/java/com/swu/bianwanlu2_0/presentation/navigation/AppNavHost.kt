package com.swu.bianwanlu2_0.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateListOf
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
import com.swu.bianwanlu2_0.presentation.screens.profile.ProfileActionDialog
import com.swu.bianwanlu2_0.presentation.screens.profile.ProfileAvatarImage
import com.swu.bianwanlu2_0.presentation.screens.profile.ProfileInfoScreen
import com.swu.bianwanlu2_0.presentation.screens.profile.ReminderSettingsScreen
import com.swu.bianwanlu2_0.presentation.screens.timeline.MainTimelineScreen
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

private enum class NavigationMotionDirection {
    Forward,
    Backward,
}

private sealed interface HostScreen {
    data object Main : HostScreen
    data object AuthEntry : HostScreen
    data class My(val destination: MyPageDestination) : HostScreen
    data object AddNote : HostScreen
    data class EditNote(val note: Note) : HostScreen
    data object AddTodo : HostScreen
    data class EditTodo(val todo: Todo) : HostScreen
    data object CategoryManage : HostScreen
}

private fun createScreenTransition(direction: NavigationMotionDirection): ContentTransform {
    val enterOffset: (Int) -> Int = { fullWidth ->
        if (direction == NavigationMotionDirection.Forward) fullWidth / 3 else -fullWidth / 3
    }
    val exitOffset: (Int) -> Int = { fullWidth ->
        if (direction == NavigationMotionDirection.Forward) -fullWidth / 5 else fullWidth / 5
    }
    return (
        slideInHorizontally(
            animationSpec = tween(durationMillis = 280),
            initialOffsetX = enterOffset,
        ) + fadeIn(animationSpec = tween(durationMillis = 280))
        ).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 240),
                targetOffsetX = exitOffset,
            ) + fadeOut(animationSpec = tween(durationMillis = 220))
        )
}

private fun appDestinationIndex(destination: AppDestination): Int = when (destination) {
    AppDestination.Notes -> 0
    AppDestination.Todo -> 1
    AppDestination.Timeline -> 2
    AppDestination.Calendar -> 3
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    var currentDestination by remember {
        mutableStateOf<AppDestination>(AppDestination.Notes)
    }
    val mainDestinationStack = remember { mutableStateListOf<AppDestination>(AppDestination.Notes) }

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
    val myPageStack = remember { mutableStateListOf(MyPageDestination.Root) }
    val myPageDestination = myPageStack.lastOrNull() ?: MyPageDestination.Root
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
    var hostMotionDirection by remember { mutableStateOf(NavigationMotionDirection.Forward) }
    var mainMotionDirection by remember { mutableStateOf(NavigationMotionDirection.Forward) }

    val allFilteredNotesSelected =
        filteredNotes.isNotEmpty() && filteredNotes.all { it.id in selectedNoteIds }
    val allFilteredTodosSelected =
        filteredTodos.isNotEmpty() && filteredTodos.all { it.id in selectedTodoIds }

    fun resetMyPageStack() {
        myPageStack.clear()
        myPageStack.add(MyPageDestination.Root)
    }

    fun openMyPage(destination: MyPageDestination = MyPageDestination.Root) {
        hostMotionDirection = NavigationMotionDirection.Forward
        showContactDialog = false
        showMyPage = true
        resetMyPageStack()
        if (destination != MyPageDestination.Root) {
            myPageStack.add(destination)
        }
    }

    fun pushMyPage(destination: MyPageDestination) {
        if (myPageDestination == destination) return
        hostMotionDirection = NavigationMotionDirection.Forward
        showContactDialog = false
        myPageStack.add(destination)
    }

    fun popMyPageOrClose() {
        hostMotionDirection = NavigationMotionDirection.Backward
        showContactDialog = false
        if (myPageStack.size > 1) {
            myPageStack.removeAt(myPageStack.lastIndex)
        } else {
            showMyPage = false
            resetMyPageStack()
        }
    }

    fun navigateMainDestination(destination: AppDestination) {
        if (destination == currentDestination) return
        if (currentDestination == AppDestination.Notes) {
            noteViewModel.clearSelection()
        }
        if (currentDestination == AppDestination.Todo) {
            todoViewModel.clearSelection()
        }
        mainMotionDirection = if (appDestinationIndex(destination) >= appDestinationIndex(currentDestination)) {
            NavigationMotionDirection.Forward
        } else {
            NavigationMotionDirection.Backward
        }
        showCategoryDropdown = false
        currentDestination = destination
        mainDestinationStack.add(destination)
    }

    fun popMainDestination() {
        if (mainDestinationStack.size <= 1) return
        mainMotionDirection = NavigationMotionDirection.Backward
        showCategoryDropdown = false
        mainDestinationStack.removeAt(mainDestinationStack.lastIndex)
        currentDestination = mainDestinationStack.last()
    }

    val hostScreen = when {
        !accountState.hasSeenAuthChoice -> HostScreen.AuthEntry
        showMyPage -> HostScreen.My(myPageDestination)
        showAddNote -> HostScreen.AddNote
        editingNote != null -> HostScreen.EditNote(editingNote!!)
        showAddTodo -> HostScreen.AddTodo
        editingTodo != null -> HostScreen.EditTodo(editingTodo!!)
        showCategoryManage -> HostScreen.CategoryManage
        else -> HostScreen.Main
    }

    BackHandler(
        enabled =
            showContactDialog ||
                showMyPage ||
                showAddNote ||
                editingNote != null ||
                showAddTodo ||
                editingTodo != null ||
                showCategoryManage ||
                drawerState.isOpen ||
                showCategoryDropdown ||
                noteSelectionMode ||
                todoSelectionMode ||
                mainDestinationStack.size > 1,
    ) {
        when {
            showContactDialog -> showContactDialog = false
            showMyPage -> popMyPageOrClose()
            showAddNote -> {
                hostMotionDirection = NavigationMotionDirection.Backward
                showAddNote = false
            }
            editingNote != null -> {
                hostMotionDirection = NavigationMotionDirection.Backward
                editingNote = null
            }
            showAddTodo -> {
                hostMotionDirection = NavigationMotionDirection.Backward
                showAddTodo = false
            }
            editingTodo != null -> {
                hostMotionDirection = NavigationMotionDirection.Backward
                editingTodo = null
            }
            showCategoryManage -> {
                hostMotionDirection = NavigationMotionDirection.Backward
                showCategoryManage = false
            }
            drawerState.isOpen -> scope.launch { drawerState.close() }
            showCategoryDropdown -> showCategoryDropdown = false
            currentDestination == AppDestination.Notes && noteSelectionMode -> noteViewModel.clearSelection()
            currentDestination == AppDestination.Todo && todoSelectionMode -> todoViewModel.clearSelection()
            mainDestinationStack.size > 1 -> popMainDestination()
        }
    }

    @Composable
    fun MyPageHost(destination: MyPageDestination) {
        when (destination) {
            MyPageDestination.Root -> {
                MyScreen(
                    displayName = accountState.displayName,
                    subtitle = if (accountState.isLoggedIn) "点击查看和修改个人信息" else "点击登录或完善个人信息",
                    avatarUri = accountState.avatarUri,
                    onBack = { popMyPageOrClose() },
                    onProfileClick = {
                        pushMyPage(
                            if (accountState.isLoggedIn) MyPageDestination.Profile else MyPageDestination.Auth,
                        )
                    },
                    onMenuClick = { action ->
                        when (action) {
                            MyMenuAction.CategoryManage -> pushMyPage(MyPageDestination.CategoryManage)
                            MyMenuAction.ReminderSettings -> pushMyPage(MyPageDestination.ReminderSettings)
                            MyMenuAction.DataAndSync -> pushMyPage(MyPageDestination.DataAndSync)
                            MyMenuAction.GeneralSettings -> pushMyPage(MyPageDestination.GeneralSettings)
                            MyMenuAction.ContactUs -> showContactDialog = true
                            MyMenuAction.About -> pushMyPage(MyPageDestination.About)
                        }
                    },
                )
            }

            MyPageDestination.Profile -> {
                ProfileInfoScreen(
                    state = accountState,
                    onBack = { popMyPageOrClose() },
                    onOpenAuth = { pushMyPage(MyPageDestination.Auth) },
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
                    onBack = { popMyPageOrClose() },
                    onSkip = {},
                    onLogin = { account, password -> accountViewModel.login(account, password) },
                    onRegister = { account, password -> accountViewModel.register(account, password) },
                    onAuthSuccess = {
                        hostMotionDirection = NavigationMotionDirection.Forward
                        if (myPageStack.lastOrNull() == MyPageDestination.Auth) {
                            myPageStack.removeAt(myPageStack.lastIndex)
                        }
                        if (myPageStack.lastOrNull() != MyPageDestination.Profile) {
                            myPageStack.add(MyPageDestination.Profile)
                        }
                    },
                )
            }

            MyPageDestination.CategoryManage -> {
                CategoryManageScreen(
                    viewModel = categoryViewModel,
                    onBack = { popMyPageOrClose() },
                )
            }

            MyPageDestination.ReminderSettings -> {
                ReminderSettingsScreen(onBack = { popMyPageOrClose() })
            }

            MyPageDestination.DataAndSync -> {
                DataAndSyncScreen(onBack = { popMyPageOrClose() })
            }

            MyPageDestination.GeneralSettings -> {
                GeneralSettingsScreen(onBack = { popMyPageOrClose() })
            }

            MyPageDestination.About -> {
                AboutBianwanluScreen(onBack = { popMyPageOrClose() })
            }
        }

        if (showContactDialog) {
            ProfileActionDialog(
                title = "联系我们",
                message = "如需反馈问题或建议，你可以优先在“关于便玩录”中的“意见与反馈”提交 GitHub Issues。更多官方联系方式后续开放。",
                options = emptyList(),
                dismissText = "我知道了",
                onDismiss = { showContactDialog = false },
                onOptionClick = {},
            )
        }
    }

    @Composable
    fun MainHost() {
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
                                navigateMainDestination(AppDestination.Notes)
                                noteViewModel.selectCategory(cat)
                            }

                            CategoryType.TODO -> {
                                navigateMainDestination(AppDestination.Todo)
                                todoViewModel.selectCategory(cat)
                            }
                        }
                        scope.launch { drawerState.close() }
                    },
                    onMyClick = {
                        openMyPage()
                        scope.launch { drawerState.close() }
                    },
                    onSyncClick = { scope.launch { drawerState.close() } },
                    onGameClick = { scope.launch { drawerState.close() } },
                )
            },
            gesturesEnabled = true,
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
                            },
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
                            onMenuClick = {},
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
                            },
                        )
                    } else {
                        BottomNavBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { navigateMainDestination(it) },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = { createScreenTransition(mainMotionDirection) },
                        modifier = Modifier.fillMaxSize(),
                        label = "main_screen_transition",
                    ) { destination ->
                        when (destination) {
                            AppDestination.Notes -> NoteListScreen(
                                viewModel = noteViewModel,
                                onAddNote = {
                                    hostMotionDirection = NavigationMotionDirection.Forward
                                    showAddNote = true
                                },
                                onEditNote = {
                                    hostMotionDirection = NavigationMotionDirection.Forward
                                    editingNote = it
                                },
                            )

                            AppDestination.Todo -> TodoListScreen(
                                viewModel = todoViewModel,
                                onAddTodo = {
                                    hostMotionDirection = NavigationMotionDirection.Forward
                                    showAddTodo = true
                                },
                                onEditTodo = {
                                    hostMotionDirection = NavigationMotionDirection.Forward
                                    editingTodo = it
                                },
                            )

                            AppDestination.Timeline -> MainTimelineScreen()
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
                                    onClick = { showCategoryDropdown = false },
                                )
                                .zIndex(5f),
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
                            onManageCategory = {
                                hostMotionDirection = NavigationMotionDirection.Forward
                                showCategoryManage = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(10f),
                        )
                    }
                }
            }
        }
    }

    AnimatedContent(
        targetState = hostScreen,
        transitionSpec = { createScreenTransition(hostMotionDirection) },
        modifier = modifier.fillMaxSize(),
        label = "host_screen_transition",
    ) { screen ->
        when (screen) {
            HostScreen.AuthEntry -> {
                AuthScreen(
                    initialMode = if (accountState.hasLocalAccount) AuthMode.Login else AuthMode.Register,
                    allowSkip = true,
                    onBack = null,
                    onSkip = { accountViewModel.skipLogin() },
                    onLogin = { account, password -> accountViewModel.login(account, password) },
                    onRegister = { account, password -> accountViewModel.register(account, password) },
                    onAuthSuccess = {},
                )
            }

            is HostScreen.My -> MyPageHost(screen.destination)

            HostScreen.AddNote -> {
                AddNoteScreen(
                    onCancel = {
                        hostMotionDirection = NavigationMotionDirection.Backward
                        showAddNote = false
                    },
                    onConfirm = { noteTitle, noteContent, reminderTime, isPriority, cardColor, textColor, imageUris ->
                        noteViewModel.addNote(
                            title = noteTitle,
                            content = noteContent,
                            reminderTime = reminderTime,
                            isPriority = isPriority,
                            cardColor = cardColor,
                            textColor = textColor,
                            imageUris = imageUris,
                        )
                        hostMotionDirection = NavigationMotionDirection.Backward
                        showAddNote = false
                    },
                )
            }

            is HostScreen.EditNote -> {
                AddNoteScreen(
                    existingNote = screen.note,
                    onCancel = {
                        hostMotionDirection = NavigationMotionDirection.Backward
                        editingNote = null
                    },
                    onConfirm = { noteTitle, noteContent, reminderTime, isPriority, cardColor, textColor, imageUris ->
                        noteViewModel.updateNote(
                            existing = screen.note,
                            title = noteTitle,
                            content = noteContent,
                            reminderTime = reminderTime,
                            isPriority = isPriority,
                            cardColor = cardColor,
                            textColor = textColor,
                            imageUris = imageUris,
                        )
                        hostMotionDirection = NavigationMotionDirection.Backward
                        editingNote = null
                    },
                )
            }

            HostScreen.AddTodo -> {
                AddTodoScreen(
                    onCancel = {
                        hostMotionDirection = NavigationMotionDirection.Backward
                        showAddTodo = false
                    },
                    onConfirm = { todoTitle, reminderTime, isPriority, cardColor ->
                        todoViewModel.addTodo(todoTitle, reminderTime, isPriority, cardColor)
                        hostMotionDirection = NavigationMotionDirection.Backward
                        showAddTodo = false
                    },
                )
            }

            is HostScreen.EditTodo -> {
                AddTodoScreen(
                    existingTodo = screen.todo,
                    onCancel = {
                        hostMotionDirection = NavigationMotionDirection.Backward
                        editingTodo = null
                    },
                    onConfirm = { todoTitle, reminderTime, isPriority, cardColor ->
                        todoViewModel.updateTodo(screen.todo, todoTitle, reminderTime, isPriority, cardColor)
                        hostMotionDirection = NavigationMotionDirection.Backward
                        editingTodo = null
                    },
                )
            }

            HostScreen.CategoryManage -> {
                CategoryManageScreen(
                    viewModel = categoryViewModel,
                    onBack = {
                        hostMotionDirection = NavigationMotionDirection.Backward
                        showCategoryManage = false
                    },
                )
            }

            HostScreen.Main -> MainHost()
        }
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
            },
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
                    },
                ) {
                    Text("删除", color = Color(0xFFE65E4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteNotesConfirm = false }) {
                    Text("取消", color = Color(0xFF212121))
                }
            },
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
            },
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
                    },
                ) {
                    Text("删除", color = Color(0xFFE65E4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTodosConfirm = false }) {
                    Text("取消", color = Color(0xFF212121))
                }
            },
        )
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
            },
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
                if (itemCount >= 0) {
                    Text(
                        text = "共${itemCount}条",
                        fontSize = 11.sp,
                        color = NavUnselected,
                        lineHeight = 14.sp
                    )
                }
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





