package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MuscleGroupDistributionChart(
    rows: List<com.example.gymlocker.data.dao.MuscleGroupDistributionRow>,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        Text(
            "No data in range",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val shown = rows.take(8)
    val totalSets = shown.sumOf { it.completedSets }.coerceAtLeast(1)
    val maxValue = shown.maxOf { it.completedSets }.coerceAtLeast(1)

    // Theme-safe roles
    val barColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val labelColor: Color = MaterialTheme.colorScheme.onSurface
    val subColor: Color = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        shown.forEach { r ->
            val pct = ((r.completedSets * 100f) / totalSets)
            val pctText = "${pct.toInt()}%" // integer percent

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left labels
                Column(modifier = Modifier.width(110.dp)) {
                    Text(
                        text = r.muscleGroupName,
                        style = MaterialTheme.typography.labelMedium,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${r.completedSets} sets",
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor
                    )
                }

                // Bar
                Canvas(
                    modifier = Modifier
                        .height(18.dp)
                        .weight(1f)
                ) {
                    val w = (r.completedSets.toFloat() / maxValue) * size.width
                    drawRect(
                        color = barColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, size.height)
                    )
                }

                // Percentage label on the right
                Text(
                    text = pctText,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor
                )
            }
        }
    }
}
