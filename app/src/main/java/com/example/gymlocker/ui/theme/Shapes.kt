package com.example.gymlocker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Peter Standard — App chrome shape
 *
 * - Rounded TOP corners
 * - Square bottom corners (flush with screen edge)
 * Used for TopAppBar / BottomBar gloss overlays.
 */
val BarShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
