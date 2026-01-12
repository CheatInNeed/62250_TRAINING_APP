package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import com.example.gymlocker.viewmodel.WeekHoursUi
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.math.max
import kotlin.math.roundToInt

private fun weekNumber(date: LocalDate): Int {
    return date.get(WeekFields.ISO.weekOfWeekBasedYear())
}

@Composable
fun WeeklyHoursChart(
    data: List<WeekHoursUi>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val maxHours = max(1f, data.maxOf { it.hours })

    // ✅ Read theme values OUTSIDE Canvas
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
                val barHeight = (item.hours / maxHours) * size.height

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }

            // baseline
            drawLine(
                color = axisColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        Spacer(Modifier.height(8.dp))

        // ✅ Legend: "Week:" + 5 week numbers
        // Pick 5 indices evenly spread from 0..last
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
                text = "Week:",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Render 5 week numbers (numbers only)
            indices.forEach { idx ->
                val wk = weekNumber(data[idx].weekStart)
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
