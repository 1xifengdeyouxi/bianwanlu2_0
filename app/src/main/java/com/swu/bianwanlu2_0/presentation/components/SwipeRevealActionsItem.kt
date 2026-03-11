package com.swu.bianwanlu2_0.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class SwipeActionItem(
    val label: String,
    val backgroundColor: Color,
    val onClick: () -> Unit
)

@Composable
fun SwipeRevealActionsItem(
    actions: List<SwipeActionItem>,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 84.dp,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    isRevealed: Boolean = false,
    onRevealedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val totalActionWidth = actionWidth * actions.size
    val totalActionWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { totalActionWidth.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRevealed, totalActionWidthPx) {
        offsetX = if (isRevealed) -totalActionWidthPx else 0f
    }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "swipe_action_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                Box(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .background(action.backgroundColor)
                        .clickable {
                            offsetX = 0f
                            onRevealedChange(false)
                            action.onClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = action.label,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
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
                        offsetX = (offsetX + delta).coerceIn(-totalActionWidthPx, 0f)
                    },
                    onDragStopped = {
                        val revealThreshold = totalActionWidthPx * 0.35f
                        val shouldReveal = offsetX <= -revealThreshold
                        offsetX = if (shouldReveal) -totalActionWidthPx else 0f
                        onRevealedChange(shouldReveal)
                    }
                )
        ) {
            content()
        }
    }
}
