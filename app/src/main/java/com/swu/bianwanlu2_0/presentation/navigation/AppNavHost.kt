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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.swu.bianwanlu2_0.presentation.components.CategoryDropdown
import com.swu.bianwanlu2_0.presentation.screens.calendar.CalendarScreen
import com.swu.bianwanlu2_0.presentation.screens.category.CategoryManageScreen
import com.swu.bianwanlu2_0.presentation.screens.category.CategoryViewModel
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteListScreen
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteViewModel
import com.swu.bianwanlu2_0.presentation.screens.timeline.TimelineScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoListScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoViewModel
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.NavUnselected

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    var currentDestination by remember {
        mutableStateOf<AppDestination>(AppDestination.Notes)
    }

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
    val defaultLabel = when (currentDestination) {
        AppDestination.Notes -> "笔记"
        AppDestination.Todo -> "待办"
        else -> ""
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }

    // 分类管理全屏页面
    if (showCategoryManage) {
        CategoryManageScreen(
            viewModel = categoryViewModel,
            onBack = { showCategoryManage = false }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(
                title = title,
                itemCount = itemCount,
                showArrow = hasCategoryDropdown,
                isDropdownOpen = showCategoryDropdown,
                onTitleClick = {
                    if (hasCategoryDropdown) showCategoryDropdown = !showCategoryDropdown
                },
                onSearchClick = {},
                onMenuClick = {}
            )
        },
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = {
                    if (it != currentDestination) showCategoryDropdown = false
                    currentDestination = it
                }
            )
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
                    AppDestination.Notes -> NoteListScreen(viewModel = noteViewModel)
                    AppDestination.Todo -> TodoListScreen(viewModel = todoViewModel)
                    AppDestination.Timeline -> TimelineScreen()
                    AppDestination.Calendar -> CalendarScreen()
                }
            }

            if (hasCategoryDropdown) {
                CategoryDropdown(
                    visible = showCategoryDropdown,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    defaultLabel = defaultLabel,
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
private fun AvatarButton(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
            .background(Color(0xFFF5F5F5))
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = "用户头像",
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(22.dp)
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
            onTitleClick = {}, onSearchClick = {}, onMenuClick = {}
        )
    }
}
