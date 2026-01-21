package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PeriodBarChart(
    values: List<Float>,
    labels: List<String>,
    xCaption: String,
    yTickStep: Float? = null,
    selectedIndex: Int? = null,
    onBarClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        Text(
            text = "No data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Peter Standard color roles:
    // - secondary: highlights / accents (charts, indicators)
    // - outline / outlineVariant: axes + helper text
    val axisColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
    val selectedBarColor: Color = MaterialTheme.colorScheme.primary

    val maxValueRaw = values.maxOrNull() ?: 0f
    val maxValue = kotlin.math.max(1f, maxValueRaw)

    // --- Y-axis tick configuration ---
    val maxTicks = 4

    var step = yTickStep ?: (maxValue / maxTicks).coerceAtLeast(1f)
    val topCeil = kotlin.math.ceil(maxValue / step) * step
    var top = topCeil.coerceAtLeast(step)

    var stepsCount = (top / step).roundToInt().coerceAtLeast(1)

    if (stepsCount > maxTicks) {
        stepsCount = maxTicks
        step = top / stepsCount
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .then(
                    if (onBarClick != null) {
                        Modifier.pointerInput(values.size) {
                            detectTapGestures { offset ->
                                val leftPad = 44.dp.toPx()
                                val chartW = size.width - leftPad
                                val n = values.size
                                val gap = 6.dp.toPx()
                                val totalGap = gap * (n - 1)
                                val barW = ((chartW - totalGap) / n).coerceAtLeast(2f)

                                // Find which bar was clicked
                                val clickX = offset.x
                                if (clickX >= leftPad) {
                                    values.forEachIndexed { idx, _ ->
                                        val barX = leftPad + idx * (barW + gap)
                                        if (clickX >= barX && clickX <= barX + barW) {
                                            onBarClick(idx)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val leftPadDp = 44.dp
            val leftPad = leftPadDp.toPx()
            val bottomPad = 18.dp.toPx()

            val chartW = size.width - leftPad
            val chartH = size.height - bottomPad

            // Axes
            drawLine(axisColor, Offset(leftPad, 0f), Offset(leftPad, chartH), 1.dp.toPx())
            drawLine(axisColor, Offset(leftPad, chartH), Offset(size.width, chartH), 1.dp.toPx())

            // Y ticks + grid + labels
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 11.dp.toPx()
                // Use an on-surface-variant derived color (theme-safe) rather than hardcoded gray
                color = labelColor.copy(alpha = 0.9f).toArgb()
            }

            for (i in 0..stepsCount) {
                val v = i * step
                val ratio = if (top == 0f) 0f else (v / top)
                val y = chartH - ratio * chartH

                // tick
                drawLine(
                    axisColor,
                    Offset(leftPad - 6.dp.toPx(), y),
                    Offset(leftPad, y),
                    1.dp.toPx()
                )
                // grid
                drawLine(
                    axisColor.copy(alpha = 0.25f),
                    Offset(leftPad, y),
                    Offset(size.width, y),
                    1.dp.toPx()
                )

                val label = if (step < 1f) {
                    String.format(Locale.US, "%.1f", v)
                } else {
                    v.toInt().toString()
                }

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    2.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }

            // Bars
            val n = values.size
            val gap = 6.dp.toPx()
            val totalGap = gap * (n - 1)
            val barW = ((chartW - totalGap) / n).coerceAtLeast(2f)

            values.forEachIndexed { idx, v ->
                val x = leftPad + idx * (barW + gap)
                val h = if (top == 0f) 0f else (v / top) * chartH
                val isSelected = selectedIndex == idx
                drawRect(
                    color = if (isSelected) selectedBarColor else barColor,
                    topLeft = Offset(x, chartH - h),
                    size = Size(barW, h)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- X labels ---
        val count = labels.size

        val desiredSlots = if (xCaption == "Month") count else 5.coerceAtMost(count)
        val slots = desiredSlots.coerceAtMost(count)

        val indices = if (slots <= 1) {
            listOf(0)
        } else {
            (0 until slots).map { s ->
                ((s.toFloat() / (slots - 1)) * (count - 1)).roundToInt()
            }.distinct().filter { it in labels.indices }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // align labels with chart area, not full screen
                .padding(start = 44.dp)
        ) {
            indices.forEach { idx ->
                Text(
                    text = labels[idx],
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = xCaption,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
    }
}

// local helper to convert Compose Color -> Android int
private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).roundToInt(),
        (red * 255f).roundToInt(),
        (green * 255f).roundToInt(),
        (blue * 255f).roundToInt()
    )
}
