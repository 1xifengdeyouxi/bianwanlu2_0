package com.swu.bianwanlu2_0.presentation.components

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.ui.theme.LocalAppIconTint

@Composable
fun AppDrawerContent(
    noteCategories: List<Category>,
    todoCategories: List<Category>,
    selectedCategory: Category?,
    userDisplayName: String,
    userSecondaryText: String,
    avatarUri: String?,
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
            .background(MaterialTheme.colorScheme.surface)
    ) {
        DrawerHeader(
            displayName = userDisplayName,
            secondaryText = userSecondaryText,
            avatarUri = avatarUri,
            onClick = onMyClick,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item(key = "note_section") {
                CategorySectionHeader(
                    title = "我的笔记分类",
                    expanded = noteExpanded,
                    onClick = { noteExpanded = !noteExpanded }
                )
            }
            item(key = "note_content") {
                AnimatedVisibility(
                    visible = noteExpanded,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                        expandVertically(
                            animationSpec = tween(durationMillis = 240),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                        shrinkVertically(
                            animationSpec = tween(durationMillis = 220),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        noteCategories.forEach { category ->
                            DrawerCategoryItem(
                                category = category,
                                icon = Icons.Outlined.BookmarkBorder,
                                isSelected = selectedCategory?.id == category.id,
                                onClick = { onCategorySelect(category) }
                            )
                        }
                        if (noteCategories.isEmpty()) {
                            Text(
                                text = "暂无笔记分类",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 52.dp, top = 8.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            item(key = "todo_section") {
                CategorySectionHeader(
                    title = "我的待办分类",
                    expanded = todoExpanded,
                    onClick = { todoExpanded = !todoExpanded }
                )
            }
            item(key = "todo_content") {
                AnimatedVisibility(
                    visible = todoExpanded,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                        expandVertically(
                            animationSpec = tween(durationMillis = 240),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                        shrinkVertically(
                            animationSpec = tween(durationMillis = 220),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        todoCategories.forEach { category ->
                            DrawerCategoryItem(
                                category = category,
                                icon = Icons.Outlined.CheckBox,
                                isSelected = selectedCategory?.id == category.id,
                                onClick = { onCategorySelect(category) }
                            )
                        }
                        if (todoCategories.isEmpty()) {
                            Text(
                                text = "暂无待办分类",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 52.dp, top = 8.dp, bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DrawerBottomBar(
            onMyClick = onMyClick,
            onSyncClick = onSyncClick,
            onGameClick = onGameClick
        )
    }
}

@Composable
private fun DrawerHeader(
    displayName: String,
    secondaryText: String,
    avatarUri: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = secondaryText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
        ) {
            DrawerAvatarImage(
                avatarUri = avatarUri,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DrawerAvatarImage(
    avatarUri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconTint = LocalAppIconTint.current
    val imageBitmap = remember(avatarUri) {
        avatarUri
            ?.takeIf { it.isNotBlank() }
            ?.let { value ->
                runCatching {
                    val uri = Uri.parse(value)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(context.contentResolver, uri),
                        ).asImageBitmap()
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri).asImageBitmap()
                    }
                }.getOrNull()
            }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = "头像",
                tint = iconTint.copy(alpha = 0.7f),
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
    val iconTint = LocalAppIconTint.current
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "drawer_section_arrow",
    )

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "收起" else "展开",
            tint = iconTint.copy(alpha = 0.8f),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = arrowRotation }
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
    val iconTint = LocalAppIconTint.current

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
            tint = if (isSelected) iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选中",
                tint = iconTint,
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
    val iconTint = LocalAppIconTint.current

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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
