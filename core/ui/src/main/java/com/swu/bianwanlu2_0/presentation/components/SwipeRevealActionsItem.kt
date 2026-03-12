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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.graphicsLayer
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
    actionSpacing: Dp = 0.dp,
    contentEndGap: Dp = 0.dp,
    animateActionsFromRight: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    isRevealed: Boolean = false,
    onRevealedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val totalActionWidth = actionWidth * actions.size + actionSpacing * (actions.size - 1).coerceAtLeast(0)
    val revealWidth = totalActionWidth + contentEndGap
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val actionSpacingPx = with(density) { actionSpacing.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isRevealed, revealWidthPx) {
        offsetX = if (isRevealed) -revealWidthPx else 0f
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
                .fillMaxWidth()
                .padding(end = contentEndGap),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val revealProgress = if (revealWidthPx > 0f) {
                (-animatedOffset / revealWidthPx).coerceIn(0f, 1f)
            } else {
                0f
            }

            actions.forEachIndexed { index, action ->
                val enterOffsetPx = if (animateActionsFromRight) {
                    (1f - revealProgress) * ((actions.size - index) * actionSpacingPx + 24f)
                } else {
                    0f
                }

                Box(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = enterOffsetPx
                            alpha = if (animateActionsFromRight) revealProgress else 1f
                        }
                        .background(action.backgroundColor, shape)
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

                if (index != actions.lastIndex) {
                    Spacer(modifier = Modifier.width(actionSpacing))
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-revealWidthPx, 0f)
                    },
                    onDragStopped = {
                        val revealThreshold = revealWidthPx * 0.35f
                        val shouldReveal = offsetX <= -revealThreshold
                        offsetX = if (shouldReveal) -revealWidthPx else 0f
                        onRevealedChange(shouldReveal)
                    }
                )
        ) {
            content()
        }
    }
}
