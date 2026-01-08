package com.example.gymlocker.ui.activeworkout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SwipeStage { Closed, Complete, RevealDelete }

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableSetRow(
    enabled: Boolean = true,
    isDone: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val revealDistanceDp = 96.dp
    val revealPx = with(density) { revealDistanceDp.toPx() }

    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val swipeState = rememberSwipeableState(initialValue = SwipeStage.Closed)

    val safeRevealPx = remember(rowWidthPx, revealPx) {
        if (rowWidthPx <= 0f) 0f else minOf(revealPx, rowWidthPx * 0.6f)
    }
    val safeRevealDp = with(density) { safeRevealPx.toDp() }

    // ✅ IMPORTANT: include BOTH directions (positive = right, negative = left)
    val anchors = remember(rowWidthPx, safeRevealPx) {
        if (rowWidthPx <= 0f || safeRevealPx <= 0f) null
        else mapOf(
            0f to SwipeStage.Closed,
            +safeRevealPx to SwipeStage.Complete,
            -safeRevealPx to SwipeStage.RevealDelete
        )
    }

    // Classic swipe: if user releases into "Complete", trigger and snap back
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeStage.Complete) {
            if (!isDone) onComplete()
            swipeState.animateTo(SwipeStage.Closed)
        }
    }

    val thresholds = { _: SwipeStage, _: SwipeStage -> FractionalThreshold(0.35f) }

    val offsetPx = swipeState.offset.value

    val showingDelete = offsetPx < 0f
    val showingComplete = offsetPx > 0f
    val revealDelete = swipeState.currentValue == SwipeStage.RevealDelete
    val isSwiping = abs(offsetPx) > 2f

    val progress = remember(offsetPx, safeRevealPx) {
        if (safeRevealPx <= 0f) 0f else (abs(offsetPx) / safeRevealPx).coerceIn(0f, 1f)
    }
    val easedProgress = progress * progress

    // ✅ Stronger minimum so green is clearly visible even in dark theme
    val maxAlpha = 0.45f
    val bgAlphaTarget = if (isSwiping) lerp(0.12f, maxAlpha, easedProgress) else 0f
    val bgAlpha by animateFloatAsState(
        targetValue = bgAlphaTarget,
        label = "SwipeBackgroundAlpha"
    )

    // ✅ Red for left swipe, green for right swipe
    val baseBgColor: Color = when {
        showingDelete -> Color(0xFFD32F2F)    // red
        showingComplete -> Color(0xFF2E7D32) // green
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bgColor = baseBgColor.copy(alpha = bgAlpha)

    val swipeableModifier = if (enabled && anchors != null) {
        Modifier.swipeable(
            state = swipeState,
            anchors = anchors,
            orientation = Orientation.Horizontal,
            thresholds = thresholds,
            // If your layout is RTL and directions feel flipped, set this to true:
            // reverseDirection = true
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width.toFloat().coerceAtLeast(0f) }
            .then(swipeableModifier)
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            val showDeleteUi =
                (revealDelete && showingDelete) ||
                        (isSwiping && showingDelete && rowWidthPx > 0f && abs(offsetPx) > (rowWidthPx * 0.12f))

            if (showDeleteUi && safeRevealPx > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(safeRevealDp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            onDelete()
                            scope.launch { swipeState.animateTo(SwipeStage.Closed) }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Foreground content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
        ) {
            content()
        }
    }
}
