package com.example.gymlocker.ui.activeworkout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    // BOTH directions: + = right complete, - = left reveal delete
    val anchors = remember(rowWidthPx, safeRevealPx) {
        if (rowWidthPx <= 0f || safeRevealPx <= 0f) null
        else mapOf(
            0f to SwipeStage.Closed,
            +safeRevealPx to SwipeStage.Complete,
            -safeRevealPx to SwipeStage.RevealDelete
        )
    }

    // Complete triggers automatically then snaps back (unchanged behavior)
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeStage.Complete) {
            if (!isDone) onComplete()
            swipeState.animateTo(SwipeStage.Closed)
        }
    }

    val thresholds = { _: SwipeStage, _: SwipeStage -> FractionalThreshold(0.35f) }

    val offsetPx = swipeState.offset.value

    val showingDelete = offsetPx < 0f          // left swipe
    val showingComplete = offsetPx > 0f        // right swipe
    val revealDelete = swipeState.currentValue == SwipeStage.RevealDelete
    val isSwiping = abs(offsetPx) > 2f

    val progress = remember(offsetPx, safeRevealPx) {
        if (safeRevealPx <= 0f) 0f else (abs(offsetPx) / safeRevealPx).coerceIn(0f, 1f)
    }
    val easedProgress = progress * progress

    // Background alpha scales with swipe distance (full-width background)
    val bgMaxAlpha = 0.65f
    val bgAlphaTarget = if (isSwiping) lerp(0.12f, bgMaxAlpha, easedProgress) else 0f
    val bgAlpha by animateFloatAsState(
        targetValue = bgAlphaTarget,
        label = "SwipeBackgroundAlpha"
    )

    // Foreground becomes slightly transparent while swiping
    val fgMinAlpha = 0.78f
    val fgAlphaTarget = if (isSwiping) lerp(1f, fgMinAlpha, easedProgress) else 1f
    val fgAlpha by animateFloatAsState(
        targetValue = fgAlphaTarget,
        label = "SwipeForegroundAlpha"
    )

    // Directional background color (visual only) — use theme roles
    val deleteColor = MaterialTheme.colorScheme.error
    val completeColor = MaterialTheme.colorScheme.primary

    // Persistent done background — subtle, theme-safe
    val doneColor = MaterialTheme.colorScheme.secondary.copy(alpha = bgMaxAlpha)

    val bgColor: Color = when {
        isSwiping && showingDelete -> deleteColor.copy(alpha = bgAlpha)
        isSwiping && showingComplete -> completeColor.copy(alpha = bgAlpha)
        !isSwiping && isDone -> doneColor
        else -> Color.Transparent
    }

    val swipeableModifier = if (enabled && anchors != null) {
        Modifier.swipeable(
            state = swipeState,
            anchors = anchors,
            orientation = Orientation.Horizontal,
            thresholds = thresholds,
            // If directions feel flipped in RTL, set to true:
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
        // =========================
        // Full-width dynamic background + action affordances
        // =========================
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor)
        ) {
            // ---- Complete icon (RIGHT swipe reveals LEFT side) ----
            // Show while swiping right enough to clearly signal action.
            // Not clickable (completion is automatic).
            val showCompleteUi =
                (isSwiping && showingComplete && rowWidthPx > 0f && offsetPx > (rowWidthPx * 0.10f))

            if (showCompleteUi && safeRevealPx > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(safeRevealDp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // ---- Delete button (LEFT swipe reveals RIGHT side) ----
            // Keep exact behavior: reveal + lock + clickable trash
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
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        // =========================
        // Foreground content (slightly transparent during swipe)
        // =========================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .graphicsLayer(alpha = fgAlpha)
        ) {
            content()
        }
    }
}
