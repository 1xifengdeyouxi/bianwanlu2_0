package com.swu.bianwanlu2_0.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.presentation.screens.calendar.CalendarScreen
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteListScreen
import com.swu.bianwanlu2_0.presentation.screens.notes.NoteViewModel
import com.swu.bianwanlu2_0.presentation.screens.timeline.TimelineScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoListScreen
import com.swu.bianwanlu2_0.presentation.screens.todo.TodoViewModel
import com.swu.bianwanlu2_0.ui.theme.Bianwanlu2_0Theme
import com.swu.bianwanlu2_0.ui.theme.NavUnselected

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    // 修复：AppDestination 是 sealed class，无法用 rememberSaveable 存入 Bundle
    // 使用 remember 代替，导航状态不需要跨进程恢复
    var currentDestination by remember {
        mutableStateOf<AppDestination>(AppDestination.Notes)
    }

    val noteViewModel: NoteViewModel = hiltViewModel()
    val todoViewModel: TodoViewModel = hiltViewModel()

    val noteCount by noteViewModel.noteCount.collectAsStateWithLifecycle()
    val todoCount by todoViewModel.todoCount.collectAsStateWithLifecycle()

    val itemCount = when (currentDestination) {
        AppDestination.Notes -> noteCount
        AppDestination.Todo -> todoCount
        else -> -1
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(
                itemCount = itemCount,
                onSearchClick = {},
                onMenuClick = {}
            )
        },
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { currentDestination = it }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screen_transition"
        ) { destination ->
            when (destination) {
                AppDestination.Notes -> NoteListScreen(viewModel = noteViewModel)
                AppDestination.Todo -> TodoListScreen(viewModel = todoViewModel)
                AppDestination.Timeline -> TimelineScreen()
                AppDestination.Calendar -> CalendarScreen()
            }
        }
    }
}

/**
 * 顶部栏：左侧头像 | 中间"便签"+副标题 | 右侧搜索/更多
 */
@Composable
private fun MainTopBar(
    itemCount: Int,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarButton(modifier = Modifier.padding(start = 8.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "便签",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF212121)
            )
            Text(
                text = if (itemCount >= 0) "共${itemCount}条" else "共···条",
                fontSize = 11.sp,
                color = NavUnselected,
                lineHeight = 14.sp
            )
        }

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = Color(0xFF424242)
            )
        }
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "更多",
                tint = Color(0xFF424242)
            )
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
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = CircleShape)
            .background(Color(0xFFF5F5F5))
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
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
        MainTopBar(itemCount = 3, onSearchClick = {}, onMenuClick = {})
    }
}
@Preview(showBackground = true)
@Composable
private fun MainTopBarPreview1() {
    Bianwanlu2_0Theme {
        AppNavHost()
    }
}