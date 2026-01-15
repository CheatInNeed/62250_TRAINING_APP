package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

private fun weekNumber(date: LocalDate): Int {
    return date.get(WeekFields.ISO.weekOfWeekBasedYear())
}

@Composable
fun <T> WeeklyBarChart(
    data: List<T>,
    weekStartOf: (T) -> LocalDate,
    valueOf: (T) -> Float,
    modifier: Modifier = Modifier,
    legendPrefix: String = "Week:"
) {
    if (data.isEmpty()) {
        Text(
            text = "No data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxValue = kotlin.math.max(1f, data.maxOf { valueOf(it) })

    val barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val axisColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val labelColor: Color = MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val barCount = data.size
            if (barCount == 0) return@Canvas

            val gap = 6.dp.toPx()
            val totalGap = gap * (barCount - 1)
            val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(2f)

            data.forEachIndexed { i, item ->
                val x = i * (barWidth + gap)
                val v = valueOf(item)
                val barHeight = (v / maxValue) * size.height

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }

            drawLine(
                color = axisColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        Spacer(Modifier.height(8.dp))

        val count = data.size
        val slots = 5
        val indices = (0 until slots).map { s ->
            if (slots == 1) 0
            else ((s.toFloat() / (slots - 1)) * (count - 1)).roundToInt()
        }.distinct().filter { it in data.indices }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = legendPrefix,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(end = 8.dp)
            )

            indices.forEach { idx ->
                val wk = weekNumber(weekStartOf(data[idx]))
                Text(
                    text = wk.toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}
