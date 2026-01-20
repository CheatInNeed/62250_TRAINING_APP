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
val BotBarShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val TopBarShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)