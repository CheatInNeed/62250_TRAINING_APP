package com.example.gymlocker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.gymlocker.ui.settings.LocalUserSettings

@Composable
fun Modifier.metalGloss(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    val settings = LocalUserSettings.current
    val isDark = isSystemInDarkTheme() || settings.forceDarkMode

    val lightGlossStrength = 0.5f
    val darkGlossStrength  = 0.5f

    val lightGlossMax = 1.5f
    val darlGlossMax  = 1.5f

    val tint = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    // ✅ split knobs for light/dark
    val strength = if (isDark) darkGlossStrength else lightGlossStrength
    val max = if (isDark) darlGlossMax else lightGlossMax
    val s = strength.coerceIn(0f, max)

    val diagonalHighlightAlpha = (0.22f * s).coerceIn(0f, 1f)
    val diagonalShadowAlpha = (0.16f * s).coerceIn(0f, 1f)
    val sheenCenterAlpha = (0.30f * s).coerceIn(0f, 1f)

    return this
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height

            val diagonal = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to tint.copy(alpha = (diagonalHighlightAlpha * 0.90f).coerceIn(0f, 1f)),
                    0.18f to tint.copy(alpha = (diagonalHighlightAlpha * 0.55f).coerceIn(0f, 1f)),
                    0.34f to tint.copy(alpha = (diagonalHighlightAlpha * 0.22f).coerceIn(0f, 1f)),
                    0.50f to Color.Transparent,
                    0.70f to Color.Black.copy(alpha = (diagonalShadowAlpha * 0.35f).coerceIn(0f, 1f)),
                    1.00f to Color.Black.copy(alpha = (diagonalShadowAlpha * 0.75f).coerceIn(0f, 1f))
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 1.8f)
            )

            val sheen = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.36f to tint.copy(alpha = (0.10f * s).coerceIn(0f, 1f)),
                    0.48f to tint.copy(alpha = sheenCenterAlpha),
                    0.60f to tint.copy(alpha = (0.10f * s).coerceIn(0f, 1f)),
                    1.00f to Color.Transparent
                ),
                start = Offset(-0.18f * w, 0.05f * h),
                end = Offset(1.12f * w, 1.35f * h)
            )

            onDrawWithContent {
                drawContent()
                drawRect(brush = diagonal, blendMode = BlendMode.Hardlight)
                drawRect(brush = sheen, blendMode = BlendMode.Overlay)
            }
        }
}
