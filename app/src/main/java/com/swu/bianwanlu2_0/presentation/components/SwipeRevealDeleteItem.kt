package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

@Composable
fun SwipeRevealDeleteItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 96.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    isRevealed: Boolean = false,
    onRevealedChange: (Boolean) -> Unit = {},
    onSwipeRightAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRevealed, actionWidthPx) {
        offsetX = if (isRevealed) -actionWidthPx else 0f
    }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "swipe_reveal_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSwipeRightAction != null) {
                Box(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .background(Color(0xFF66BB6A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .padding(start = 14.dp, end = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE53935))
                        .clickable {
                            offsetX = 0f
                            onRevealedChange(false)
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "删除",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val minOffset = -actionWidthPx
                        val maxOffset = if (onSwipeRightAction != null) actionWidthPx else 0f
                        offsetX = (offsetX + delta).coerceIn(minimumValue = minOffset, maximumValue = maxOffset)
                    },
                    onDragStopped = {
                        val deleteRevealThreshold = actionWidthPx * 0.45f
                        val completeTriggerThreshold = actionWidthPx * 0.55f
                        offsetX = when {
                            onSwipeRightAction != null && offsetX >= completeTriggerThreshold -> {
                                onSwipeRightAction()
                                onRevealedChange(false)
                                0f
                            }

                            offsetX <= -deleteRevealThreshold -> {
                                onRevealedChange(true)
                                -actionWidthPx
                            }

                            else -> {
                                onRevealedChange(false)
                                0f
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}
