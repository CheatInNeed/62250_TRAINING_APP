package com.example.gymlocker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimerBottomSheet(
    visible: Boolean,
    initialSeconds: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onClear: () -> Unit,
    stepSeconds: Int = 15,
    quickSelectSeconds: List<Int> = listOf(60, 90, 120, 180),
    maxSeconds: Int = 60 * 30,
    minSeconds: Int = 15
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Working value (det brugeren redigerer lige nu)
    var seconds by remember(visible) { mutableIntStateOf(initialSeconds ?: quickSelectSeconds.first()) }

    // clamp helper
    fun clamp(v: Int): Int = v.coerceIn(0, maxSeconds)

    // ensure valid when opened
    LaunchedEffect(visible) {
        seconds = clamp(seconds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // 40% af skærmen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.40f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ---------- TOP ----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rest timer",
                    style = MaterialTheme.typography.headlineSmall
                )
                // Spacer(Modifier.height(1.dp))
                Text(
                    text = "+/- justerer med ${stepSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---------- MIDDLE:  -  TIMER  + ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                    //.padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canMinus = seconds > 0 && seconds - stepSeconds >= 0
                FilledIconButton(
                    onClick = {
                        val next = (seconds - stepSeconds).coerceAtLeast(0)
                        seconds = if (next in 1 until minSeconds) minSeconds else next
                    },
                    enabled = canMinus
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Minus")
                }

                Text(
                    text = formatMmSs(seconds.coerceAtLeast(0)),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )

                val canPlus = seconds + stepSeconds <= maxSeconds
                FilledIconButton(
                    onClick = { seconds = clamp(seconds + stepSeconds) },
                    enabled = canPlus
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Plus")
                }
            }

            // ---------- QUICK SELECT ----------
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickSelectSeconds.forEach { s ->
                        val selected = seconds == s
                        val chipColors = FilterChipDefaults.filterChipColors(
                            // Unselected
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,

                            // Selected (matcher app theme)
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        FilterChip(
                            selected = selected,
                            onClick = { seconds = s },
                            label = { Text(formatMmSs(s)) },
                            shape = RoundedCornerShape(14.dp),
                            colors = chipColors,
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---------- ACTIONS ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        }
                    ) { Text("Off") }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Button(
                            onClick = {
                                // hvis du vil sikre minimum (fx 0 => Off håndteres separat)
                                val finalValue =
                                    if (seconds in 1 until minSeconds) minSeconds else seconds
                                onSave(finalValue)
                                onDismiss()
                            }
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = (totalSeconds / 60).coerceAtLeast(0)
    val s = (totalSeconds % 60).coerceAtLeast(0)
    return "%02d:%02d".format(m, s)
}
