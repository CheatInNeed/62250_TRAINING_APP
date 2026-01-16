package com.example.gymlocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymlocker.data.dao.MuscleGroupDistributionRow
import kotlin.math.roundToInt

@Composable
fun MuscleGroupDistributionPieChart(
    rows: List<MuscleGroupDistributionRow>,
    modifier: Modifier = Modifier,
    maxSlices: Int = 6,            // antal “rigtige” slices før vi samler resten i "Other"
    showLegend: Boolean = true,
    donut: Boolean = true
) {
    if (rows.isEmpty()) {
        Text(
            "No data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val total = rows.sumOf { it.completedSets }.coerceAtLeast(1)

    // Sortér + tag top N, resten som "Other"
    val sorted = remember(rows) { rows.sortedByDescending { it.completedSets } }

    val slices = remember(sorted, total, maxSlices) {
        val top = sorted.take(maxSlices)
        val rest = sorted.drop(maxSlices)
        val otherSets = rest.sumOf { it.completedSets }

        buildList {
            addAll(top)
            if (otherSets > 0) add(MuscleGroupDistributionRow("Other", otherSets))
        }
    }

    val cs = MaterialTheme.colorScheme

    val colors = listOf(
        cs.primary,                                  // Blå (primær)
        cs.onSurface.copy(alpha = 0.85f),            // Mørk grå
        cs.onSurface.copy(alpha = 0.65f),            // Grå
        cs.onSurface.copy(alpha = 0.45f),            // Lys grå
        cs.onSurfaceVariant.copy(alpha = 0.35f)      // Meget lys grå
    )

    Column(modifier = modifier) {

        // Pie / donut
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f // start øverst

                val stroke = if (donut) Stroke(width = size.minDimension * 0.22f) else Stroke(width = 0f)

                slices.forEachIndexed { i, s ->
                    val sweep = (s.completedSets.toFloat() / total.toFloat()) * 360f
                    val color = colors[i % colors.size]

                    if (donut) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            size = Size(size.width, size.height),
                            style = stroke
                        )
                    } else {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            size = Size(size.width, size.height)
                        )
                    }

                    startAngle += sweep
                }
            }

            // Center label (valgfri)
            if (donut) {
                val pctTop = (slices.first().completedSets * 100f / total).roundToInt()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${total} sets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    /*Text(
                        text = "${slices.first().muscleGroupName} $pctTop%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )*/
                }
            }
        }

        if (!showLegend) return@Column

        Spacer(Modifier.height(8.dp))

        // Legend (navn + sets + %)
        slices.forEachIndexed { i, s ->
            val color = colors[i % colors.size]
            val pct = ((s.completedSets * 100f) / total.toFloat()).roundToInt()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // lille farveprik
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawArc(color = color, startAngle = 0f, sweepAngle = 360f, useCenter = true)
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = s.muscleGroupName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun Color.adjustBrightness(factor: Float): Color {
    return Color(
        red = (red * factor).coerceIn(0f, 1f),
        green = (green * factor).coerceIn(0f, 1f),
        blue = (blue * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}
