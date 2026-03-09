package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.ui.theme.NoteRed

@Composable
fun CategoryDropdown(
    visible: Boolean,
    categories: List<Category>,
    selectedCategory: Category?,
    defaultLabel: String,
    onSelect: (Category?) -> Unit,
    onDismiss: () -> Unit,
    onAddCategory: () -> Unit,
    onManageCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                CategoryItem(
                    name = defaultLabel,
                    isSelected = selectedCategory == null,
                    onClick = {
                        onSelect(null)
                        onDismiss()
                    }
                )

                categories.forEach { category ->
                    CategoryItem(
                        name = category.name,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = {
                            onSelect(category)
                            onDismiss()
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0xFFEEEEEE)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onDismiss()
                                onAddCategory()
                            }
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "新增分类",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF757575)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增分类", fontSize = 14.sp, color = Color(0xFF757575))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onDismiss()
                                onManageCategory()
                            }
                        )
                    ) {
                        Text("分类管理", fontSize = 14.sp, color = Color(0xFF757575))
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "分类管理",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }

    if (visible) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

@Composable
private fun CategoryItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF757575)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = NoteRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
