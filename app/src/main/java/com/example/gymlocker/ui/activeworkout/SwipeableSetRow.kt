package com.example.gymlocker.ui.activeworkout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableSetRow(
    enabled: Boolean = true,
    isDone: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // Must match dismissThresholds below
    val thresholdFraction = 0.35f

    var hasBuzzedThisSwipe by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState(
        confirmStateChange = { newValue ->
            if (!enabled) return@rememberDismissState false

            when (newValue) {
                DismissValue.DismissedToEnd -> {
                    if (!isDone) onComplete()
                    hasBuzzedThisSwipe = false
                    false
                }

                DismissValue.DismissedToStart -> {
                    onDelete()
                    hasBuzzedThisSwipe = false
                    false
                }

                else -> {
                    hasBuzzedThisSwipe = false
                    false
                }
            }
        }
    )

    // Raw swipe offset in px (positive=right, negative=left)
    val offsetPx = dismissState.offset.value
    val dirRaw = dismissState.dismissDirection

    // ✅ Only treat as swiping if we actually moved a bit
    val isSwiping = abs(offsetPx) > 2f && dirRaw != null
    val dir: DismissDirection? = if (isSwiping) dirRaw else null

    // A decent normalization constant for phones; tune if you want
    val normalizePx = 280f

    // ✅ If not swiping, force progress to 0 so background text can't "stick"
    val rawProgress = (abs(offsetPx) / normalizePx).coerceIn(0f, 1f)
    val progressTarget = if (isSwiping) rawProgress else 0f

    // Smooth the visuals
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.85f),
        label = "swipeProgress"
    )

    // Tiny parallax on content
    val contentShift = if (isSwiping) (offsetPx * 0.08f).coerceIn(-18f, 18f) else 0f

    // Threshold “pop”: scale slightly more once past threshold
    val pastThreshold = progress >= thresholdFraction
    val iconScaleTarget = if (pastThreshold) 1.12f else (0.85f + 0.20f * progress)
    val iconScale by animateFloatAsState(
        targetValue = iconScaleTarget,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.75f),
        label = "iconScale"
    )

    // ✅ Hide icons/text completely when not swiping
    val iconAlphaTarget = if (isSwiping) (0.15f + 0.85f * progress).coerceIn(0f, 1f) else 0f
    val iconAlpha by animateFloatAsState(
        targetValue = iconAlphaTarget,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.9f),
        label = "iconAlpha"
    )

    // One-time haptic feedback when crossing threshold during a swipe
    LaunchedEffect(pastThreshold, dir) {
        if (!enabled) return@LaunchedEffect
        if (dir == null) return@LaunchedEffect

        if (pastThreshold && !hasBuzzedThisSwipe) {
            haptics.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
            )
            hasBuzzedThisSwipe = true
        }
        if (!pastThreshold) {
            hasBuzzedThisSwipe = false
        }
    }

    SwipeToDismiss(
        state = dismissState,
        directions = if (enabled)
            setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
        else emptySet(),
        dismissThresholds = { FractionalThreshold(thresholdFraction) },
        background = {
            val base = MaterialTheme.colorScheme.surfaceVariant
            val complete = MaterialTheme.colorScheme.primaryContainer
            val delete = MaterialTheme.colorScheme.errorContainer

            val bgColor = when (dir) {
                DismissDirection.StartToEnd -> lerp(base, complete, progress)
                DismissDirection.EndToStart -> lerp(base, delete, progress)
                null -> base
            }

            val align = when (dir) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
                null -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = align
            ) {
                // ✅ Only draw label+icon while swiping (dir != null)
                when (dir) {
                    DismissDirection.StartToEnd -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                modifier = Modifier.graphicsLayer(
                                    scaleX = iconScale,
                                    scaleY = iconScale,
                                    alpha = iconAlpha
                                )
                            )
                            Text(
                                "Complete",
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .graphicsLayer(alpha = iconAlpha)
                            )
                        }
                    }

                    DismissDirection.EndToStart -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Delete",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .graphicsLayer(alpha = iconAlpha)
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.graphicsLayer(
                                    scaleX = iconScale,
                                    scaleY = iconScale,
                                    alpha = iconAlpha
                                )
                            )
                        }
                    }

                    null -> Unit
                }
            }
        },
        dismissContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(translationX = contentShift)
            ) {
                content()
            }
        }
    )
}
