package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PeriodBarChart(
    values: List<Float>,
    labels: List<String>,
    xCaption: String,
    yTickStep: Float? = null,  // if null => auto ticks
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

    val axisColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val labelColor: Color = MaterialTheme.colorScheme.outline
    val barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

    val maxValueRaw = values.maxOrNull() ?: 0f
    val maxValue = kotlin.math.max(1f, maxValueRaw)

    // --- Y-axis tick configuration ---
    val maxTicks = 4 // "up to" this many steps above zero
    val step = yTickStep ?: (maxValue / maxTicks).coerceAtLeast(1f)
    val top = (kotlin.math.ceil(maxValue / step) * step).coerceAtLeast(step)
    val stepsCount = (top / step).roundToInt().coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val leftPad = 44.dp.toPx()
            val bottomPad = 18.dp.toPx()

            val chartW = size.width - leftPad
            val chartH = size.height - bottomPad

            // Y axis + X axis
            drawLine(axisColor, Offset(leftPad, 0f), Offset(leftPad, chartH), 1.dp.toPx())
            drawLine(axisColor, Offset(leftPad, chartH), Offset(size.width, chartH), 1.dp.toPx())

            // Y ticks + grid + labels
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 11.dp.toPx()
                color = android.graphics.Color.GRAY
            }

            val actualSteps = stepsCount.coerceAtMost(maxTicks)

            for (i in 0..actualSteps) {
                val v = i * step
                val ratio = if (top == 0f) 0f else (v / top)
                val y = chartH - ratio * chartH

                // tick line
                drawLine(
                    axisColor,
                    Offset(leftPad - 6.dp.toPx(), y),
                    Offset(leftPad, y),
                    1.dp.toPx()
                )
                // grid line
                drawLine(
                    axisColor.copy(alpha = 0.25f),
                    Offset(leftPad, y),
                    Offset(size.width, y),
                    1.dp.toPx()
                )

                val label = if (step < 1f) {
                    // show decimals for small ranges like hours
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
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, chartH - h),
                    size = Size(barW, h)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- X labels ---
        val count = labels.size

        // For Month view we show all labels (12 months).
        // For others (like Week) keep the old behaviour: ~5 samples.
        val desiredSlots = if (xCaption == "Month") {
            count
        } else {
            5.coerceAtMost(count)
        }

        val slots = desiredSlots.coerceAtMost(count)

        val indices = if (slots <= 1) {
            listOf(0)
        } else {
            (0 until slots).map { s ->
                ((s.toFloat() / (slots - 1)) * (count - 1)).roundToInt()
            }.distinct().filter { it in labels.indices }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
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

        // Caption BELOW axis/labels (no wasted chart space)
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
