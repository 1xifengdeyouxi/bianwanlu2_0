package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.ui.theme.NoteRed

@Composable
fun AppDrawerContent(
    noteCategories: List<Category>,
    todoCategories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (Category) -> Unit,
    onMyClick: () -> Unit,
    onSyncClick: () -> Unit,
    onGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteExpanded by remember { mutableStateOf(true) }
    var todoExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color.White)
    ) {
        // 顶部用户区域
        DrawerHeader()

        // 分类列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // ===== 笔记分类 =====
            item(key = "note_section") {
                CategorySectionHeader(
                    title = "我的笔记分类",
                    expanded = noteExpanded,
                    onClick = { noteExpanded = !noteExpanded }
                )
            }
            if (noteExpanded) {
                items(noteCategories, key = { "n_${it.id}" }) { category ->
                    DrawerCategoryItem(
                        category = category,
                        icon = Icons.Outlined.BookmarkBorder,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { onCategorySelect(category) }
                    )
                }
                if (noteCategories.isEmpty()) {
                    item {
                        Text(
                            text = "暂无笔记分类",
                            fontSize = 13.sp,
                            color = Color(0xFFBDBDBD),
                            modifier = Modifier.padding(start = 52.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            // ===== 待办分类 =====
            item(key = "todo_section") {
                CategorySectionHeader(
                    title = "我的待办分类",
                    expanded = todoExpanded,
                    onClick = { todoExpanded = !todoExpanded }
                )
            }
            if (todoExpanded) {
                items(todoCategories, key = { "t_${it.id}" }) { category ->
                    DrawerCategoryItem(
                        category = category,
                        icon = Icons.Outlined.CheckBox,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { onCategorySelect(category) }
                    )
                }
                if (todoCategories.isEmpty()) {
                    item {
                        Text(
                            text = "暂无待办分类",
                            fontSize = 13.sp,
                            color = Color(0xFFBDBDBD),
                            modifier = Modifier.padding(start = 52.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        // 底部按钮行：我的 | 同步 | 小游戏
        DrawerBottomBar(
            onMyClick = onMyClick,
            onSyncClick = onSyncClick,
            onGameClick = onGameClick
        )
    }
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF5F5F5), Color.White)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Hi,上午好",
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "昵称未设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 头像
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFFE0E0E0), CircleShape)
                .background(Color(0xFFEEEEEE))
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = "头像",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun CategorySectionHeader(
    title: String,
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起" else "展开",
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DrawerCategoryItem(
    category: Category,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) NoteRed else Color(0xFF757575),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color(0xFF212121) else Color(0xFF424242),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = NoteRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DrawerBottomBar(
    onMyClick: () -> Unit,
    onSyncClick: () -> Unit,
    onGameClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawerBottomItem(
            icon = Icons.Outlined.Person,
            label = "我的",
            onClick = onMyClick
        )
        DrawerBottomItem(
            icon = Icons.Outlined.Sync,
            label = "同步",
            onClick = onSyncClick
        )
        DrawerBottomItem(
            icon = Icons.Outlined.SportsEsports,
            label = "小游戏",
            onClick = onGameClick
        )
    }
}

@Composable
private fun DrawerBottomItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF757575),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF757575))
    }
}
