package com.example.gymlocker.ui.activeworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlocker.ui.theme.GymLockerTheme

/**
 * Represents which type of field is selected
 */
enum class FieldType {
    WEIGHT,
    REPS,
    DONE
}

/**
 * Represents the currently selected cell in the workout grid
 * @param exerciseIndex Index of the exercise in the list (0-based)
 * @param setIndex Index of the set within that exercise (0-based)
 * @param field Which field in the set row is selected
 */
data class CursorPosition(
    val exerciseIndex: Int,
    val setIndex: Int,
    val field: FieldType
)

/**
 * A workout navigation bar with:
 * - Left side: Number pad (1-9, 0, comma, backspace)
 * - Right side: Hide button, Left/Right arrows, Plus/Minus buttons, Next button
 */
@Composable
fun WorkoutNumpadBar(
    isVisible: Boolean,
    onHide: () -> Unit,
    onNavigateLeft: () -> Unit,
    onNavigateRight: () -> Unit,
    onNumberClick: (String) -> Unit = {},
    onBackspace: () -> Unit = {},
    onPlus: () -> Unit = {},
    onMinus: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Use MaterialTheme colors instead of hardcoded colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val buttonBackground = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSurface

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 250)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 200)
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: Number pad (3x4 grid)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: 1 2 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumpadButton("1", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("1") }
                        NumpadButton("2", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("2") }
                        NumpadButton("3", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("3") }
                    }
                    // Row 2: 4 5 6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumpadButton("4", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("4") }
                        NumpadButton("5", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("5") }
                        NumpadButton("6", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("6") }
                    }
                    // Row 3: 7 8 9
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumpadButton("7", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("7") }
                        NumpadButton("8", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("8") }
                        NumpadButton("9", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("9") }
                    }
                    // Row 4: , 0 ⌫
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumpadButton(",", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick(",") }
                        NumpadButton("0", buttonBackground, contentColor, Modifier.weight(1f)) { onNumberClick("0") }
                        IconButton(
                            icon = Icons.AutoMirrored.Filled.Backspace,
                            backgroundColor = buttonBackground,
                            contentColor = contentColor,
                            contentDescription = "Delete",
                            modifier = Modifier.weight(1f),
                            onClick = onBackspace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // RIGHT SIDE: Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hide keyboard button - same width as other buttons
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(buttonBackground)
                            .clickable { onHide() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardHide,
                            contentDescription = "Hide",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Left/Right navigation row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            backgroundColor = buttonBackground,
                            contentColor = contentColor,
                            contentDescription = "Left",
                            onClick = onNavigateLeft
                        )
                        IconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            backgroundColor = buttonBackground,
                            contentColor = contentColor,
                            contentDescription = "Right",
                            onClick = onNavigateRight
                        )
                    }

                    // Plus/Minus row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            icon = Icons.Filled.Remove,
                            backgroundColor = buttonBackground,
                            contentColor = contentColor,
                            contentDescription = "Minus",
                            onClick = onMinus
                        )
                        IconButton(
                            icon = Icons.Filled.Add,
                            backgroundColor = buttonBackground,
                            contentColor = contentColor,
                            contentDescription = "Plus",
                            onClick = onPlus
                        )
                    }

                    // Next button (move to next field) - uses primary color
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable { onNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Next",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * A single numpad button with text
 */
@Composable
private fun NumpadButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 40.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A single icon button
 */
@Composable
private fun IconButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * A small handle/pill that the user can swipe up or tap to reveal the numpad
 */
@Composable
fun NumpadRevealHandle(
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val handleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) {
                        onReveal()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onReveal() }
                )
            }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(handleColor)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E)
@Composable
fun WorkoutNumpadBarPreview() {
    GymLockerTheme {
        WorkoutNumpadBar(
            isVisible = true,
            onHide = {},
            onNavigateLeft = {},
            onNavigateRight = {},
            onNumberClick = {},
            onBackspace = {},
            onPlus = {},
            onMinus = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NumpadRevealHandlePreview() {
    GymLockerTheme {
        NumpadRevealHandle(onReveal = {})
    }
}

