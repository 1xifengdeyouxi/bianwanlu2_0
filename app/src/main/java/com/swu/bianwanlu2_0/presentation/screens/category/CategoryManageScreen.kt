package com.swu.bianwanlu2_0.presentation.screens.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.presentation.components.AddCategoryDialog
import com.swu.bianwanlu2_0.ui.theme.NoteRed

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
    var noteExpanded by remember { mutableStateOf(true) }
    var todoExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部栏
        ManageTopBar(
            onBack = onBack,
            onAdd = {
                addDialogDefaultType = CategoryType.NOTE
                showAddDialog = true
            }
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ===== 笔记分组 =====
            item(key = "note_header") {
                SectionHeader(
                    title = "笔记",
                    expanded = noteExpanded,
                    onClick = { noteExpanded = !noteExpanded }
                )
            }
            if (noteExpanded) {
                items(noteCategories, key = { "note_${it.id}" }) { category ->
                    CategoryManageItem(
                        category = category,
                        viewModel = viewModel,
                        icon = Icons.Outlined.BookmarkBorder
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // ===== 待办分组 =====
            item(key = "todo_header") {
                SectionHeader(
                    title = "待办",
                    expanded = todoExpanded,
                    onClick = { todoExpanded = !todoExpanded }
                )
            }
            if (todoExpanded) {
                items(todoCategories, key = { "todo_${it.id}" }) { category ->
                    CategoryManageItem(
                        category = category,
                        viewModel = viewModel,
                        icon = Icons.Outlined.CheckBox
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                }
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
private fun CategoryManageItem(
    category: Category,
    viewModel: CategoryViewModel,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val count by viewModel.countItemsInCategory(category).collectAsStateWithLifecycle()
    val typeLabel = when (category.type) {
        CategoryType.NOTE -> "便签"
        CategoryType.TODO -> "待办"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = "排序",
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(22.dp)
        )
    }
}
