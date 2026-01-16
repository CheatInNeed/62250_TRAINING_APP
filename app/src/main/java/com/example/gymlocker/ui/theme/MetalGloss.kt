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

/**
 * Global strength knob for the metal gloss effect.
 * Tweak this value only (no settings/db/per-component overrides).
 *
 * Safe range is clamped internally to [0f, 1.5f].
 */
private const val METAL_GLOSS_STRENGTH = 0.5f // tweak this

@Composable
fun Modifier.metalGloss(
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier {
    // Required dark-mode detection logic (settings + system)
    val settings = LocalUserSettings.current
    val isDark = isSystemInDarkTheme() || settings.forceDarkMode

    // Tint logic (only tint changes by mode; math/algorithm stays identical)
    val tint = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    // Strength: single knob, clamped
    val s = METAL_GLOSS_STRENGTH.coerceIn(0f, 1.5f)

    // Map strength into pass alphas (kept stable across light/dark)
    // Clamp to [0f, 1f] for alpha safety
    val diagonalHighlightAlpha = (0.22f * s).coerceIn(0f, 1f)
    val diagonalShadowAlpha = (0.16f * s).coerceIn(0f, 1f)
    val sheenCenterAlpha = (0.30f * s).coerceIn(0f, 1f)

    // Avoid composable reads inside drawWithCache by capturing computed values above.
    return this
        .clip(shape)
        .drawWithCache {
            val w = size.width
            val h = size.height

            // PASS 1: Full diagonal “metal lighting” gradient (highlight -> transparent -> shadow tail)
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
                end = Offset(w, h)
            )


            // PASS 2: Specular sheen stripe (reflective band)
            val sheen = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.36f to tint.copy(alpha = (0.10f * s).coerceIn(0f, 1f)),
                    0.48f to tint.copy(alpha = sheenCenterAlpha),
                    0.60f to tint.copy(alpha = (0.10f * s).coerceIn(0f, 1f)),
                    1.00f to Color.Transparent
                ),
                start = Offset(-0.15f * w, 0.10f * h),
                end = Offset(1.10f * w, 0.90f * h)
            )

            onDrawWithContent {
                drawContent()

                // Metallic base
                drawRect(
                    brush = diagonal,
                    blendMode = BlendMode.Hardlight
                )

                // IMPORTANT: Overlay shows on light AND dark surfaces.
                // Screen often disappears on near-white surfaces.
                drawRect(
                    brush = sheen,
                    blendMode = BlendMode.Overlay
                )
            }
        }
}
