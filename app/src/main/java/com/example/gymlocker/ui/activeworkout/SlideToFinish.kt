package com.example.gymlocker.ui.activeworkout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToFinish(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    text: String = "Slide to finish",
    onFinished: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var trackWidthPx by remember { mutableStateOf(0f) }
    val thumbSize = 44.dp
    val thumbSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbSize.toPx() }
    val horizontalPadding = 6.dp
    val horizontalPaddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { horizontalPadding.toPx() }


    val x = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }

    val progress = remember(trackWidthPx, x.value) {
        val max = (trackWidthPx - 2f * horizontalPaddingPx - thumbSizePx).coerceAtLeast(1f)
        (x.value / max).coerceIn(0f, 1f)
    }

    // Text fades out as you drag
    val textAlpha = if (!enabled) 0.35f else (1f - (progress * 1.2f)).coerceIn(0f, 1f)

    // Color shifts blue -> green (at 100%)
    val startColor = MaterialTheme.colorScheme.primary
    val endColor = Color(0xFF2E7D32)
    val trackColor = lerpColor(startColor, endColor, progress)

    Box(
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor, RoundedCornerShape(999.dp))
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Center text (fade out on drag)
        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(textAlpha)
                .padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .size(thumbSize)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .draggable(
                    enabled = enabled && !completed,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val max = (trackWidthPx - 2f * horizontalPaddingPx - thumbSizePx).coerceAtLeast(1f)
                        val newX = (x.value + delta).coerceIn(0f, max)
                        scope.launch { x.snapTo(newX) }
                    },
                    onDragStopped = {
                        if (completed) return@draggable

                        val max = (trackWidthPx - 2f * horizontalPaddingPx - thumbSizePx).coerceAtLeast(1f)
                        val done = x.value >= max * 0.98f

                        if (done) {
                            completed = true

                            // "Heavy" haptic at end
                            // Compose har ikke "heavyImpact" som i iOS; LongPress føles tungest på Android.
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            scope.launch {
                                // snap to end
                                x.animateTo(max, tween(120))
                                // success + navigate after 1s
                                onFinished()
                            }
                        } else {
                            scope.launch { x.animateTo(0f, tween(200)) }
                        }
                    }
                )
        )
    }
}

// Simple color lerp without depending on additional libs
private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clamped,
        green = a.green + (b.green - a.green) * clamped,
        blue = a.blue + (b.blue - a.blue) * clamped,
        alpha = a.alpha + (b.alpha - a.alpha) * clamped
    )
}
